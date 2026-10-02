package com.lifeforge.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/** Horizonte de simulação/otimização em meses de calendário até a data-alvo. */
class GoalHorizonTest {

    private val now = Instant.parse("2026-10-02T15:00:00Z")

    @Test
    fun `27 anos e 2 meses mais 30 dias arredondam para 327 meses`() {
        // dias ÷ 30 dava 331: ~5 meses a mais que o calendário.
        assertThat(GoalHorizon.months(now, Instant.parse("2054-01-01T15:00:00Z"))).isEqualTo(327)
    }

    @Test
    fun `sobra de dias abaixo de meio mes nao conta`() {
        assertThat(GoalHorizon.months(now, Instant.parse("2027-10-10T15:00:00Z"))).isEqualTo(12)
        assertThat(GoalHorizon.months(now, Instant.parse("2027-10-20T15:00:00Z"))).isEqualTo(13)
    }

    @Test
    fun `data-alvo de hoje ou passada vira o minimo de 1 mes`() {
        assertThat(GoalHorizon.months(now, now)).isEqualTo(1)
        assertThat(GoalHorizon.months(now, Instant.parse("2020-01-01T00:00:00Z"))).isEqualTo(1)
    }

    @Test
    fun `usa o dia de Sao Paulo, nao o de Greenwich`() {
        // 01/06/2030 à meia-noite UTC ainda é 31/05 em São Paulo: 43 meses e 29 dias → 44.
        assertThat(GoalHorizon.months(now, Instant.parse("2030-06-01T00:00:00Z"))).isEqualTo(44)
    }
}
