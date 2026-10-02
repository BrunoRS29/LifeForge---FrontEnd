package com.lifeforge.presentation.screen.dashboard

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

/** Textos do painel: saudação na barra superior e sobra do mês com sinal. */
class DashboardTextsTest {

    @Test
    fun `saudacao usa so o primeiro nome`() {
        assertThat(greeting("Gabriel Souza")).isEqualTo("Olá, Gabriel!")
        assertThat(greeting("  Ana  ")).isEqualTo("Olá, Ana!")
    }

    @Test
    fun `sem nome a saudacao e neutra`() {
        assertThat(greeting(null)).isEqualTo("Olá!")
        assertThat(greeting("   ")).isEqualTo("Olá!")
    }

    @Test
    fun `sobra do mes leva o sinal a frente, como no extrato`() {
        assertThat(signedBrl(BigDecimal("3373.56"))).isEqualTo("+R$ 3.373,56")
        assertThat(signedBrl(BigDecimal("-120"))).isEqualTo("−R$ 120,00")
    }
}
