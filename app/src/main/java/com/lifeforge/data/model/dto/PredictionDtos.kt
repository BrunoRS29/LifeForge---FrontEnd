package com.lifeforge.data.model.dto

import kotlinx.serialization.Serializable

/**
 * DTOs do subsistema de predicoes/IA (Sprint 5).
 *
 * Casamento exato com o contrato publico do backend Ktor:
 *  - `com.lifeforge.dto.PredictionDtos` (renome em PT/EN preservado em camelCase)
 *  - Endpoints: POST /api/v1/predictions/income, POST /api/v1/predictions/expenses,
 *    GET /api/v1/predictions, POST /api/v1/simulation/run-calibrated
 *
 * Convencao: nomes em camelCase no JSON (o backend ja serializa assim
 * via kotlinx.serialization sem @SerialName). Diferente dos
 * `MlClientDtos.kt` que vivem no backend e usam snake_case porque
 * conversam com o Python.
 */

// ============================================================================
// POST /api/v1/predictions/income
// ============================================================================

@Serializable
data class PredictIncomeRequestDto(
    val horizonMonths: Int,
)

@Serializable
data class PredictIncomePointDto(
    val monthIndex: Int,
    val predictedAmount: Double,
)

@Serializable
data class PredictIncomeResponseDto(
    val predictionId: Long,
    val modelName: String,
    val horizonMonths: Int,
    val projection: List<PredictIncomePointDto>,
    val expectedMonthlyIncome: Double,
    val annualGrowthRate: Double,
    val residualVolatilityMonthly: Double,
    val mae: Double,
    val rmse: Double,
    val r2: Double,
    val createdAt: String,
)

// ============================================================================
// POST /api/v1/predictions/expenses
// ============================================================================

@Serializable
data class PredictExpensesRequestDto(
    val horizonMonths: Int = 1,
)

@Serializable
data class PredictExpensesCategoryDto(
    val category: String,
    val predictedAmount: Double,
)

@Serializable
data class PredictExpensesResponseDto(
    val predictionId: Long,
    val modelName: String,
    val horizonMonths: Int,
    val byCategory: List<PredictExpensesCategoryDto>,
    val expectedMonthlyExpense: Double,
    val mae: Double,
    val rmse: Double,
    val r2: Double,
    val createdAt: String,
)

// ============================================================================
// POST /api/v1/predictions/wealth
// ============================================================================

@Serializable
data class PredictWealthRequestDto(
    val horizonMonths: Int = 12,
)

@Serializable
data class WealthHistoryPointDto(
    val monthIndex: Int,
    val amount: Double,
)

@Serializable
data class PredictWealthPointDto(
    val monthIndex: Int,
    val predictedAmount: Double,
)

@Serializable
data class PredictWealthResponseDto(
    val predictionId: Long,
    val modelName: String,
    val horizonMonths: Int,
    val history: List<WealthHistoryPointDto>,
    val projection: List<PredictWealthPointDto>,
    val expectedFinalWealth: Double,
    val monthlyGrowthRate: Double,
    val mae: Double,
    val rmse: Double,
    val r2: Double,
    val createdAt: String,
)

// ============================================================================
// GET /api/v1/predictions
// ============================================================================

@Serializable
data class PredictionSummaryResponseDto(
    val id: Long,
    val modelName: String,
    val errorMetric: Double?,
    val createdAt: String,
)

// ============================================================================
// POST /api/v1/simulation/run-calibrated
// ============================================================================

/**
 * Request da simulacao calibrada. Note a AUSENCIA de `monthlyContribution`:
 * o backend vai derivar `expected_income - expected_expenses` a partir
 * das predicoes que ele mesmo dispara antes de rodar o Monte Carlo.
 */
@Serializable
data class RunCalibratedSimulationRequestDto(
    val goalId: String,
    val initialCapital: Double,
    // Premissas de mercado: nulas => backend preenche pela base calibrada ao
    // perfil (perfil de risco + vinculo). Com explicitNulls=false no Json, os
    // campos nulos sao omitidos do corpo enviado.
    val expectedReturnAnnual: Double? = null,
    val volatilityAnnual: Double? = null,
    val horizonMonths: Int,
    val targetAmount: Double,
    val unemploymentProbAnnual: Double? = null,
    val unemploymentDurationMonths: Int? = null,
    val inflationAnnual: Double? = null,
    val numSimulations: Int = 10_000,
    val seed: Long? = null,
    val incomeHorizonMonths: Int = 12,
)

@Serializable
data class CalibrationSummaryResponseDto(
    val incomePredictionId: Long,
    val expensePredictionId: Long,
    val predictedMonthlyIncome: Double,
    val predictedMonthlyExpense: Double,
    val rawMonthlyContribution: Double,
    val appliedMonthlyContribution: Double,
    val appliedVolatilityAnnual: Double,
)

/**
 * Response da simulacao calibrada: SimulationResultResponseDto (Sprint 2)
 * + sumario da calibracao. Reaproveita o tipo ja existente para o motor
 * de gráficos da SimulationScreen funcionar sem mudança.
 */
@Serializable
data class RunCalibratedSimulationResponseDto(
    val simulation: SimulationResultResponseDto,
    val calibration: CalibrationSummaryResponseDto,
)
