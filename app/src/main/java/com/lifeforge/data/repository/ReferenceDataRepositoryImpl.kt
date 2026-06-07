package com.lifeforge.data.repository

import com.lifeforge.data.api.ReferenceApi
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.ReferenceData
import com.lifeforge.domain.model.mapCatching
import com.lifeforge.domain.repository.ReferenceDataRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Premissas de referencia: pass-through para a API com cache em memoria. Como
 * sao premissas de longo prazo (mudam raramente), a primeira chamada bem
 * sucedida e reaproveitada pelo resto da sessao.
 */
@Singleton
class ReferenceDataRepositoryImpl @Inject constructor(
    private val api: ReferenceApi,
    private val json: Json,
) : ReferenceDataRepository {

    @Volatile
    private var cached: ReferenceData? = null

    override suspend fun getReferenceData(): DataResult<ReferenceData> {
        cached?.let { return DataResult.Success(it) }
        val result = safeApiCall(json) { api.getReferenceData() }.mapCatching { it.toDomain() }
        if (result is DataResult.Success) cached = result.data
        return result
    }
}
