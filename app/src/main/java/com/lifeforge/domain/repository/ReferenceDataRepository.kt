package com.lifeforge.domain.repository

import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.ReferenceData

/**
 * Premissas de referencia (calibracao). Network-only com cache em memoria: os
 * valores raramente mudam, entao basta buscar uma vez por sessao.
 */
interface ReferenceDataRepository {
    suspend fun getReferenceData(): DataResult<ReferenceData>
}
