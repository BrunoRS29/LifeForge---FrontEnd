package com.lifeforge.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Patrimônio REALIZADO dos últimos meses, para o gráfico "realizado ×
 * projetado" do painel (proposta, Seção 8.4; TCC, Seção 4.8).
 *
 * O app não guarda fotografias mensais do patrimônio, então ele é
 * reconstruído pelo fluxo de caixa já registrado, ancorado no valor atual
 * dos ativos e percorrendo os meses para trás:
 *
 *     R(0) = patrimônio atual
 *     R(k) = R(k−1) − saldo do mês (k−1)      (saldo = receitas − despesas)
 *
 * Premissa (declarada no gráfico): a variação do patrimônio no período vem do
 * saldo mensal — rendimentos e reavaliações de ativos não são separados. É a
 * mesma base do modelo de série temporal do backend (patrimônio acumulado pelo
 * fluxo de caixa). Lançamentos futuros (parcelas agendadas) não entram.
 */
object WealthHistory {

    /**
     * @param months quantos meses de histórico devolver (além do atual).
     * @return valores do mais antigo ao atual; `size = months + 1`, o último é
     *   [currentWealth]. Lista vazia quando não há lançamento no período (não
     *   há o que reconstruir).
     */
    fun reconstruct(
        incomes: List<Income>,
        expenses: List<Expense>,
        currentWealth: BigDecimal,
        now: Instant,
        months: Int = DEFAULT_MONTHS,
    ): List<Double> {
        require(months > 0) { "months deve ser > 0" }
        val currentMonth = YearMonth.from(now.atZone(ZoneOffset.UTC))
        val firstMonth = currentMonth.minusMonths(months.toLong())

        fun inWindow(at: Instant): Boolean {
            if (at.isAfter(now)) return false
            val month = YearMonth.from(at.atZone(ZoneOffset.UTC))
            return !month.isBefore(firstMonth)
        }

        val netByMonth = HashMap<YearMonth, Double>()
        incomes.filter { inWindow(it.receivedAt) }.forEach {
            val month = YearMonth.from(it.receivedAt.atZone(ZoneOffset.UTC))
            netByMonth[month] = (netByMonth[month] ?: 0.0) + it.amount.toDouble()
        }
        expenses.filter { inWindow(it.spentAt) }.forEach {
            val month = YearMonth.from(it.spentAt.atZone(ZoneOffset.UTC))
            netByMonth[month] = (netByMonth[month] ?: 0.0) - it.amount.toDouble()
        }
        if (netByMonth.isEmpty()) return emptyList()

        // Do mês atual para trás: o patrimônio ao FIM do mês m-1 é o do fim de m
        // menos o saldo de m.
        val values = DoubleArray(months + 1)
        var wealth = currentWealth.toDouble()
        values[months] = wealth
        for (k in 1..months) {
            val month = currentMonth.minusMonths((k - 1).toLong())
            wealth -= netByMonth[month] ?: 0.0
            values[months - k] = wealth
        }
        // Patrimônio não fica negativo: um valor abaixo de zero indica ativos não
        // cadastrados (a poupança foi para fora do app), então o piso é zero.
        return values.map { it.coerceAtLeast(0.0) }
    }

    const val DEFAULT_MONTHS = 12
}
