package com.lifeforge.domain.usecase

import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.AuthSession
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCases do domínio de autenticação.
 *
 * Cada UseCase encapsula uma operação atômica com pré-validação que
 * faz sentido na camada de domínio (campos vazios, formato de email,
 * tamanho mínimo de senha). Validações de negócio do servidor (email
 * duplicado, etc.) continuam vindo como `AppError.Validation` da rede.
 */

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        name: String,
        password: String,
        riskProfile: RiskProfile? = null,
    ): DataResult<AuthSession> {
        validateEmail(email)?.let { return it }
        if (name.isBlank()) {
            return DataResult.Failure(AppError.Validation("name", "nome é obrigatório"))
        }
        if (password.length < 8) {
            return DataResult.Failure(
                AppError.Validation("password", "senha deve ter pelo menos 8 caracteres")
            )
        }
        return repository.register(email.trim(), name.trim(), password, riskProfile)
    }
}

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
    ): DataResult<AuthSession> {
        validateEmail(email)?.let { return it }
        if (password.isBlank()) {
            return DataResult.Failure(AppError.Validation("password", "senha é obrigatória"))
        }
        return repository.login(email.trim(), password)
    }
}

class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() = repository.logout()
}

class ObserveSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthSession?> = repository.observeSession()
}

/**
 * Validação de email reutilizada por register e login. Retorna
 * `Failure` se inválido ou `null` se ok — padrão "early return" deixa
 * os UseCases legíveis.
 *
 * Regex deliberadamente simples — formato local-part@domain.tld.
 * Validação mais rigorosa fica para o servidor.
 */
private fun validateEmail(email: String): DataResult.Failure? {
    val trimmed = email.trim()
    if (trimmed.isBlank()) {
        return DataResult.Failure(AppError.Validation("email", "email é obrigatório"))
    }
    val regex = Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")
    if (!regex.matches(trimmed)) {
        return DataResult.Failure(AppError.Validation("email", "formato de email inválido"))
    }
    return null
}
