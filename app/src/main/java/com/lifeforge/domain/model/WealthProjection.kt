package com.lifeforge.domain.model

import kotlin.math.abs
import kotlin.math.pow

/**
 * Projeção determinística de patrimônio (juros compostos com aportes).
 *
 * Implementa a fórmula da Seção 6.1 da proposta do TCC:
 *
 *   P(t) = P0·(1+r)^t + A·[((1+r)^t − 1) / r]    (r ≠ 0)
 *   P(t) = P0 + A·t                               (r = 0)
 *
 * Onde P0 = patrimônio inicial, A = aporte mensal, r = taxa de retorno
 * mensal equivalente à anual informada, t = mês.
 *
 * É a base "projetada" do gráfico real × projetado do Dashboard. É
 * determinística (sem componente estocástico) — a versão probabilística é a
 * simulação de Monte Carlo da tela de Simulação.
 */
object WealthProjection {

    /**
     * Retorna o patrimônio projetado para cada mês de 0 a [months] (inclusive),
     * ou seja, [months] + 1 pontos. O índice 0 é o patrimônio atual ([initial]).
     */
    fun project(
        initial: Double,
        monthlyContribution: Double,
        annualReturnRate: Double,
        months: Int,
    ): List<Double> {
        require(months >= 0) { "months deve ser >= 0" }
        // Taxa mensal equivalente à anual: (1+r_a)^(1/12) − 1
        val r = (1.0 + annualReturnRate).pow(1.0 / 12.0) - 1.0

        return (0..months).map { t ->
            if (abs(r) < 1e-12) {
                initial + monthlyContribution * t
            } else {
                val growth = (1.0 + r).pow(t.toDouble())
                initial * growth + monthlyContribution * (growth - 1.0) / r
            }
        }
    }
}
