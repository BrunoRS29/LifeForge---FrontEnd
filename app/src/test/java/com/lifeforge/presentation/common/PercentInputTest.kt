package com.lifeforge.presentation.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

/** Campos de taxa: o usuário digita a porcentagem; o motor recebe a fração. */
class PercentInputTest {

    @Test
    fun `fracao vira porcentagem sem zeros nem ruido de ponto flutuante`() {
        assertThat(fractionToPercentInput(0.08)).isEqualTo("8")
        assertThat(fractionToPercentInput(0.1065)).isEqualTo("10,65")
        assertThat(fractionToPercentInput(0.005)).isEqualTo("0,5")
        assertThat(fractionToPercentInput(0.07)).isEqualTo("7")
        assertThat(fractionToPercentInput(1.0)).isEqualTo("100")
        assertThat(fractionToPercentInput(BigDecimal("0.1500"))).isEqualTo("15")
    }

    @Test
    fun `porcentagem digitada vira fracao`() {
        assertThat(parsePercentInput("8")).isEqualToIgnoringScale(BigDecimal("0.08"))
        assertThat(parsePercentInput("8,25")).isEqualToIgnoringScale(BigDecimal("0.0825"))
        assertThat(parsePercentInputAsDouble("0,5")).isWithin(1e-12).of(0.005)
    }

    @Test
    fun `entrada invalida ou vazia nao vira numero`() {
        assertThat(parsePercentInput("")).isNull()
        assertThat(parsePercentInput("abc")).isNull()
    }

    @Test
    fun `ida e volta preserva o valor`() {
        listOf(0.08, 0.15, 0.0425, 0.9).forEach { fraction ->
            assertThat(parsePercentInputAsDouble(fractionToPercentInput(fraction))).isWithin(1e-12).of(fraction)
        }
    }
}
