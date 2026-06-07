package com.lifeforge.domain.usecase

import com.google.common.truth.Truth.assertThat
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
import java.time.Instant

/**
 * Testes da composicao do snapshot do Dashboard, com foco na REGRA DE RENDA:
 * a receita mensal usa o SALARIO CONFIGURADO no perfil como fonte de verdade
 * e so cai no salario inferido dos lancamentos quando o perfil nao informa um.
 *
 * Isso reproduz o bug relatado: receita aparecia ~2364 (inferida dos extratos)
 * mesmo com salario 18480 configurado no perfil.
 */
class GetFinancialSnapshotUseCaseTest {

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

    private fun useCaseWith(incomes: List<Income>): GetFinancialSnapshotUseCase {
        val incomeRepo = mockk<IncomeRepository> { every { observeAll() } returns flowOf(incomes) }
        val expenseRepo = mockk<ExpenseRepository> { every { observeAll() } returns flowOf(emptyList()) }
        val assetRepo = mockk<AssetRepository> { every { observeAll() } returns flowOf(emptyList()) }
        return GetFinancialSnapshotUseCase(incomeRepo, expenseRepo, assetRepo)
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
}
