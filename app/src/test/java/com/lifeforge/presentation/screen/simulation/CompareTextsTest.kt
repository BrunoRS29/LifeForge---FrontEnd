package com.lifeforge.presentation.screen.simulation

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.SimulationInputs
import com.lifeforge.domain.model.SimulationResult
import com.lifeforge.domain.model.StrategyComparator
import com.lifeforge.presentation.common.formatAnnualRate
import com.lifeforge.presentation.common.formatHorizon
import org.junit.Test
import java.time.Instant

/** Textos da comparação de estratégias e da linha de estratégia do histórico. */
class CompareTextsTest {

    private fun result(id: Long, probability: Double, contribution: Double) = SimulationResult(
        id = id, goalId = 1, numSimulations = 10_000, seed = 1, targetAmount = 1_000_000.0,
        successProbability = probability, mean = 1.0, median = 1.0, standardDeviation = 1.0,
        percentiles = mapOf("P10" to 1.0, "P90" to 2.0), worstCase = 0.5, bestCase = 3.0,
        meanReal = 1.0, histogram = emptyList(), executionTimeMs = 1,
        createdAt = Instant.parse("2026-10-0${id}T00:00:00Z"),
        inputs = SimulationInputs(50_000.0, contribution, 0.08, 0.15, 240, 1_000_000.0),
    )

    @Test
    fun `veredito aponta a estrategia vencedora e a diferenca de aporte`() {
        val text = verdictText(StrategyComparator.compare(result(1, 0.657, 2_000.0), result(2, 0.781, 2_500.0)))

        assertThat(text).startsWith("A estratégia B tem 12,4 p.p. a mais de chance")
        assertThat(text).contains("A estratégia A exige")
    }

    @Test
    fun `veredito de empate tecnico explica a margem de erro`() {
        val text = verdictText(StrategyComparator.compare(result(1, 0.652, 2_500.0), result(2, 0.657, 2_000.0)))

        assertThat(text).startsWith("Empate técnico")
        assertThat(text).contains("margem de erro")
        assertThat(text).contains("a estratégia B é mais eficiente")
    }

    @Test
    fun `linha de estrategia resume as premissas`() {
        val line = strategyLine(SimulationInputs(50_000.0, 2_000.0, 0.08, 0.15, 240, 1_000_000.0))

        assertThat(line).contains("Aporte")
        assertThat(line).contains("8% a.a.")
        assertThat(line).contains("vol. 15%")
        assertThat(line).endsWith("20 anos")
    }

    @Test
    fun `horizonte e taxas por extenso`() {
        assertThat(formatHorizon(30)).isEqualTo("2 anos e 6 meses")
        assertThat(formatHorizon(1)).isEqualTo("1 mês")
        assertThat(formatAnnualRate(0.105)).isEqualTo("10,5%")
        assertThat(formatAnnualRate(0.08)).isEqualTo("8%")
    }
}
