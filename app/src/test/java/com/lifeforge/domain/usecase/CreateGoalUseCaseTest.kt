package com.lifeforge.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.domain.repository.GoalRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Cobre todas as ramificações de validação do [CreateGoalUseCase] e
 * confirma que, em caso de falha, o repositório nunca é chamado —
 * comportamento essencial para "fail fast" antes da rede.
 *
 * Usa um Clock fixo (FIXED_NOW) para tornar a validação de
 * `targetDate no futuro` determinística.
 */
class CreateGoalUseCaseTest {

    private val repository: GoalRepository = mockk()

    // 9 de maio de 2026 às 10:00 UTC — referência fixa para os testes.
    private val fixedNow: Instant = Instant.parse("2026-05-09T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedNow, ZoneOffset.UTC)

    private val useCase = CreateGoalUseCase(repository, clock)

    private val futureDate: Instant = fixedNow.plusSeconds(60 * 60 * 24 * 30)  // +30 dias
    private val pastDate: Instant = fixedNow.minusSeconds(60 * 60 * 24)         // -1 dia

    // ------------------------------------------------------------------------
    // Caminho feliz
    // ------------------------------------------------------------------------

    @Test
    fun `caminho feliz delega para o repositorio`() = runTest {
        val expected = sampleGoal()
        coEvery { repository.create(any(), any(), any(), any(), any()) } returns
            DataResult.Success(expected)

        val result = useCase(
            name = "Aposentadoria",
            category = GoalCategory.RETIREMENT,
            targetAmount = BigDecimal("1500000.00"),
            targetDate = futureDate,
            priority = 1,
        )

        assertThat(result).isEqualTo(DataResult.Success(expected))
        coVerify(exactly = 1) {
            repository.create("Aposentadoria", GoalCategory.RETIREMENT, BigDecimal("1500000.00"), futureDate, 1)
        }
    }

    @Test
    fun `nome com espacos extras e trimado antes de enviar`() = runTest {
        coEvery { repository.create(any(), any(), any(), any(), any()) } returns
            DataResult.Success(sampleGoal())

        useCase(
            name = "   Casa nova   ",
            category = GoalCategory.REAL_ESTATE,
            targetAmount = BigDecimal("500000"),
            targetDate = futureDate,
            priority = 2,
        )

        coVerify { repository.create("Casa nova", any(), any(), any(), any()) }
    }

    // ------------------------------------------------------------------------
    // Validações — repositório nunca chamado
    // ------------------------------------------------------------------------

    @Test
    fun `nome em branco retorna Validation e nao chama repositorio`() = runTest {
        val result = useCase(
            name = "   ",
            category = GoalCategory.CUSTOM,
            targetAmount = BigDecimal("1000"),
            targetDate = futureDate,
            priority = 1,
        )

        val error = (result as DataResult.Failure).error
        assertThat(error).isInstanceOf(AppError.Validation::class.java)
        assertThat((error as AppError.Validation).field).isEqualTo("name")
        coVerify(exactly = 0) { repository.create(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `targetAmount zero retorna Validation`() = runTest {
        val result = useCase(
            name = "Test",
            category = GoalCategory.CUSTOM,
            targetAmount = BigDecimal.ZERO,
            targetDate = futureDate,
            priority = 1,
        )

        val error = (result as DataResult.Failure).error as AppError.Validation
        assertThat(error.field).isEqualTo("targetAmount")
    }

    @Test
    fun `targetAmount negativo retorna Validation`() = runTest {
        val result = useCase(
            name = "Test",
            category = GoalCategory.CUSTOM,
            targetAmount = BigDecimal("-100"),
            targetDate = futureDate,
            priority = 1,
        )

        val error = (result as DataResult.Failure).error as AppError.Validation
        assertThat(error.field).isEqualTo("targetAmount")
    }

    @Test
    fun `targetDate no passado retorna Validation`() = runTest {
        val result = useCase(
            name = "Test",
            category = GoalCategory.CUSTOM,
            targetAmount = BigDecimal("1000"),
            targetDate = pastDate,
            priority = 1,
        )

        val error = (result as DataResult.Failure).error as AppError.Validation
        assertThat(error.field).isEqualTo("targetDate")
    }

    @Test
    fun `targetDate igual a now retorna Validation`() = runTest {
        // Borda: targetDate exatamente == now não conta como "futuro".
        val result = useCase(
            name = "Test",
            category = GoalCategory.CUSTOM,
            targetAmount = BigDecimal("1000"),
            targetDate = fixedNow,
            priority = 1,
        )

        val error = (result as DataResult.Failure).error as AppError.Validation
        assertThat(error.field).isEqualTo("targetDate")
    }

    @Test
    fun `priority fora de 1 a 10 retorna Validation`() = runTest {
        val resultZero = useCase("X", GoalCategory.CUSTOM, BigDecimal("100"), futureDate, 0)
        val resultEleven = useCase("X", GoalCategory.CUSTOM, BigDecimal("100"), futureDate, 11)

        assertThat(((resultZero as DataResult.Failure).error as AppError.Validation).field)
            .isEqualTo("priority")
        assertThat(((resultEleven as DataResult.Failure).error as AppError.Validation).field)
            .isEqualTo("priority")
    }

    private fun sampleGoal() = Goal(
        id = 1L,
        userId = 1L,
        name = "Aposentadoria",
        category = GoalCategory.RETIREMENT,
        targetAmount = BigDecimal("1500000.00"),
        targetDate = futureDate,
        priority = 1,
        createdAt = fixedNow,
    )
}
