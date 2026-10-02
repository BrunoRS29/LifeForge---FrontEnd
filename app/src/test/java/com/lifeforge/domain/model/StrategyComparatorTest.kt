package com.lifeforge.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/** Comparação lado a lado de estratégias (proposta, Seção 8.3). */
class StrategyComparatorTest {

    private fun result(
        id: Long,
        probability: Double,
        p10: Double = 600_000.0,
        median: Double = 1_200_000.0,
        contribution: Double? = 2_000.0,
        returnAnnual: Double = 0.08,
    ) = SimulationResult(
        id = id,
        goalId = 1,
        numSimulations = 10_000,
        seed = 42,
        targetAmount = 1_000_000.0,
        successProbability = probability,
        mean = median * 1.1,
        median = median,
        standardDeviation = 500_000.0,
        percentiles = mapOf("P10" to p10, "P50" to median, "P90" to median * 1.8),
        worstCase = p10 * 0.8,
        bestCase = median * 2.2,
        meanReal = median * 0.5,
        histogram = emptyList(),
        executionTimeMs = 5,
        createdAt = Instant.parse("2026-10-0${id}T00:00:00Z"),
        inputs = contribution?.let {
            SimulationInputs(
                initialCapital = 50_000.0,
                monthlyContribution = it,
                expectedReturnAnnual = returnAnnual,
                volatilityAnnual = 0.15,
                horizonMonths = 240,
                targetAmount = 1_000_000.0,
            )
        },
    )

    @Test
    fun `estrategia com mais chance de sucesso e a recomendada`() {
        val a = result(1, probability = 0.657)
        val b = result(2, probability = 0.781, p10 = 750_000.0, contribution = 2_500.0)

        val c = StrategyComparator.compare(a, b)

        assertThat(c.recommended).isEqualTo(StrategySide.B)
        assertThat(c.technicalTie).isFalse()
        assertThat(c.probabilityDeltaPp).isWithin(1e-9).of(12.4)
        val success = c.outcomes.first { it.metric == StrategyMetric.SUCCESS_PROBABILITY }
        assertThat(success.better).isEqualTo(StrategySide.B)
        val p10 = c.outcomes.first { it.metric == StrategyMetric.PESSIMISTIC_P10 }
        assertThat(p10.better).isEqualTo(StrategySide.B)
    }

    @Test
    fun `premissas sao comparadas sem eleger um lado melhor`() {
        val c = StrategyComparator.compare(result(1, 0.6), result(2, 0.7, contribution = 3_000.0))

        val contribution = c.premises.first { it.metric == StrategyMetric.MONTHLY_CONTRIBUTION }
        assertThat(contribution.differs).isTrue()
        assertThat(contribution.better).isNull()
        assertThat(c.premises.first { it.metric == StrategyMetric.VOLATILITY }.differs).isFalse()
    }

    @Test
    fun `empate tecnico abaixo de 1 pp desempata pelo menor aporte`() {
        val a = result(1, probability = 0.652, contribution = 2_500.0)
        val b = result(2, probability = 0.657, contribution = 2_000.0)

        val c = StrategyComparator.compare(a, b)

        assertThat(c.technicalTie).isTrue()
        assertThat(c.recommended).isEqualTo(StrategySide.B)
    }

    @Test
    fun `empate tecnico com o mesmo aporte desempata pelo cenario pessimista`() {
        val a = result(1, probability = 0.66, p10 = 700_000.0)
        val b = result(2, probability = 0.655, p10 = 600_000.0)

        assertThat(StrategyComparator.compare(a, b).recommended).isEqualTo(StrategySide.A)
    }

    @Test
    fun `sem premissas de alguma rodada a tabela de premissas fica vazia`() {
        val c = StrategyComparator.compare(result(1, 0.5, contribution = null), result(2, 0.6))

        assertThat(c.premises).isEmpty()
        assertThat(c.outcomes).isNotEmpty()
    }

    @Test
    fun `percentil ausente recua para o pior e o melhor caso`() {
        val r = result(1, 0.5).copy(percentiles = emptyMap())

        assertThat(r.p10()).isEqualTo(r.worstCase)
        assertThat(r.p90()).isEqualTo(r.bestCase)
    }
}
