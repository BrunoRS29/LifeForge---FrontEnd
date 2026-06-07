package com.lifeforge.domain.usecase

import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.UserProfile
import com.lifeforge.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * UseCases do perfil estendido (parâmetros para projeções). Camada fina sobre
 * o [UserProfileRepository] — mantém a presentation desacoplada do repositório.
 */
class GetUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke(): DataResult<UserProfile> = repository.getProfile()
}

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke(profile: UserProfile): DataResult<UserProfile> =
        repository.updateProfile(profile)
}
