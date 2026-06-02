package com.lifeforge.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.CalibratedSimulation
import com.lifeforge.domain.model.CalibratedSimulationParameters
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.ExpensePrediction
import com.lifeforge.domain.model.IncomePrediction
import com.lifeforge.domain.model.PredictionMetrics
import com.lifeforge.domain.model.PredictionSummary
import com.lifeforge.domain.repository.PredictionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

/**
 * Testes dos UseCases da Sprint 5.
 *
 * Foco: validacoes em camada de aplicacao - garantir que requests
 * obviamente invalidas falhem ANTES de bater na rede (UX > 3s perdidos).
 *
 * Nota sobre [DataResult.Failure]: nao eh generico no projeto. A assinatura
 * eh `Failure(error: AppError) : DataResult<Nothing>` - portanto NAO usar
 * `DataResult.Failure<SeuTipo>(...)` em lugar nenhum.
 */
class PredictIncomeUseCaseTest {

    // ------------------------------------------------------------------------
    // Fake repo capturador
    // ------------------------------------------------------------------------

    private class FakeRepository(
        private val incomeResult: DataResult<IncomePrediction> =
            DataResult.Success(stubIncomePrediction()),
        private val expenseResult: DataResult<ExpensePrediction> =
            DataResult.Success(stubExpensePrediction()),
        private val calibratedResult: DataResult<CalibratedSimulation> =
            DataResult.Failure(AppError.Unknown(IllegalStateException("nao deve chamar"))),
    ) : PredictionRepository {
        var lastIncomeHorizon: Int? = null
        var lastExpenseHorizon: Int? = null
        var lastCalibratedParams: CalibratedSimulationParameters? = null
        var callCount = 0

        override suspend fun predictIncome(horizonMonths: Int): DataResult<IncomePrediction> {
            callCount++
            lastIncomeHorizon = horizonMonths
            return incomeResult
        }

        override suspend fun predictExpenses(horizonMonths: Int): DataResult<ExpensePrediction> {
            callCount++
            lastExpenseHorizon = horizonMonths
            return expenseResult
        }

        override suspend fun listRecent(limit: Int): DataResult<List<PredictionSummary>> =
            DataResult.Success(emptyList())

        override suspend fun runCalibrated(
            parameters: CalibratedSimulationParameters,
        ): DataResult<CalibratedSimulation> {
            callCount++
            lastCalibratedParams = parameters
            return calibratedResult
        }
    }

    // ------------------------------------------------------------------------
    // PredictIncomeUseCase
    // ------------------------------------------------------------------------

    @Test
    fun `predictIncome - happy path com 12 meses bate na rede e devolve sucesso`() = runTest {
        val repo = FakeRepository()
        val useCase = PredictIncomeUseCase(repo)

        val result = useCase(horizonMonths = 12)

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        assertThat(repo.lastIncomeHorizon).isEqualTo(12)
        assertThat(repo.callCount).isEqualTo(1)
    }

    @Test
    fun `predictIncome - horizonte zero falha sem bater na rede`() = runTest {
        val repo = FakeRepository()
        val useCase = PredictIncomeUseCase(repo)

        val result = useCase(horizonMonths = 0)

        val error = (result as DataResult.Failure).error
        assertThat(error).isInstanceOf(AppError.Validation::class.java)
        assertThat((error as AppError.Validation).field).isEqualTo("horizonMonths")
        assertThat(repo.callCount).isEqualTo(0)
    }

    @Test
    fun `predictIncome - horizonte maior que 60 falha sem bater na rede`() = runTest {
        val repo = FakeRepository()
        val useCase = PredictIncomeUseCase(repo)

        val result = useCase(horizonMonths = 61)

        assertThat(result).isInstanceOf(DataResult.Failure::class.java)
        assertThat(repo.callCount).isEqualTo(0)
    }

    @Test
    fun `predictIncome - propaga falha do repositorio`() = runTest {
        val repo = FakeRepository(
            incomeResult = DataResult.Failure(AppError.Network("offline")),
        )
        val useCase = PredictIncomeUseCase(repo)

        val result = useCase(horizonMonths = 6)

        val error = (result as DataResult.Failure).error
        assertThat(error).isInstanceOf(AppError.Network::class.java)
    }

    // ------------------------------------------------------------------------
    // PredictExpensesUseCase
    // ------------------------------------------------------------------------

    @Test
    fun `predictExpenses - default 1 mes funciona`() = runTest {
        val repo = FakeRepository()
        val useCase = PredictExpensesUseCase(repo)

        val result = useCase()

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        assertThat(repo.lastExpenseHorizon).isEqualTo(1)
    }

    @Test
    fun `predictExpenses - horizonte maior que 12 falha`() = runTest {
        val repo = FakeRepository()
        val useCase = PredictExpensesUseCase(repo)

        val result = useCase(horizonMonths = 13)

        assertThat(result).isInstanceOf(DataResult.Failure::class.java)
        assertThat(repo.callCount).isEqualTo(0)
    }

    // ------------------------------------------------------------------------
    // RunCalibratedSimulationUseCase
    // ------------------------------------------------------------------------

    @Test
    fun `runCalibrated - numSimulations menor que 10k falha`() = runTest {
        val repo = FakeRepository()
        val useCase = RunCalibratedSimulationUseCase(repo)

        val result = useCase(
            CalibratedSimulationParameters(
                goalId = 1L,
                initialCapital = 10_000.0,
                expectedReturnAnnual = 0.08,
                volatilityAnnual = 0.15,
                horizonMonths = 120,
                targetAmount = 100_000.0,
                numSimulations = 100,  // muito baixo
            )
        )

        val error = (result as DataResult.Failure).error
        assertThat(error).isInstanceOf(AppError.Validation::class.java)
        assertThat((error as AppError.Validation).field).isEqualTo("numSimulations")
        assertThat(repo.callCount).isEqualTo(0)
    }

    @Test
    fun `runCalibrated - probabilidade desemprego fora de 0-1 falha`() = runTest {
        val repo = FakeRepository()
        val useCase = RunCalibratedSimulationUseCase(repo)

        val result = useCase(
            CalibratedSimulationParameters(
                goalId = 1L,
                initialCapital = 10_000.0,
                expectedReturnAnnual = 0.08,
                volatilityAnnual = 0.15,
                horizonMonths = 120,
                targetAmount = 100_000.0,
                unemploymentProbAnnual = 1.5,  // > 1
            )
        )

        val error = (result as DataResult.Failure).error
        assertThat(error).isInstanceOf(AppError.Validation::class.java)
        assertThat((error as AppError.Validation).field).isEqualTo("unemploymentProbAnnual")
    }

    @Test
    fun `runCalibrated - incomeHorizon fora de 1-60 falha`() = runTest {
        val repo = FakeRepository()
        val useCase = RunCalibratedSimulationUseCase(repo)

        val result = useCase(validParams().copy(incomeHorizonMonths = 0))

        assertThat(result).isInstanceOf(DataResult.Failure::class.java)
        assertThat(repo.callCount).isEqualTo(0)
    }

    @Test
    fun `runCalibrated - parametros validos chegam ao repo`() = runTest {
        val repo = FakeRepository(
            calibratedResult = DataResult.Failure(AppError.Network("offline-only-as-marker")),
        )
        val useCase = RunCalibratedSimulationUseCase(repo)

        useCase(validParams())

        assertThat(repo.callCount).isEqualTo(1)
        assertThat(repo.lastCalibratedParams).isNotNull()
        assertThat(repo.lastCalibratedParams!!.targetAmount).isEqualTo(100_000.0)
    }

    // ------------------------------------------------------------------------
    // ListRecentPredictionsUseCase
    // ------------------------------------------------------------------------

    @Test
    fun `listRecent - limit acima do maximo eh coagido para 200`() = runTest {
        var observedLimit: Int? = null
        val repo = object : PredictionRepository {
            override suspend fun predictIncome(horizonMonths: Int): DataResult<IncomePrediction> =
                DataResult.Success(stubIncomePrediction())
            override suspend fun predictExpenses(horizonMonths: Int): DataResult<ExpensePrediction> =
                DataResult.Success(stubExpensePrediction())
            override suspend fun listRecent(limit: Int): DataResult<List<PredictionSummary>> {
                observedLimit = limit
                return DataResult.Success(emptyList())
            }
            override suspend fun runCalibrated(
                parameters: CalibratedSimulationParameters,
            ): DataResult<CalibratedSimulation> =
                DataResult.Failure(AppError.Unknown(Exception()))
        }

        ListRecentPredictionsUseCase(repo).invoke(limit = 10_000)

        assertThat(observedLimit).isEqualTo(200)
    }

    @Test
    fun `listRecent - limit zero eh coagido para 1`() = runTest {
        var observedLimit: Int? = null
        val repo = object : PredictionRepository {
            override suspend fun predictIncome(horizonMonths: Int): DataResult<IncomePrediction> =
                DataResult.Success(stubIncomePrediction())
            override suspend fun predictExpenses(horizonMonths: Int): DataResult<ExpensePrediction> =
                DataResult.Success(stubExpensePrediction())
            override suspend fun listRecent(limit: Int): DataResult<List<PredictionSummary>> {
                observedLimit = limit
                return DataResult.Success(emptyList())
            }
            override suspend fun runCalibrated(
                parameters: CalibratedSimulationParameters,
            ): DataResult<CalibratedSimulation> =
                DataResult.Failure(AppError.Unknown(Exception()))
        }

        ListRecentPredictionsUseCase(repo).invoke(limit = 0)

        assertThat(observedLimit).isEqualTo(1)
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private fun validParams() = CalibratedSimulationParameters(
        goalId = 1L,
        initialCapital = 10_000.0,
        expectedReturnAnnual = 0.08,
        volatilityAnnual = 0.15,
        horizonMonths = 120,
        targetAmount = 100_000.0,
        numSimulations = 10_000,
        incomeHorizonMonths = 12,
    )
}

private fun stubIncomePrediction() = IncomePrediction(
    predictionId = 1L,
    modelName = "INCOME_REGRESSION",
    horizonMonths = 12,
    projection = emptyList(),
    expectedMonthlyIncome = 5000.0,
    annualGrowthRate = 0.10,
    residualVolatilityMonthly = 100.0,
    metrics = PredictionMetrics(50.0, 70.0, 0.9),
    createdAt = Instant.now(),
)

private fun stubExpensePrediction() = ExpensePrediction(
    predictionId = 2L,
    modelName = "EXPENSE_RANDOM_FOREST",
    horizonMonths = 1,
    byCategory = emptyList(),
    expectedMonthlyExpense = 3000.0,
    metrics = PredictionMetrics(100.0, 150.0, 0.6),
    createdAt = Instant.now(),
)
