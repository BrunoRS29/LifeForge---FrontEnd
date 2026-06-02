package com.lifeforge.domain.model

import java.time.Instant

/**
 * Modelos de dominio do subsistema de IA preditiva (Sprint 5).
 *
 * Mantemos separados dos DTOs Retrofit por dois motivos:
 *  1. A UI nao deve depender de strings opacas (`category: String`)
 *     vindas do backend. Aqui convertemos para o enum `ExpenseCategory`
 *     existente em `Enums.kt` (Sprint 1).
 *  2. `createdAt` vem como String ISO do backend - aqui ja eh `Instant`,
 *     pronto para passar pelos Formatters.
 *
 * Decisao: NAO temos cache local (Room) para predicoes nesta sprint. O
 * backend ja persiste em `predictions`. Cachear no app duplicaria storage
 * sem grande beneficio - predicoes sao geradas sob demanda quando o
 * usuario aperta "Simular com IA".
 */

// ============================================================================
// Predicao de renda
// ============================================================================

data class IncomePrediction(
    val predictionId: Long,
    val modelName: String,
    val horizonMonths: Int,
    val projection: List<IncomePredictionPoint>,
    /** Renda mensal media projetada - usada na calibracao do Monte Carlo. */
    val expectedMonthlyIncome: Double,
    /** Crescimento anual estimado (fracional, ex.: 0.10 = +10%/ano). */
    val annualGrowthRate: Double,
    /**
     * Desvio padrao dos residuos do modelo de regressao - proxy da
     * volatilidade da renda. Usado para calibrar `volatilityAnnual`.
     */
    val residualVolatilityMonthly: Double,
    val metrics: PredictionMetrics,
    val createdAt: Instant,
)

data class IncomePredictionPoint(
    val monthIndex: Int,
    val predictedAmount: Double,
)

// ============================================================================
// Predicao de despesa
// ============================================================================

data class ExpensePrediction(
    val predictionId: Long,
    val modelName: String,
    val horizonMonths: Int,
    val byCategory: List<ExpenseCategoryPrediction>,
    val expectedMonthlyExpense: Double,
    val metrics: PredictionMetrics,
    val createdAt: Instant,
)

/**
 * `category` eh nullable para acomodar valores que o backend possa
 * adicionar no futuro (defensive parsing). Na UI, mostramos os
 * desconhecidos sob "Outros".
 */
data class ExpenseCategoryPrediction(
    val category: ExpenseCategory?,
    val rawCategory: String,
    val predictedAmount: Double,
)

// ============================================================================
// Metricas compartilhadas
// ============================================================================

data class PredictionMetrics(
    val mae: Double,
    val rmse: Double,
    /** R^2 pode ser negativo (modelo pior que media constante). */
    val r2: Double,
)

// ============================================================================
// Sumario de auditoria (GET /predictions)
// ============================================================================

data class PredictionSummary(
    val id: Long,
    val modelName: String,
    val errorMetric: Double?,
    val createdAt: Instant,
)

// ============================================================================
// Simulacao calibrada
// ============================================================================

/**
 * Resultado completo da simulacao calibrada.
 *
 * Reaproveita o [SimulationResult] da Sprint 2 (mesma estrutura de
 * histograma, percentis, etc.) e adiciona o sumario da calibracao.
 * A UI mostra primeiro a calibracao (renda + despesa derivadas) e
 * depois os graficos de Monte Carlo - dois cards em sequencia.
 */
data class CalibratedSimulation(
    val simulation: SimulationResult,
    val calibration: CalibrationSummary,
)

/**
 * Sumario da calibracao - explica AO USUARIO como os parametros foram
 * derivados. Indispensavel para o requisito de transparencia em ML
 * documentado no Capitulo 4 do TCC.
 *
 * @property rawMonthlyContribution renda - despesa, pode ser negativo
 * @property appliedMonthlyContribution maximo entre raw e zero (capping)
 * @property appliedVolatilityAnnual maximo entre volatilidade de mercado
 *   informada pelo usuario e a sigma anualizada da renda
 */
data class CalibrationSummary(
    val incomePredictionId: Long,
    val expensePredictionId: Long,
    val predictedMonthlyIncome: Double,
    val predictedMonthlyExpense: Double,
    val rawMonthlyContribution: Double,
    val appliedMonthlyContribution: Double,
    val appliedVolatilityAnnual: Double,
) {
    /** True quando o capping zerou a contribuicao (despesa >= renda). */
    val cappedToZero: Boolean get() = rawMonthlyContribution < 0.0

    /** Diferenca entre o raw e o aplicado - 0 quando nao houve capping. */
    val cappingDelta: Double get() = appliedMonthlyContribution - rawMonthlyContribution
}

/**
 * Parametros para disparar uma simulacao calibrada. Note a AUSENCIA
 * de `monthlyContribution` - eh exatamente o que sera derivado pelo
 * backend a partir das predicoes.
 */
data class CalibratedSimulationParameters(
    val goalId: Long,
    val initialCapital: Double,
    val expectedReturnAnnual: Double,
    val volatilityAnnual: Double,
    val horizonMonths: Int,
    val targetAmount: Double,
    val unemploymentProbAnnual: Double = 0.0,
    val unemploymentDurationMonths: Int = 6,
    val inflationAnnual: Double = 0.0,
    val numSimulations: Int = 10_000,
    val seed: Long? = null,
    /** Horizonte usado APENAS para calcular a media da renda projetada. */
    val incomeHorizonMonths: Int = 12,
)
