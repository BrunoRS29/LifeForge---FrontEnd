package com.lifeforge.domain.usecase

import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.model.User
import com.lifeforge.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCases do usuário corrente. Pass-through puro — sem lógica de
 * domínio adicional. Mantidos para que ViewModels nunca dependam
 * de Repository diretamente.
 */

class ObserveCurrentUserUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    operator fun invoke(): Flow<User?> = repository.observeCurrentUser()
}

class RefreshCurrentUserUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(): DataResult<User> = repository.refreshCurrentUser()
}

class UpdateRiskProfileUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(profile: RiskProfile): DataResult<User> =
        repository.updateRiskProfile(profile)
}
