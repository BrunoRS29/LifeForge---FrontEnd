package com.lifeforge.data.api

import com.lifeforge.data.model.dto.ReferenceDataResponseDto
import retrofit2.Response
import retrofit2.http.GET

/**
 * Base de estatisticas de referencia (publico). Nao exige auth, mas o
 * AuthInterceptor injetar o Bearer e inofensivo.
 *  GET /reference-data -> premissas de longo prazo (calibracao)
 */
interface ReferenceApi {

    @GET("reference-data")
    suspend fun getReferenceData(): Response<ReferenceDataResponseDto>
}
