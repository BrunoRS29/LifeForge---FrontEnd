package com.lifeforge.domain.usecase

import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.ReferenceData
import com.lifeforge.domain.repository.ReferenceDataRepository
import javax.inject.Inject

/**
 * Busca as premissas de referencia (calibracao). Camada fina sobre o
 * repositorio, mantendo a presentation desacoplada.
 */
class GetReferenceDataUseCase @Inject constructor(
    private val repository: ReferenceDataRepository,
) {
    suspend operator fun invoke(): DataResult<ReferenceData> = repository.getReferenceData()
}
