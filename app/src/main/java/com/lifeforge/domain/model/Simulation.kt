package com.lifeforge.domain.model

import java.time.Instant

/**
 * Parâmetros para uma rodada de simulação Monte Carlo.
 *
 * Espelham o `MonteCarloParameters` do backend, mas em domínio Android:
 * os valores chegam como Double pelo wire (decisão do backend) e ficam
 * assim no domínio para alinhar com a engine remota — não há cálculo
 * monetário cumulativo no cliente.
 */
data class SimulationParameters(
    val goalId: Long,
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

/**
 * Bucket de histograma da distribuição de patrimônios finais.
 * Usado pelo gráfico de histograma da Fase 4.4.
 */
data class HistogramBucket(
    val rangeStart: Double,
    val rangeEnd: Double,
    val count: Int,
)

/**
 * Resultado completo de uma simulação Monte Carlo.
 *
 * - `percentiles` usa chaves "P5", "P10", ..., "P95" (decisão wire-format
 *   herdada do backend para evitar serializadores customizados).
 * - `meanReal` é a média descontada da inflação anual informada.
 * - `histogram` tem tipicamente 30-50 buckets — suficiente para renderizar
 *   um histograma legível sem inflar o payload.
 */
data class SimulationResult(
    val id: Long,
    val goalId: Long,
    val numSimulations: Int,
    val seed: Long,
    val targetAmount: Double,
    val successProbability: Double,
    val mean: Double,
    val median: Double,
    val standardDeviation: Double,
    val percentiles: Map<String, Double>,
    val worstCase: Double,
    val bestCase: Double,
    val meanReal: Double,
    val histogram: List<HistogramBucket>,
    val executionTimeMs: Long,
    val createdAt: Instant,
)

/**
 * Versão resumida usada na listagem de simulações por meta — sem o
 * histograma para reduzir payload.
 */
data class SimulationSummary(
    val id: Long,
    val goalId: Long,
    val successProbability: Double,
    val mean: Double,
    val median: Double,
    val targetAmount: Double,
    val createdAt: Instant,
)
