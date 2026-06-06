package com.lifeforge.data.mapper

import com.lifeforge.data.model.dto.CalibrationSummaryResponseDto
import com.lifeforge.data.model.dto.PredictExpensesCategoryDto
import com.lifeforge.data.model.dto.PredictExpensesResponseDto
import com.lifeforge.data.model.dto.PredictIncomePointDto
import com.lifeforge.data.model.dto.PredictIncomeResponseDto
import com.lifeforge.data.model.dto.PredictWealthResponseDto
import com.lifeforge.data.model.dto.PredictionSummaryResponseDto
import com.lifeforge.data.model.dto.RunCalibratedSimulationRequestDto
import com.lifeforge.data.model.dto.RunCalibratedSimulationResponseDto
import com.lifeforge.domain.model.CalibratedSimulation
import com.lifeforge.domain.model.CalibratedSimulationParameters
import com.lifeforge.domain.model.CalibrationSummary
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.ExpenseCategoryPrediction
import com.lifeforge.domain.model.ExpensePrediction
import com.lifeforge.domain.model.IncomePrediction
import com.lifeforge.domain.model.IncomePredictionPoint
import com.lifeforge.domain.model.PredictionMetrics
import com.lifeforge.domain.model.PredictionSummary
import com.lifeforge.domain.model.WealthHistoryPoint
import com.lifeforge.domain.model.WealthPrediction
import com.lifeforge.domain.model.WealthPredictionPoint
import java.time.Instant

/**
 * Mappers entre DTOs Retrofit e modelos de dominio do subsistema de IA.
 *
 * Padroes seguidos das sprints anteriores:
 *  - Extension functions em vez de funcoes top-level com nome longo
 *  - `parseInstantOrNow` resiste a timestamps malformados sem crashar a UI
 *  - Enums sao mapeados defensivamente (raw String preservada quando
 *    nao reconhecida) para nao quebrar caso o backend evolua
 */

// ============================================================================
// DTO -> Domain (Income)
// ============================================================================

fun PredictIncomeResponseDto.toDomain(): IncomePrediction = IncomePrediction(
    predictionId = predictionId,
    modelName = modelName,
    horizonMonths = horizonMonths,
    projection = projection.map { it.toDomain() },
    expectedMonthlyIncome = expectedMonthlyIncome,
    annualGrowthRate = annualGrowthRate,
    residualVolatilityMonthly = residualVolatilityMonthly,
    metrics = PredictionMetrics(mae = mae, rmse = rmse, r2 = r2),
    createdAt = parseInstantOrNow(createdAt),
)

private fun PredictIncomePointDto.toDomain() = IncomePredictionPoint(
    monthIndex = monthIndex,
    predictedAmount = predictedAmount,
)

// ============================================================================
// DTO -> Domain (Expense)
// ============================================================================

fun PredictExpensesResponseDto.toDomain(): ExpensePrediction = ExpensePrediction(
    predictionId = predictionId,
    modelName = modelName,
    horizonMonths = horizonMonths,
    byCategory = byCategory.map { it.toDomain() },
    expectedMonthlyExpense = expectedMonthlyExpense,
    metrics = PredictionMetrics(mae = mae, rmse = rmse, r2 = r2),
    createdAt = parseInstantOrNow(createdAt),
)

private fun PredictExpensesCategoryDto.toDomain(): ExpenseCategoryPrediction =
    ExpenseCategoryPrediction(
        category = expenseCategoryOrNull(category),
        rawCategory = category,
        predictedAmount = predictedAmount,
    )

// ============================================================================
// DTO -> Domain (Wealth)
// ============================================================================

fun PredictWealthResponseDto.toDomain(): WealthPrediction = WealthPrediction(
    predictionId = predictionId,
    modelName = modelName,
    horizonMonths = horizonMonths,
    history = history.map { WealthHistoryPoint(it.monthIndex, it.amount) },
    projection = projection.map { WealthPredictionPoint(it.monthIndex, it.predictedAmount) },
    expectedFinalWealth = expectedFinalWealth,
    monthlyGrowthRate = monthlyGrowthRate,
    metrics = PredictionMetrics(mae = mae, rmse = rmse, r2 = r2),
    createdAt = parseInstantOrNow(createdAt),
)

// ============================================================================
// DTO -> Domain (Summary)
// ============================================================================

fun PredictionSummaryResponseDto.toDomain(): PredictionSummary = PredictionSummary(
    id = id,
    modelName = modelName,
    errorMetric = errorMetric,
    createdAt = parseInstantOrNow(createdAt),
)

// ============================================================================
// DTO -> Domain (Calibrated Simulation)
// ============================================================================

fun RunCalibratedSimulationResponseDto.toDomain(): CalibratedSimulation = CalibratedSimulation(
    // Usa o mapper DTO->Domain direto (SimulationMappers.kt) para preservar a
    // `trajectory` do fan chart, que o caminho DTO->Entity->Domain descartaria
    // (a entity nao guarda a trajetoria).
    simulation = simulation.toDomain(),
    calibration = calibration.toDomain(),
)

fun CalibrationSummaryResponseDto.toDomain(): CalibrationSummary = CalibrationSummary(
    incomePredictionId = incomePredictionId,
    expensePredictionId = expensePredictionId,
    predictedMonthlyIncome = predictedMonthlyIncome,
    predictedMonthlyExpense = predictedMonthlyExpense,
    rawMonthlyContribution = rawMonthlyContribution,
    appliedMonthlyContribution = appliedMonthlyContribution,
    appliedVolatilityAnnual = appliedVolatilityAnnual,
)

// ============================================================================
// Domain -> Request DTO
// ============================================================================

fun CalibratedSimulationParameters.toRequestDto(): RunCalibratedSimulationRequestDto =
    RunCalibratedSimulationRequestDto(
        goalId = goalId.toString(),
        initialCapital = initialCapital,
        expectedReturnAnnual = expectedReturnAnnual,
        volatilityAnnual = volatilityAnnual,
        horizonMonths = horizonMonths,
        targetAmount = targetAmount,
        unemploymentProbAnnual = unemploymentProbAnnual,
        unemploymentDurationMonths = unemploymentDurationMonths,
        inflationAnnual = inflationAnnual,
        numSimulations = numSimulations,
        seed = seed,
        incomeHorizonMonths = incomeHorizonMonths,
    )

// ============================================================================
// Helpers internos
// ============================================================================

/**
 * Converte string do backend para [ExpenseCategory], retornando null em
 * categorias desconhecidas. Faz o lookup case-insensitive como defesa
 * contra inconsistencias entre Python (UPPERCASE) e Kotlin.
 */
private fun expenseCategoryOrNull(raw: String): ExpenseCategory? =
    runCatching { ExpenseCategory.valueOf(raw.uppercase()) }.getOrNull()

/**
 * Parse de ISO-8601 com fallback para `Instant.now()` em vez de crash.
 * Diferente de UI direta, aqui nao podemos lancar - o resultado da
 * simulacao ja foi calculado, perder o timestamp e mau, perder a
 * tela inteira eh pior.
 */
private fun parseInstantOrNow(iso: String): Instant =
    runCatching { Instant.parse(iso) }.getOrElse { Instant.now() }
