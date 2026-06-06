package com.lifeforge.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.pow

/**
 * Testa a projeção determinística de patrimônio (fórmula 6.1 do TCC).
 * Garante que o gráfico do Dashboard usa a fórmula correta de juros
 * compostos com aportes.
 */
class WealthProjectionTest {

    @Test
    fun `t zero retorna o patrimonio inicial`() {
        val series = WealthProjection.project(
            initial = 10_000.0,
            monthlyContribution = 500.0,
            annualReturnRate = 0.08,
            months = 12,
        )
        assertThat(series.first()).isWithin(1e-6).of(10_000.0)
    }

    @Test
    fun `retorna months mais 1 pontos`() {
        val series = WealthProjection.project(0.0, 100.0, 0.08, 60)
        assertThat(series).hasSize(61)
    }

    @Test
    fun `sem rendimento e apenas soma dos aportes`() {
        // r = 0: P(t) = P0 + A*t
        val series = WealthProjection.project(
            initial = 1_000.0,
            monthlyContribution = 200.0,
            annualReturnRate = 0.0,
            months = 10,
        )
        assertThat(series[10]).isWithin(1e-6).of(1_000.0 + 200.0 * 10)
    }

    @Test
    fun `com rendimento bate a formula de juros compostos com aporte`() {
        val initial = 10_000.0
        val contribution = 1_000.0
        val annual = 0.12
        val months = 24

        val series = WealthProjection.project(initial, contribution, annual, months)

        val r = (1.0 + annual).pow(1.0 / 12.0) - 1.0
        val growth = (1.0 + r).pow(months.toDouble())
        val expected = initial * growth + contribution * (growth - 1.0) / r

        assertThat(series[months]).isWithin(0.01).of(expected)
        // Com rendimento positivo, o patrimônio final supera a soma dos aportes.
        assertThat(series[months]).isGreaterThan(initial + contribution * months)
    }
}
