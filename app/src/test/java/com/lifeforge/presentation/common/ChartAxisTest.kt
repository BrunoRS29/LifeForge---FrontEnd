package com.lifeforge.presentation.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Rótulos dos eixos dos gráficos: R$ compacto e espaçamento dos meses. */
class ChartAxisTest {

    @Test
    fun `eixo em reais usa mil, mi e bi com no maximo uma casa`() {
        assertThat(formatAxisBrl(0.0)).isEqualTo("R$ 0")
        assertThat(formatAxisBrl(750.0)).isEqualTo("R$ 750")
        assertThat(formatAxisBrl(1_500.0)).isEqualTo("R$ 1,5 mil")
        assertThat(formatAxisBrl(850_000.0)).isEqualTo("R$ 850 mil")
        assertThat(formatAxisBrl(1_300_000.0)).isEqualTo("R$ 1,3 mi")
        assertThat(formatAxisBrl(7_800_000.0)).isEqualTo("R$ 7,8 mi")
        assertThat(formatAxisBrl(2_000_000_000.0)).isEqualTo("R$ 2 bi")
        assertThat(formatAxisBrl(-250_000.0)).isEqualTo("-R$ 250 mil")
    }

    @Test
    fun `espacamento dos meses deixa de 4 a 7 rotulos em qualquer horizonte`() {
        (19..720).forEach { months ->
            val labels = months / monthAxisSpacing(months) + 1
            assertThat(labels).isIn(4..7)
        }
        (6..18).forEach { months -> assertThat(months / monthAxisSpacing(months) + 1).isIn(3..7) }
        assertThat(monthAxisSpacing(12)).isEqualTo(3)
        assertThat(monthAxisSpacing(114)).isEqualTo(24)
        assertThat(monthAxisSpacing(327)).isEqualTo(60)
    }
}
