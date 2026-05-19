package com.lifeforge.data.model.dto

import kotlinx.serialization.Serializable

/**
 * DTOs do endpoint `/api/v1/optimize/`.
 * Espelha `com.lifeforge.dto.OptimizationDtos.kt` do backend.
 */

// ============================================================================
// Requests
// ============================================================================

@Serializable
data class OptimizeContributionRequestDto(
    val goalId: String? = null,
    val initialCapital: Double,
    val expectedReturnAnnual: Double,
    val volatilityAnnual: Double,
    val targetAmount: Double,
    val horizonMonths: Int,
    val targetSuccessProbability: Double = 0.80,
    val unemploymentProbAnnual: Double = 0.0,
    val unemploymentDurationMonths: Int = 6,
    val inflationAnnual: Double = 0.0,
    val maxContribution: Double? = null,
    val simulationsPerStep: Int = 2_000,
    val verificationSimulations: Int = 10_000,
    val seed: Long? = null,
)

@Serializable
data class OptimizeHorizonRequestDto(
    val goalId: String? = null,
    val initialCapital: Double,
    val expectedReturnAnnual: Double,
    val volatilityAnnual: Double,
    val targetAmount: Double,
    val monthlyContribution: Double,
    val targetSuccessProbability: Double = 0.80,
    val unemploymentProbAnnual: Double = 0.0,
    val unemploymentDurationMonths: Int = 6,
    val inflationAnnual: Double = 0.0,
    val maxHorizonMonths: Int = 600,
    val simulationsPerStep: Int = 2_000,
    val verificationSimulations: Int = 10_000,
    val seed: Long? = null,
)

@Serializable
data class RebalanceRequestDto(
    val riskProfile: String,
    val currentCapital: Double,
    val targetAmount: Double,
    val monthsToGoal: Int,
)

// ============================================================================
// Responses
// ============================================================================

@Serializable
data class IterationStepDto(
    val index: Int,
    val candidate: Double,
    val measuredProbability: Double,
    val lowerBound: Double,
    val upperBound: Double,
)

@Serializable
data class VerificationResultDto(
    val numSimulations: Int,
    val successProbability: Double,
    val mean: Double,
    val median: Double,
    val standardDeviation: Double,
    val percentiles: Map<String, Double>,
    val worstCase: Double,
    val bestCase: Double,
    val meanReal: Double,
    val histogram: List<HistogramBucketDto>,
)

@Serializable
data class OptimizationResponseDto(
    val type: String,                  // OPTIMAL_CONTRIBUTION | OPTIMAL_HORIZON
    val feasible: Boolean,
    val optimalValue: Double,
    val achievedProbability: Double,
    val targetProbability: Double,
    val terminationReason: String,
    val iterations: List<IterationStepDto>,
    val verification: VerificationResultDto?,
    val executionTimeMs: Long,
    val seed: Long,
)

@Serializable
data class RebalanceResponseDto(
    val weights: Map<String, Double>,  // chave = AssetType.name, valores somam 1.0
    val expectedReturnAnnual: Double,
    val volatilityAnnual: Double,
    val riskScore: Double,
    val rationale: String,
)