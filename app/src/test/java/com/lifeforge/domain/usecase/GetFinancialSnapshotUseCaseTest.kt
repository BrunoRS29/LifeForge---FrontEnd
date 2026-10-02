package com.lifeforge.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.Expense
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.Income
import com.lifeforge.domain.model.IncomeType
import com.lifeforge.domain.repository.AssetRepository
import com.lifeforge.domain.repository.ExpenseRepository
import com.lifeforge.domain.repository.IncomeRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Testes da composicao do snapshot do Dashboard, com foco na REGRA DE RENDA:
 * a receita mensal usa o SALARIO CONFIGURADO no perfil como fonte de verdade
 * e so cai no salario inferido dos lancamentos quando o perfil nao informa um.
 *
 * Isso reproduz o bug relatado: receita aparecia ~2364 (inferida dos extratos)
 * mesmo com salario 18480 configurado no perfil.
 *
 * Tambem cobre a REGRA DE DESPESA: lancamentos recorrentes repetidos todo mes
 * contam uma vez por serie (antes, 18 meses de aluguel viravam 18 alugueis).
 */
class GetFinancialSnapshotUseCaseTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-02T15:00:00Z"), ZoneOffset.UTC)

    private fun salaryIncome(amount: String, recurring: Boolean = true) = Income(
        id = 1L,
        userId = 1L,
        source = "Salario",
        amount = BigDecimal(amount),
        incomeType = IncomeType.SALARY,
        recurring = recurring,
        receivedAt = Instant.parse("2026-05-05T00:00:00Z"),
        createdAt = Instant.parse("2026-05-05T00:00:00Z"),
    )

    private fun monthlyIncome(source: String, amount: String, month: YearMonth) = Income(
        id = 0L, userId = 1L, source = source, amount = BigDecimal(amount), incomeType = IncomeType.SALARY,
        recurring = true, receivedAt = month.atDay(5).atStartOfDay().toInstant(ZoneOffset.UTC), createdAt = Instant.EPOCH,
    )

    private fun expense(description: String, amount: String, category: ExpenseCategory, month: YearMonth, recurring: Boolean) =
        Expense(
            id = 0L, userId = 1L, description = description, amount = BigDecimal(amount), category = category,
            recurring = recurring, spentAt = month.atDay(10).atStartOfDay().toInstant(ZoneOffset.UTC),
            createdAt = Instant.EPOCH,
        )

    /** 18 meses ate setembro/2026, como na conta de demonstracao. */
    private val eighteenMonths = (17 downTo 0).map { YearMonth.of(2026, 9).minusMonths(it.toLong()) }

    private fun useCaseWith(
        incomes: List<Income>,
        expenses: List<Expense> = emptyList(),
    ): GetFinancialSnapshotUseCase {
        val incomeRepo = mockk<IncomeRepository> { every { observeAll() } returns flowOf(incomes) }
        val expenseRepo = mockk<ExpenseRepository> { every { observeAll() } returns flowOf(expenses) }
        val assetRepo = mockk<AssetRepository> { every { observeAll() } returns flowOf(emptyList()) }
        return GetFinancialSnapshotUseCase(incomeRepo, expenseRepo, assetRepo, clock)
    }

    @Test
    fun `salario do perfil tem prioridade sobre o inferido dos lancamentos`() = runTest {
        // Lancamentos diriam 2000, mas o perfil configurou 18480.
        val useCase = useCaseWith(listOf(salaryIncome("2000")))

        val snapshot = useCase(configuredSalary = flowOf(BigDecimal("18480"))).first()

        assertThat(snapshot.monthlySalary).isEqualToIgnoringScale("18480")
        assertThat(snapshot.monthlyIncome).isEqualToIgnoringScale("18480")
    }

    @Test
    fun `sem salario no perfil usa o inferido dos lancamentos`() = runTest {
        val useCase = useCaseWith(listOf(salaryIncome("2000")))

        val snapshot = useCase(configuredSalary = flowOf(null)).first()

        assertThat(snapshot.monthlySalary).isEqualToIgnoringScale("2000")
        assertThat(snapshot.monthlyIncome).isEqualToIgnoringScale("2000")
    }

    @Test
    fun `default invoke sem perfil cai no inferido`() = runTest {
        val useCase = useCaseWith(listOf(salaryIncome("2000")))

        // invoke() sem argumento -> flowOf(null) -> inferido.
        val snapshot = useCase().first()

        assertThat(snapshot.monthlyIncome).isEqualToIgnoringScale("2000")
    }

    @Test
    fun `despesas recorrentes de 18 meses contam uma vez por serie`() = runTest {
        // Regressao: o painel mostrava R$ 72 mil de despesa mensal e poupanca negativa.
        val expenses = eighteenMonths.flatMap { m ->
            listOf(
                expense("Aluguel", "2800", ExpenseCategory.HOUSING, m, recurring = true),
                expense("Escola infantil", "950", ExpenseCategory.EDUCATION, m, recurring = true),
                expense("Supermercado", "1400", ExpenseCategory.FOOD, m, recurring = false),
            )
        }
        val incomes = eighteenMonths.map { monthlyIncome("Salario", "9500", it) }

        val snapshot = useCaseWith(incomes, expenses)().first()

        // 2800 + 950 (series) + 1400 (media dos pontuais)
        assertThat(snapshot.monthlyExpenses).isEqualToIgnoringScale("5150")
        // salario inferido: uma vez, nao 18
        assertThat(snapshot.monthlySalary).isEqualToIgnoringScale("9500")
        // (9500 - 5150) / 9500 = 45,79%
        assertThat(snapshot.savingsRate).isEqualToIgnoringScale("45.79")
    }

    @Test
    fun `reajuste do salario usa o valor mais recente da serie`() = runTest {
        val incomes = eighteenMonths.mapIndexed { i, m ->
            monthlyIncome("Salario", if (i < 12) "9500" else "9975", m)
        }

        val snapshot = useCaseWith(incomes)().first()

        assertThat(snapshot.monthlySalary).isEqualToIgnoringScale("9975")
    }
}
