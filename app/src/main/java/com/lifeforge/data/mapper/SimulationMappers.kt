package com.lifeforge.data.mapper

import com.lifeforge.data.db.entity.SimulationEntity
import com.lifeforge.data.db.entity.SimulationSummaryEntity
import com.lifeforge.data.model.dto.HistogramBucketDto
import com.lifeforge.data.model.dto.RunSimulationRequestDto
import com.lifeforge.data.model.dto.SimulationInputsDto
import com.lifeforge.data.model.dto.SimulationResultResponseDto
import com.lifeforge.data.model.dto.SimulationSummaryResponseDto
import com.lifeforge.data.model.dto.TrajectoryBandDto
import com.lifeforge.domain.model.HistogramBucket
import com.lifeforge.domain.model.SimulationInputs
import com.lifeforge.domain.model.SimulationParameters
import com.lifeforge.domain.model.SimulationResult
import com.lifeforge.domain.model.SimulationSummary
import com.lifeforge.domain.model.TrajectoryBand
import java.time.Instant

/**
 * Mappers do agregado de simulação Monte Carlo.
 *
 * Particularidade: IDs vêm como String no wire (decisão do backend para
 * compat. com clientes JS) e como Long no domain/entity. Conversão é
 * feita aqui — falha de parse implica contrato quebrado.
 */

// ============================================================================
// DTO → Entity (cache do resultado completo após /simulation/run)
// ============================================================================

fun SimulationResultResponseDto.toEntity(): SimulationEntity = SimulationEntity(
    id = id.toLong(),
    goalId = goalId.toLong(),
    numSimulations = numSimulations,
    seed = seed,
    targetAmount = targetAmount,
    successProbability = successProbability,
    mean = mean,
    median = median,
    standardDeviation = standardDeviation,
    percentiles = percentiles,
    worstCase = worstCase,
    bestCase = bestCase,
    meanReal = meanReal,
    histogram = histogram,
    executionTimeMs = executionTimeMs,
    createdAt = Instant.parse(createdAt),
    trajectory = trajectory,
    inputs = inputs,
)

/** Resumo para o histórico local, a partir do resultado completo de uma rodada. */
fun SimulationResultResponseDto.toSummaryEntity(): SimulationSummaryEntity = SimulationSummaryEntity(
    id = id.toLong(),
    goalId = goalId.toLong(),
    successProbability = successProbability,
    mean = mean,
    median = median,
    targetAmount = targetAmount,
    createdAt = Instant.parse(createdAt),
    inputs = inputs,
)

// ============================================================================
// DTO → Domain (direto) — preserva a `trajectory` do fan chart, que NÃO é
// persistida na entity. Usado pela rodada fresca (/run, /run-calibrated).
// ============================================================================

fun SimulationResultResponseDto.toDomain(): SimulationResult = SimulationResult(
    id = id.toLong(),
    goalId = goalId.toLong(),
    numSimulations = numSimulations,
    seed = seed,
    targetAmount = targetAmount,
    successProbability = successProbability,
    mean = mean,
    median = median,
    standardDeviation = standardDeviation,
    percentiles = percentiles,
    worstCase = worstCase,
    bestCase = bestCase,
    meanReal = meanReal,
    histogram = histogram.map { it.toDomain() },
    trajectory = trajectory.map { it.toDomain() },
    executionTimeMs = executionTimeMs,
    createdAt = Instant.parse(createdAt),
    inputs = inputs?.toDomain(),
)

fun TrajectoryBandDto.toDomain(): TrajectoryBand =
    TrajectoryBand(monthIndex, p10, p25, p50, p75, p90)

// ============================================================================
// Entity → Domain
// ============================================================================

fun SimulationEntity.toDomain(): SimulationResult = SimulationResult(
    id = id,
    goalId = goalId,
    numSimulations = numSimulations,
    seed = seed,
    targetAmount = targetAmount,
    successProbability = successProbability,
    mean = mean,
    median = median,
    standardDeviation = standardDeviation,
    percentiles = percentiles,
    worstCase = worstCase,
    bestCase = bestCase,
    meanReal = meanReal,
    histogram = histogram.map { it.toDomain() },
    trajectory = trajectory.map { it.toDomain() },
    executionTimeMs = executionTimeMs,
    createdAt = createdAt,
    inputs = inputs?.toDomain(),
)

/**
 * Versão "summary": Entity → SimulationSummary (sem histograma/percentiles).
 * Usada pela listagem da tela de detalhe de meta para evitar carregar
 * payload pesado quando só precisamos dos dados resumidos.
 */
fun SimulationEntity.toSummary(): SimulationSummary = SimulationSummary(
    id = id,
    goalId = goalId,
    successProbability = successProbability,
    mean = mean,
    median = median,
    targetAmount = targetAmount,
    createdAt = createdAt,
    inputs = inputs?.toDomain(),
)

// Histórico resumido (GET /by-goal/{id}) → cache local e domínio.
fun SimulationSummaryResponseDto.toEntity(): SimulationSummaryEntity = SimulationSummaryEntity(
    id = id.toLong(),
    goalId = goalId.toLong(),
    successProbability = successProbability,
    mean = mean,
    median = median,
    targetAmount = targetAmount,
    createdAt = Instant.parse(createdAt),
    inputs = inputs,
)

fun SimulationSummaryEntity.toDomain(): SimulationSummary = SimulationSummary(
    id = id,
    goalId = goalId,
    successProbability = successProbability,
    mean = mean,
    median = median,
    targetAmount = targetAmount,
    createdAt = createdAt,
    inputs = inputs?.toDomain(),
)

fun SimulationInputsDto.toDomain(): SimulationInputs = SimulationInputs(
    initialCapital = initialCapital,
    monthlyContribution = monthlyContribution,
    expectedReturnAnnual = expectedReturnAnnual,
    volatilityAnnual = volatilityAnnual,
    horizonMonths = horizonMonths,
    targetAmount = targetAmount,
    unemploymentProbAnnual = unemploymentProbAnnual,
    inflationAnnual = inflationAnnual,
    numSimulations = numSimulations,
    calibrated = calibrated,
)

fun SimulationSummaryResponseDto.toDomain(): SimulationSummary = SimulationSummary(
    id = id.toLong(),
    goalId = goalId.toLong(),
    successProbability = successProbability,
    mean = mean,
    median = median,
    targetAmount = targetAmount,
    createdAt = Instant.parse(createdAt),
)

// ============================================================================
// HistogramBucket — DTO ↔ Domain
// ============================================================================

fun HistogramBucketDto.toDomain(): HistogramBucket =
    HistogramBucket(rangeStart, rangeEnd, count)

// ============================================================================
// Request builder
// ============================================================================

fun SimulationParameters.toRequestDto(): RunSimulationRequestDto = RunSimulationRequestDto(
    goalId = goalId.toString(),
    initialCapital = initialCapital,
    monthlyContribution = monthlyContribution,
    expectedReturnAnnual = expectedReturnAnnual,
    volatilityAnnual = volatilityAnnual,
    horizonMonths = horizonMonths,
    targetAmount = targetAmount,
    unemploymentProbAnnual = unemploymentProbAnnual,
    unemploymentDurationMonths = unemploymentDurationMonths,
    inflationAnnual = inflationAnnual,
    numSimulations = numSimulations,
    seed = seed,
)
