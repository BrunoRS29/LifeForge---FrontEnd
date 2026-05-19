package com.lifeforge.domain.model

/**
 * Modelos de domínio do módulo de otimização (Sprint 3 / endpoints
 * `/api/v1/optimize/{contribution,horizon,rebalance}`).
 */

/** Tipo de problema de otimização sendo resolvido. */
enum class OptimizationType { OPTIMAL_CONTRIBUTION, OPTIMAL_HORIZON }

/** Razão pela qual a busca terminou. */
enum class TerminationReason {
    /** Convergiu dentro da tolerância — solução confiável. */
    CONVERGED,

    /** Estourou o teto de iterações antes de convergir. */
    MAX_ITERATIONS,

    /** Nem o teto da busca atinge a probabilidade alvo (infeasible). */
    INFEASIBLE_UPPER_BOUND,

    /** Limite inferior já bastante para atingir o alvo. */
    LOWER_BOUND_SUFFICIENT,
}

/**
 * Um passo da busca binária. Útil para visualizar a convergência da
 * otimização num gráfico (probabilidade x candidato) na UI.
 */
data class IterationStep(
    val index: Int,
    val candidate: Double,
    val measuredProbability: Double,
    val lowerBound: Double,
    val upperBound: Double,
)

/**
 * Resultado da rodada de verificação final da otimização.
 *
 * Tem a mesma estrutura de [SimulationResult], mas sem id/goalId/createdAt
 * — a otimização não é persistida (cliente decide se quer guardar).
 */
data class OptimizationVerification(
    val numSimulations: Int,
    val successProbability: Double,
    val mean: Double,
    val median: Double,
    val standardDeviation: Double,
    val percentiles: Map<String, Double>,
    val worstCase: Double,
    val bestCase: Double,
    val meanReal: Double,
    val histogram: List<HistogramBucket>,
)

/**
 * Resultado unificado para `/optimize/contribution` e `/optimize/horizon`.
 *
 * @param optimalValue aporte ideal (R$/mês) ou horizonte ideal (meses),
 *        dependendo de [type].
 * @param verification null quando [feasible] é false.
 */
data class OptimizationResult(
    val type: OptimizationType,
    val feasible: Boolean,
    val optimalValue: Double,
    val achievedProbability: Double,
    val targetProbability: Double,
    val terminationReason: TerminationReason,
    val iterations: List<IterationStep>,
    val verification: OptimizationVerification?,
    val executionTimeMs: Long,
    val seed: Long,
)

/**
 * Resultado do endpoint `/optimize/rebalance`.
 *
 * @param weights chave = [AssetType.name], valores somam 1.0 (validado
 *        no backend — cliente pode confiar).
 */
data class RebalanceResult(
    val weights: Map<AssetType, Double>,
    val expectedReturnAnnual: Double,
    val volatilityAnnual: Double,
    val riskScore: Double,
    val rationale: String,
)
