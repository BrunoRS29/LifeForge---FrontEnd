package com.lifeforge.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale

/**
 * Testa a detecção de recorrências no histórico (usado pelo Dashboard para
 * identificar receitas/despesas recorrentes a partir dos extratos importados).
 */
class RecurrenceDetectorTest {

    private fun instant(year: Int, month: Int): Instant =
        Instant.parse(String.format(Locale.US, "%04d-%02d-10T12:00:00Z", year, month))

    private fun income(amount: String, desc: String, year: Int, month: Int) = Income(
        id = 0, userId = 1, source = desc, amount = BigDecimal(amount),
        incomeType = IncomeType.SALARY, recurring = false,
        receivedAt = instant(year, month), createdAt = Instant.EPOCH,
    )

    private fun expense(amount: String, desc: String, year: Int, month: Int) = Expense(
        id = 0, userId = 1, description = desc, amount = BigDecimal(amount),
        category = ExpenseCategory.OTHER, recurring = false,
        spentAt = instant(year, month), createdAt = Instant.EPOCH,
    )

    @Test
    fun `salario em 3 meses e detectado com valor mediano`() {
        val incomes = listOf(
            income("16400.00", "Transferencia Recebida - GABRIEL CONSULTORIA 57.423", 2026, 1),
            income("16800.00", "Transferencia Recebida - GABRIEL CONSULTORIA 57.423", 2026, 2),
            income("17000.00", "Transferencia Recebida - GABRIEL CONSULTORIA 57.423", 2026, 3),
        )
        val res = RecurrenceDetector.detectIncome(incomes)

        assertThat(res).hasSize(1)
        assertThat(res[0].months).isEqualTo(3)
        assertThat(res[0].monthlyAmount).isEqualTo(BigDecimal("16800.00")) // mediana
    }

    @Test
    fun `item em apenas 2 meses nao e considerado recorrente`() {
        val expenses = listOf(
            expense("63.98", "SEMAE PIRACICABA", 2026, 2),
            expense("63.98", "SEMAE PIRACICABA", 2026, 3),
        )
        assertThat(RecurrenceDetector.detectExpense(expenses)).isEmpty()
    }

    @Test
    fun `assinatura agrupa descricoes que diferem so na data`() {
        val expenses = listOf(
            expense("100.00", "PIX TRANSF FORNECEDOR 10/01", 2026, 1),
            expense("100.00", "PIX TRANSF FORNECEDOR 12/02", 2026, 2),
            expense("100.00", "PIX TRANSF FORNECEDOR 08/03", 2026, 3),
        )
        val res = RecurrenceDetector.detectExpense(expenses)

        assertThat(res).hasSize(1)
        assertThat(res[0].months).isEqualTo(3)
    }
}
