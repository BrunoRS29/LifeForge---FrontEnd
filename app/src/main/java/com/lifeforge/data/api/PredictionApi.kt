package com.lifeforge.data.api

import com.lifeforge.data.model.dto.PredictExpensesRequestDto
import com.lifeforge.data.model.dto.PredictExpensesResponseDto
import com.lifeforge.data.model.dto.PredictIncomeRequestDto
import com.lifeforge.data.model.dto.PredictIncomeResponseDto
import com.lifeforge.data.model.dto.PredictWealthRequestDto
import com.lifeforge.data.model.dto.PredictWealthResponseDto
import com.lifeforge.data.model.dto.PredictionSummaryResponseDto
import com.lifeforge.data.model.dto.RunCalibratedSimulationRequestDto
import com.lifeforge.data.model.dto.RunCalibratedSimulationResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Endpoints do subsistema de IA preditiva (Sprint 5).
 *
 * Caracteristicas operacionais:
 *  - `predictIncome` / `predictExpenses` sao caros: o backend dispara o
 *    microsservico Python que treina o modelo on-the-fly. Pode levar
 *    1-3s. ViewModels devem mostrar loading state.
 *  - `runCalibratedSimulation` eh a chamada mais cara do app: o
 *    backend chama o Python DUAS vezes (renda + despesa), calibra os
 *    parametros e roda 10k iteracoes de Monte Carlo. 3-6s. Indispensavel
 *    timeout generoso no OkHttp (60s ja configurado no NetworkModule).
 *  - `listPredictions` eh leve - so metadata, sem o JSON output completo.
 */
interface PredictionApi {

    /**
     * Roda regressao linear sobre o historico de renda do usuario.
     * O backend le o historico do PostgreSQL - nao precisamos enviar.
     */
    @POST("predictions/income")
    suspend fun predictIncome(
        @Body body: PredictIncomeRequestDto,
    ): Response<PredictIncomeResponseDto>

    /**
     * Random Forest sobre historico de despesas categorizadas.
     */
    @POST("predictions/expenses")
    suspend fun predictExpenses(
        @Body body: PredictExpensesRequestDto,
    ): Response<PredictExpensesResponseDto>

    /**
     * Serie temporal de patrimonio (ARIMA). O backend reconstroi a serie
     * mensal a partir do fluxo de caixa e devolve historico + projecao.
     */
    @POST("predictions/wealth")
    suspend fun predictWealth(
        @Body body: PredictWealthRequestDto,
    ): Response<PredictWealthResponseDto>

    /**
     * Lista predicoes recentes do usuario (auditoria).
     * Limit default 50 no backend, max 200.
     */
    @GET("predictions")
    suspend fun listPredictions(
        @Query("limit") limit: Int? = null,
    ): Response<List<PredictionSummaryResponseDto>>

    /**
     * Monte Carlo CALIBRADO por IA.
     *
     * Substitui `simulation/run` (Sprint 2): em vez do usuario digitar
     * `monthlyContribution`, o backend deriva esse valor de
     * `predicted_income - predicted_expenses`.
     */
    @POST("simulation/run-calibrated")
    suspend fun runCalibratedSimulation(
        @Body body: RunCalibratedSimulationRequestDto,
    ): Response<RunCalibratedSimulationResponseDto>
}
