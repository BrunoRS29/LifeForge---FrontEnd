package com.lifeforge.domain.usecase

import com.lifeforge.domain.model.UsabilitySession
import com.lifeforge.domain.repository.UsabilityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** UseCases do registro das sessões de avaliação de usabilidade (pass-through). */

class ObserveUsabilitySessionsUseCase @Inject constructor(
    private val repository: UsabilityRepository,
) {
    operator fun invoke(): Flow<List<UsabilitySession>> = repository.observeSessions()
}

class SaveUsabilitySessionUseCase @Inject constructor(
    private val repository: UsabilityRepository,
) {
    suspend operator fun invoke(session: UsabilitySession): UsabilitySession = repository.save(session)
}

class DeleteUsabilitySessionUseCase @Inject constructor(
    private val repository: UsabilityRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
