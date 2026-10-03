package com.lifeforge.presentation.screen.optimization

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.TerminationReason
import org.junit.Test

/** Textos do resultado da otimização: erro amostral, fim da busca e prazo. */
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

    @Test
    fun `convergencia normal nao precisa de explicacao`() {
        assertThat(terminationText(TerminationReason.CONVERGED)).isNull()
        assertThat(terminationText(TerminationReason.INFEASIBLE_UPPER_BOUND)).startsWith("Nem o teto da busca")
    }

    @Test
    fun `prazo em anos e meses ou menos de um mes`() {
        assertThat(formatPeriod(30.0)).isEqualTo("2 anos e 6 meses")
        assertThat(formatPeriod(12.4)).isEqualTo("1 ano")
        assertThat(formatPeriod(0.5)).isEqualTo("Menos de 1 mês")
    }
}
