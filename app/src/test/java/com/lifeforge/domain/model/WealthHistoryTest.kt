package com.lifeforge.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * Reconstrução do patrimônio realizado pelo fluxo de caixa (gráfico realizado
 * × projetado do painel): ancorada no valor atual, voltando mês a mês.
 */
class WealthHistoryTest {

    private val now = Instant.parse("2026-10-15T12:00:00Z")

    private fun income(amount: String, at: String) = Income(
        id = 1, userId = 1, source = "Salário", amount = BigDecimal(amount),
        incomeType = IncomeType.SALARY, recurring = true, receivedAt = Instant.parse(at), createdAt = Instant.EPOCH,
    )

    private fun expense(amount: String, at: String) = Expense(
        id = 1, userId = 1, description = "Aluguel", amount = BigDecimal(amount),
        category = ExpenseCategory.HOUSING, recurring = true, spentAt = Instant.parse(at), createdAt = Instant.EPOCH,
    )

    @Test
    fun `volta mes a mes descontando o saldo de cada mes`() {
        val incomes = listOf(
            income("5000", "2026-08-05T00:00:00Z"),
            income("5000", "2026-09-05T00:00:00Z"),
            income("5000", "2026-10-05T00:00:00Z"),
        )
        val expenses = listOf(
            expense("3000", "2026-08-10T00:00:00Z"),
            expense("3000", "2026-09-10T00:00:00Z"),
            expense("4000", "2026-10-10T00:00:00Z"),
        )

        val history = WealthHistory.reconstruct(incomes, expenses, BigDecimal("20000"), now, months = 3)

        // Fim de out = 20.000 (atual); fim de set = 20.000 − 1.000 (saldo de out);
        // fim de ago = 19.000 − 2.000 (saldo de set); fim de jul = 17.000 − 2.000 (saldo de ago).
        assertThat(history).containsExactly(15_000.0, 17_000.0, 19_000.0, 20_000.0).inOrder()
    }

    @Test
    fun `lancamentos futuros nao entram e o ultimo ponto e sempre o patrimonio atual`() {
        val incomes = listOf(
            income("1000", "2026-10-01T00:00:00Z"),
            income("99999", "2026-12-01T00:00:00Z"), // parcela agendada (futuro)
        )

        val history = WealthHistory.reconstruct(incomes, emptyList(), BigDecimal("5000"), now, months = 2)

        assertThat(history).containsExactly(4_000.0, 4_000.0, 5_000.0).inOrder()
    }

    @Test
    fun `sem lancamentos no periodo nao ha o que reconstruir`() {
        val old = listOf(income("1000", "2020-01-01T00:00:00Z"))

        assertThat(WealthHistory.reconstruct(old, emptyList(), BigDecimal("5000"), now, months = 6)).isEmpty()
    }

    @Test
    fun `patrimonio reconstruido nunca fica negativo`() {
        // Poupança grande sem ativos cadastrados (o dinheiro foi para fora do app).
        val incomes = listOf(income("10000", "2026-10-01T00:00:00Z"))

        val history = WealthHistory.reconstruct(incomes, emptyList(), BigDecimal.ZERO, now, months = 1)

        assertThat(history).containsExactly(0.0, 0.0).inOrder()
    }
}
