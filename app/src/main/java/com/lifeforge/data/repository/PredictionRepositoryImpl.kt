package com.lifeforge.data.repository

import com.lifeforge.data.api.PredictionApi
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.mapper.toRequestDto
import com.lifeforge.data.model.dto.PredictExpensesRequestDto
import com.lifeforge.data.model.dto.PredictIncomeRequestDto
import com.lifeforge.data.model.dto.PredictWealthRequestDto
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.model.CalibratedSimulation
import com.lifeforge.domain.model.CalibratedSimulationParameters
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.ExpensePrediction
import com.lifeforge.domain.model.IncomePrediction
import com.lifeforge.domain.model.PredictionSummary
import com.lifeforge.domain.model.WealthPrediction
import com.lifeforge.domain.model.mapCatching
import com.lifeforge.domain.repository.PredictionRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacao do [PredictionRepository] - somente network, sem cache local.
 *
 * Segue o padrao das outras impls da Sprint 1-3:
 *  - `safeApiCall(json) { api.x(...) }` para mapear excecoes -> DataResult
 *  - `.mapCatching { it.toDomain() }` para a conversao DTO -> Domain
 *  - `@Singleton` porque a unica dependencia (PredictionApi) eh tambem singleton
 */
@Singleton
class PredictionRepositoryImpl @Inject constructor(
    private val api: PredictionApi,
    private val json: Json,
) : PredictionRepository {

    override suspend fun predictIncome(
        horizonMonths: Int,
    ): DataResult<IncomePrediction> = safeApiCall(json) {
        api.predictIncome(PredictIncomeRequestDto(horizonMonths = horizonMonths))
    }.mapCatching { it.toDomain() }

    override suspend fun predictExpenses(
        horizonMonths: Int,
    ): DataResult<ExpensePrediction> = safeApiCall(json) {
        api.predictExpenses(PredictExpensesRequestDto(horizonMonths = horizonMonths))
    }.mapCatching { it.toDomain() }

    override suspend fun predictWealth(
        horizonMonths: Int,
    ): DataResult<WealthPrediction> = safeApiCall(json) {
        api.predictWealth(PredictWealthRequestDto(horizonMonths = horizonMonths))
    }.mapCatching { it.toDomain() }

    override suspend fun listRecent(limit: Int): DataResult<List<PredictionSummary>> =
        safeApiCall(json) { api.listPredictions(limit = limit) }
            .mapCatching { list -> list.map { it.toDomain() } }

    override suspend fun runCalibrated(
        parameters: CalibratedSimulationParameters,
    ): DataResult<CalibratedSimulation> = safeApiCall(json) {
        api.runCalibratedSimulation(parameters.toRequestDto())
    }.mapCatching { it.toDomain() }
}
