package com.lifeforge.data.repository

import com.lifeforge.data.api.UserApi
import com.lifeforge.data.db.dao.UserDao
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.mapper.toEntity
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.User
import com.lifeforge.domain.model.mapCatching
import com.lifeforge.data.model.dto.UpdateNameRequestDto
import com.lifeforge.data.model.dto.UpdateRiskProfileRequestDto
import com.lifeforge.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserRepository — observa o user em cache e permite refresh sob demanda.
 *
 * Diferente dos outros repositórios CRUD, não há `create/update/delete`
 * — alterações no perfil acontecem via auth (register) ou via fluxos
 * específicos (mudar perfil de risco — não implementado nesta sprint).
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val json: Json,
) : UserRepository {

    override fun observeCurrentUser(): Flow<User?> =
        userDao.observeCurrent().map { it?.toDomain() }

    override suspend fun refreshCurrentUser(): DataResult<User> =
        safeApiCall(json) { userApi.getCurrentUser() }
            .mapCatching { dto ->
                userDao.upsert(dto.toEntity())
                dto.toDomain()
            }

    // ADD THIS METHOD:
    override suspend fun updateRiskProfile(profile: com.lifeforge.domain.model.RiskProfile): DataResult<User> {
        // We map the Domain 'RiskProfile' to the DTO expected by AuthApi.kt
        val requestBody = UpdateRiskProfileRequestDto(
            riskProfile = profile.name // or however your DTO/Domain maps
        )

        return safeApiCall(json) { userApi.updateRiskProfile(requestBody) }
            .mapCatching { dto ->
                // Save the updated user to Room so the UI updates automatically
                userDao.upsert(dto.toEntity())
                dto.toDomain()
            }
    }

    override suspend fun updateName(name: String): DataResult<User> =
        safeApiCall(json) { userApi.updateName(UpdateNameRequestDto(name)) }
            .mapCatching { dto ->
                // Atualiza o Room — a UI (header do perfil) reage sozinha.
                userDao.upsert(dto.toEntity())
                dto.toDomain()
            }
}
