package com.lifeforge.presentation.screen.optimization

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Nota de erro amostral quando a verificação fica um pouco abaixo do alvo. */
class OptimizationTextsTest {

    @Test
    fun `abaixo do alvo dentro do erro amostral explica a diferenca`() {
        // Alvo 90%, verificado 89,9% com 10.000 cenários: 2 erros-padrão = 0,6 p.p.
        assertThat(samplingNote(achieved = 0.899, target = 0.90))
            .isEqualTo(
                "Diferença de 0,1 p.p. em relação ao alvo: dentro do erro amostral do Monte Carlo " +
                    "(±0,6 p.p. com 10.000 cenários).",
            )
    }

    @Test
    fun `no alvo ou acima nao ha nota`() {
        assertThat(samplingNote(achieved = 0.90, target = 0.90)).isNull()
        assertThat(samplingNote(achieved = 0.93, target = 0.90)).isNull()
    }

    @Test
    fun `diferenca maior que o erro amostral nao e mascarada`() {
        assertThat(samplingNote(achieved = 0.85, target = 0.90)).isNull()
    }
}
