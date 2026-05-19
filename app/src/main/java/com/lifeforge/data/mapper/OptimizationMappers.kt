package com.lifeforge.data.mapper

import com.lifeforge.data.model.dto.IterationStepDto
import com.lifeforge.data.model.dto.OptimizationResponseDto
import com.lifeforge.data.model.dto.OptimizeContributionRequestDto
import com.lifeforge.data.model.dto.OptimizeHorizonRequestDto
import com.lifeforge.data.model.dto.RebalanceRequestDto
import com.lifeforge.data.model.dto.RebalanceResponseDto
import com.lifeforge.data.model.dto.VerificationResultDto
import com.lifeforge.domain.model.AssetType
import com.lifeforge.domain.model.IterationStep
import com.lifeforge.domain.model.OptimizationResult
import com.lifeforge.domain.model.OptimizationType
import com.lifeforge.domain.model.OptimizationVerification
import com.lifeforge.domain.model.RebalanceResult
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.model.TerminationReason

/**
 * Mappers do agregado de otimização. Sem persistência — sempre DTO → Domain.
 *
 * O `histogram` é convertido bucket a bucket porque a verificação usa o
 * mesmo formato da Simulation, mas o domain quer [HistogramBucket] (sem
 * `Dto`).
 */

// ============================================================================
// DTO → Domain
// ============================================================================

fun OptimizationResponseDto.toDomain(): OptimizationResult = OptimizationResult(
    type = OptimizationType.valueOf(type),
    feasible = feasible,
    optimalValue = optimalValue,
    achievedProbability = achievedProbability,
    targetProbability = targetProbability,
    terminationReason = TerminationReason.valueOf(terminationReason),
    iterations = iterations.map { it.toDomain() },
    verification = verification?.toDomain(),
    executionTimeMs = executionTimeMs,
    seed = seed,
)

fun IterationStepDto.toDomain(): IterationStep = IterationStep(
    index = index,
    candidate = candidate,
    measuredProbability = measuredProbability,
    lowerBound = lowerBound,
    upperBound = upperBound,
)

fun VerificationResultDto.toDomain(): OptimizationVerification = OptimizationVerification(
    numSimulations = numSimulations,
    successProbability = successProbability,
    mean = mean,
    median = median,
    standardDeviation = standardDeviation,
    percentiles = percentiles,
    worstCase = worstCase,
    bestCase = bestCase,
    meanReal = meanReal,
    histogram = histogram.map { it.toDomain() },
)

fun RebalanceResponseDto.toDomain(): RebalanceResult = RebalanceResult(
    weights = weights.mapKeys { (key, _) -> AssetType.valueOf(key) },
    expectedReturnAnnual = expectedReturnAnnual,
    volatilityAnnual = volatilityAnnual,
    riskScore = riskScore,
    rationale = rationale,
)

// ============================================================================
// Request builders — produzidos pelos métodos do RepositoryImpl
// ============================================================================

fun optimizeContributionRequestDto(
    goalId: Long?,
    initialCapital: Double,
    expectedReturnAnnual: Double,
    volatilityAnnual: Double,
    targetAmount: Double,
    horizonMonths: Int,
    targetSuccessProbability: Double,
    seed: Long?,
): OptimizeContributionRequestDto = OptimizeContributionRequestDto(
    goalId = goalId?.toString(),
    initialCapital = initialCapital,
    expectedReturnAnnual = expectedReturnAnnual,
    volatilityAnnual = volatilityAnnual,
    targetAmount = targetAmount,
    horizonMonths = horizonMonths,
    targetSuccessProbability = targetSuccessProbability,
    seed = seed,
)

fun optimizeHorizonRequestDto(
    goalId: Long?,
    initialCapital: Double,
    expectedReturnAnnual: Double,
    volatilityAnnual: Double,
    targetAmount: Double,
    monthlyContribution: Double,
    targetSuccessProbability: Double,
    seed: Long?,
): OptimizeHorizonRequestDto = OptimizeHorizonRequestDto(
    goalId = goalId?.toString(),
    initialCapital = initialCapital,
    expectedReturnAnnual = expectedReturnAnnual,
    volatilityAnnual = volatilityAnnual,
    targetAmount = targetAmount,
    monthlyContribution = monthlyContribution,
    targetSuccessProbability = targetSuccessProbability,
    seed = seed,
)

fun rebalanceRequestDto(
    riskProfile: RiskProfile,
    currentCapital: Double,
    targetAmount: Double,
    monthsToGoal: Int,
): RebalanceRequestDto = RebalanceRequestDto(
    riskProfile = riskProfile.name,
    currentCapital = currentCapital,
    targetAmount = targetAmount,
    monthsToGoal = monthsToGoal,
)
