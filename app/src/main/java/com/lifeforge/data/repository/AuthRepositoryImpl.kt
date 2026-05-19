package com.lifeforge.data.repository

import com.lifeforge.data.api.AuthApi
import com.lifeforge.data.auth.TokenStore
import com.lifeforge.data.db.LifeForgeDatabase
import com.lifeforge.data.db.dao.UserDao
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.mapper.toEntity
import com.lifeforge.data.model.dto.AuthResponseDto
import com.lifeforge.data.model.dto.LoginRequestDto
import com.lifeforge.data.model.dto.RegisterRequestDto
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.model.AuthSession
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.model.mapCatching
import com.lifeforge.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação de [AuthRepository].
 *
 * Política de isolamento entre sessões: ao fazer register/login, chamamos
 * `database.clearAllTables()` ANTES de gravar o novo token e usuário.
 * Isso garante que dados em cache de uma sessão anterior (do mesmo
 * dispositivo, com outro usuário) não vazem para a nova sessão.
 *
 * No logout fazemos o mesmo: limpa o token e o banco. O Flow de
 * [observeSession] auto-emite null e a UI navega para o login.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userDao: UserDao,
    private val tokenStore: TokenStore,
    private val database: LifeForgeDatabase,
    private val json: Json,
) : AuthRepository {

    override suspend fun register(
        email: String,
        name: String,
        password: String,
        riskProfile: RiskProfile?,
    ): DataResult<AuthSession> = safeApiCall(json) {
        authApi.register(
            RegisterRequestDto(
                email = email,
                name = name,
                password = password,
                riskProfile = riskProfile?.name,
            )
        )
    }.mapCatching { response -> persistSession(response) }

    override suspend fun login(
        email: String,
        password: String,
    ): DataResult<AuthSession> = safeApiCall(json) {
        authApi.login(LoginRequestDto(email = email, password = password))
    }.mapCatching { response -> persistSession(response) }

    override suspend fun logout() {
        tokenStore.clear()
        // clearAllTables é uma operação bloqueante de I/O — Room lança
        // IllegalStateException se chamada na Main. Como o ViewModel
        // chama esta função pelo `viewModelScope` (cujo dispatcher
        // default é Main), forçamos a troca para IO aqui.
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
    }

    override fun observeSession(): Flow<AuthSession?> =
        // Combine emite uma sessão completa apenas quando token E user
        // estão presentes simultaneamente. Isso evita uma janela onde
        // a UI veria "logado mas sem dados de usuário" durante a transição.
        tokenStore.tokenFlow.combine(userDao.observeCurrent()) { token, userEntity ->
            if (!token.isNullOrBlank() && userEntity != null) {
                AuthSession(token = token, user = userEntity.toDomain())
            } else {
                null
            }
        }

    /**
     * Persiste o resultado de um auth bem-sucedido:
     *  1. Limpa qualquer dado da sessão anterior
     *  2. Salva o token no DataStore
     *  3. Salva o usuário no Room
     *
     * Ordem importa — `clearAllTables` antes do `userDao.upsert` para
     * não apagar o registro recém-inserido.
     */
    private suspend fun persistSession(response: AuthResponseDto): AuthSession {
        // Toda a sequência de I/O (limpar banco → gravar token → upsert)
        // precisa rodar em IO. Sem isto, clearAllTables lança
        // IllegalStateException ("Cannot access database on the main thread")
        // que é capturada pelo mapCatching e devolvida como AppError.Unknown
        // — explicando a mensagem "Algo deu errado" mesmo com HTTP 201/200.
        return withContext(Dispatchers.IO) {
            database.clearAllTables()
            tokenStore.setToken(response.token)
            val userEntity = response.user.toEntity()
            userDao.upsert(userEntity)
            AuthSession(
                token = response.token,
                user = response.user.toDomain(),
            )
        }
    }
}
