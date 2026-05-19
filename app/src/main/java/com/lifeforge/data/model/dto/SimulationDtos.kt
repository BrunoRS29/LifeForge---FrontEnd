package com.lifeforge.data.model.dto

import kotlinx.serialization.Serializable

/**
 * DTOs do endpoint `/api/v1/simulation/`.
 *
 * Espelha exatamente `com.lifeforge.dto.SimulationDtos.kt` do backend.
 * IDs Long viram String para evitar problemas com integers >2^53 em JS
 * (decisão herdada do backend; mantemos por compatibilidade).
 */

@Serializable
data class RunSimulationRequestDto(
    val goalId: String,
    val initialCapital: Double,
    val monthlyContribution: Double,
    val expectedReturnAnnual: Double,
    val volatilityAnnual: Double,
    val horizonMonths: Int,
    val targetAmount: Double,
    val unemploymentProbAnnual: Double = 0.0,
    val unemploymentDurationMonths: Int = 6,
    val inflationAnnual: Double = 0.0,
    val numSimulations: Int = 10_000,
    val seed: Long? = null,
)

@Serializable
data class HistogramBucketDto(
    val rangeStart: Double,
    val rangeEnd: Double,
    val count: Int,
)

@Serializable
data class SimulationResultResponseDto(
    val id: String,
    val goalId: String,
    val numSimulations: Int,
    val seed: Long,
    val targetAmount: Double,
    val successProbability: Double,
    val mean: Double,
    val median: Double,
    val standardDeviation: Double,
    val percentiles: Map<String, Double>,  // "P5", "P10", ..., "P95"
    val worstCase: Double,
    val bestCase: Double,
    val meanReal: Double,
    val histogram: List<HistogramBucketDto>,
    val executionTimeMs: Long,
    val createdAt: String,
)

@Serializable
data class SimulationSummaryResponseDto(
    val id: String,
    val goalId: String,
    val successProbability: Double,
    val mean: Double,
    val median: Double,
    val targetAmount: Double,
    val createdAt: String,
)