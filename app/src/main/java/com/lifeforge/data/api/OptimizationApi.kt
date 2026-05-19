package com.lifeforge.data.api

import com.lifeforge.data.model.dto.OptimizationResponseDto
import com.lifeforge.data.model.dto.OptimizeContributionRequestDto
import com.lifeforge.data.model.dto.OptimizeHorizonRequestDto
import com.lifeforge.data.model.dto.RebalanceRequestDto
import com.lifeforge.data.model.dto.RebalanceResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Endpoints de otimização (Sprint 3 backend).
 *
 * Os três endpoints são pesados (cada um faz busca binária + verificação
 * Monte Carlo de 10k iterações). ViewModels devem rodar em IO dispatcher
 * e exibir loading durante 2-5s típicos.
 *
 * Resultados não são persistidos no servidor — cliente decide se quer
 * cachear. Por padrão a Fase 4.1 NÃO faz cache de otimização.
 */
interface OptimizationApi {

    @POST("optimize/contribution")
    suspend fun optimizeContribution(
        @Body body: OptimizeContributionRequestDto,
    ): Response<OptimizationResponseDto>

    @POST("optimize/horizon")
    suspend fun optimizeHorizon(
        @Body body: OptimizeHorizonRequestDto,
    ): Response<OptimizationResponseDto>

    @POST("optimize/rebalance")
    suspend fun rebalance(
        @Body body: RebalanceRequestDto,
    ): Response<RebalanceResponseDto>
}
