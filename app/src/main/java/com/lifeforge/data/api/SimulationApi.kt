package com.lifeforge.data.api

import com.lifeforge.data.model.dto.RunSimulationRequestDto
import com.lifeforge.data.model.dto.SimulationResultResponseDto
import com.lifeforge.data.model.dto.SimulationSummaryResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Endpoints de simulação Monte Carlo.
 *
 * - `run` é a chamada pesada (pode levar 1-3s no backend para 10k iterações).
 *   ViewModels devem mostrar loading state apropriado.
 * - `getById` retorna o histograma completo; `listByGoal` retorna resumos.
 */
interface SimulationApi {

    @POST("simulation/run")
    suspend fun run(@Body body: RunSimulationRequestDto): Response<SimulationResultResponseDto>

    @GET("simulation/{id}")
    suspend fun getById(@Path("id") id: Long): Response<SimulationResultResponseDto>

    @GET("simulation/by-goal/{goalId}")
    suspend fun listByGoal(@Path("goalId") goalId: Long): Response<List<SimulationSummaryResponseDto>>

    @DELETE("simulation/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>
}
