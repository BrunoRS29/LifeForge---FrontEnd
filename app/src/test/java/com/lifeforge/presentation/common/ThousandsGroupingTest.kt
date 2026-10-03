package com.lifeforge.presentation.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Separador de milhar exibido nos campos de R$ enquanto o usuário digita. */
class ThousandsGroupingTest {

    @Test
    fun `agrupa a parte inteira e preserva os decimais`() {
        assertThat(groupThousands("2500000,00")).isEqualTo("2.500.000,00")
        assertThat(groupThousands("1000")).isEqualTo("1.000")
        assertThat(groupThousands("999")).isEqualTo("999")
        assertThat(groupThousands("12345,6")).isEqualTo("12.345,6")
        assertThat(groupThousands(",5")).isEqualTo(",5")
        assertThat(groupThousands("")).isEqualTo("")
    }

    @Test
    fun `texto com ponto fica como o usuario digitou`() {
        assertThat(groupThousands("1.500")).isEqualTo("1.500")
        assertThat(groupThousands("1500.50")).isEqualTo("1500.50")
    }

    @Test
    fun `cursor acompanha os pontos inseridos`() {
        val grouping = ThousandsGrouping.of("2500000,00") // exibido: 2.500.000,00
        assertThat(grouping.toShown(0)).isEqualTo(0)
        assertThat(grouping.toShown(1)).isEqualTo(2) // depois de "2."
        assertThat(grouping.toShown(7)).isEqualTo(9) // antes da vírgula
        assertThat(grouping.toShown(10)).isEqualTo(12) // fim
        assertThat(grouping.toOriginal(2)).isEqualTo(1)
        assertThat(grouping.toOriginal(1)).isEqualTo(1) // logo depois do "2"
        assertThat(grouping.toOriginal(12)).isEqualTo(10)
    }

    @Test
    fun `ida e volta do cursor e consistente em todas as posicoes`() {
        val raw = "123456789,12"
        val grouping = ThousandsGrouping.of(raw)
        for (offset in 0..raw.length) {
            assertThat(grouping.toOriginal(grouping.toShown(offset))).isEqualTo(offset)
        }
    }
}
