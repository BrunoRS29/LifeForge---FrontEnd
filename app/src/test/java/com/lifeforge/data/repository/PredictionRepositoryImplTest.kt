package com.lifeforge.data.repository

// Import correto do converter Jake Wharton (mesmo que o NetworkModule do
// app usa). NAO usar `retrofit2.converter.kotlinx.serialization` - esse
// pacote so existe a partir do Retrofit 2.11+ que o projeto nao usa.
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.google.common.truth.Truth.assertThat
import com.lifeforge.data.api.PredictionApi
import com.lifeforge.domain.model.CalibratedSimulationParameters
import com.lifeforge.domain.model.DataResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * Testes do [PredictionRepositoryImpl] usando MockWebServer.
 *
 * Estrategia:
 *  - Sobe um MockWebServer (servidor HTTP fake em-processo)
 *  - Cria Retrofit/PredictionApi apontando para a URL local
 *  - Enqueue respostas e verifica que o repository:
 *    (a) chama os paths corretos com bodies corretos
 *    (b) desserializa corretamente
 *    (c) trata erros HTTP via safeApiCall
 *
 * Esses testes pegam coisas que testes com fake repo NAO pegariam:
 *  - Mismatches de @SerialName/snake_case vs camelCase
 *  - Paths errados no @POST/@GET
 *  - Bugs no parse de Instant ou enums
 */
class PredictionRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: PredictionRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
        val api = retrofit.create(PredictionApi::class.java)
        repo = PredictionRepositoryImpl(api, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ------------------------------------------------------------------------
    // predictIncome
    // ------------------------------------------------------------------------

    @Test
    fun `predictIncome - sucesso desserializa todos os campos`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "predictionId": 42,
                      "modelName": "INCOME_REGRESSION",
                      "horizonMonths": 6,
                      "projection": [
                        {"monthIndex": 1, "predictedAmount": 5100.0},
                        {"monthIndex": 2, "predictedAmount": 5200.0}
                      ],
                      "expectedMonthlyIncome": 5150.0,
                      "annualGrowthRate": 0.12,
                      "residualVolatilityMonthly": 80.5,
                      "mae": 50.1,
                      "rmse": 70.2,
                      "r2": 0.85,
                      "createdAt": "2026-05-20T10:00:00Z"
                    }
                    """.trimIndent()
                )
        )

        val result = repo.predictIncome(horizonMonths = 6)

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        val data = (result as DataResult.Success).data
        assertThat(data.predictionId).isEqualTo(42L)
        assertThat(data.expectedMonthlyIncome).isEqualTo(5150.0)
        assertThat(data.annualGrowthRate).isEqualTo(0.12)
        assertThat(data.projection).hasSize(2)
        assertThat(data.metrics.r2).isEqualTo(0.85)

        // Verifica o request enviado
        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/predictions/income")
        assertThat(recordedRequest.body.readUtf8()).contains("\"horizonMonths\":6")
    }

    @Test
    fun `predictIncome - 422 do backend vira falha`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"INSUFFICIENT_DATA","message":"minimo 6 meses"}""")
        )

        val result = repo.predictIncome(horizonMonths = 6)

        val error = (result as DataResult.Failure).error
        // O safeApiCall do projeto mapeia 422 conforme a logica em parseHttpError.
        // Qualquer que seja o subtipo, a mensagem deve estar preservada.
        assertThat(error.message).contains("6 meses")
    }

    // ------------------------------------------------------------------------
    // predictExpenses
    // ------------------------------------------------------------------------

    @Test
    fun `predictExpenses - mapeia by_category preservando ordem`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "predictionId": 7,
                      "modelName": "EXPENSE_RANDOM_FOREST",
                      "horizonMonths": 1,
                      "byCategory": [
                        {"category": "HOUSING", "predictedAmount": 2500.0},
                        {"category": "FOOD", "predictedAmount": 1500.0}
                      ],
                      "expectedMonthlyExpense": 4000.0,
                      "mae": 100.0,
                      "rmse": 150.0,
                      "r2": 0.7,
                      "createdAt": "2026-05-20T10:00:00Z"
                    }
                    """.trimIndent()
                )
        )

        val result = repo.predictExpenses()

        val data = (result as DataResult.Success).data
        assertThat(data.byCategory.map { it.rawCategory }).isEqualTo(listOf("HOUSING", "FOOD"))
        assertThat(data.expectedMonthlyExpense).isEqualTo(4000.0)
    }

    @Test
    fun `predictExpenses - categoria desconhecida vira null mas preserva rawCategory`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "predictionId": 8,
                      "modelName": "EXPENSE_RANDOM_FOREST",
                      "horizonMonths": 1,
                      "byCategory": [
                        {"category": "FUTURE_NEW_CATEGORY", "predictedAmount": 500.0}
                      ],
                      "expectedMonthlyExpense": 500.0,
                      "mae": 0.0, "rmse": 0.0, "r2": 1.0,
                      "createdAt": "2026-05-20T10:00:00Z"
                    }
                    """.trimIndent()
                )
        )

        val data = (repo.predictExpenses() as DataResult.Success).data
        val first = data.byCategory.first()
        assertThat(first.category).isNull()
        assertThat(first.rawCategory).isEqualTo("FUTURE_NEW_CATEGORY")
    }

    // ------------------------------------------------------------------------
    // runCalibrated
    // ------------------------------------------------------------------------

    @Test
    fun `runCalibrated - body inclui campos novos e nao inclui monthlyContribution`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(stubCalibratedResponse())
        )

        repo.runCalibrated(
            CalibratedSimulationParameters(
                goalId = 42L,
                initialCapital = 10_000.0,
                expectedReturnAnnual = 0.08,
                volatilityAnnual = 0.15,
                horizonMonths = 120,
                targetAmount = 100_000.0,
                incomeHorizonMonths = 24,
            )
        )

        val body = server.takeRequest().body.readUtf8()
        assertThat(body).contains("\"incomeHorizonMonths\":24")
        assertThat(body).contains("\"goalId\":\"42\"")
        assertThat(body).doesNotContain("monthlyContribution")
    }

    @Test
    fun `runCalibrated - extrai calibration summary corretamente`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(stubCalibratedResponse())
        )

        val result = repo.runCalibrated(
            CalibratedSimulationParameters(
                goalId = 1L,
                initialCapital = 10_000.0,
                expectedReturnAnnual = 0.08,
                volatilityAnnual = 0.15,
                horizonMonths = 120,
                targetAmount = 100_000.0,
            )
        )

        val data = (result as DataResult.Success).data
        assertThat(data.calibration.predictedMonthlyIncome).isEqualTo(5000.0)
        assertThat(data.calibration.predictedMonthlyExpense).isEqualTo(3500.0)
        assertThat(data.calibration.appliedMonthlyContribution).isEqualTo(1500.0)
        assertThat(data.calibration.cappedToZero).isFalse()
    }

    // ------------------------------------------------------------------------
    // listRecent
    // ------------------------------------------------------------------------

    @Test
    fun `listRecent - propaga limit como query parameter`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]")
        )

        repo.listRecent(limit = 25)

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/predictions?limit=25")
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    /**
     * Stub minimo de RunCalibratedSimulationResponseDto. Eh um boilerplate
     * grande porque o backend serializa o SimulationResultResponseDto
     * inteiro (Sprint 2) ali dentro. Os campos sao redundantes para os
     * testes - so precisamos do `calibration` e que a desserializacao
     * nao quebre.
     */
    private fun stubCalibratedResponse(): String = """
        {
          "simulation": {
            "id": "1",
            "goalId": "42",
            "numSimulations": 10000,
            "seed": 12345,
            "targetAmount": 100000.0,
            "successProbability": 0.85,
            "mean": 110000.0,
            "median": 105000.0,
            "standardDeviation": 25000.0,
            "percentiles": {"P10": 80000.0, "P50": 105000.0, "P90": 145000.0},
            "worstCase": 50000.0,
            "bestCase": 200000.0,
            "meanReal": 95000.0,
            "histogram": [{"rangeStart": 0.0, "rangeEnd": 50000.0, "count": 100}],
            "executionTimeMs": 2500,
            "createdAt": "2026-05-20T10:00:00Z"
          },
          "calibration": {
            "incomePredictionId": 10,
            "expensePredictionId": 11,
            "predictedMonthlyIncome": 5000.0,
            "predictedMonthlyExpense": 3500.0,
            "rawMonthlyContribution": 1500.0,
            "appliedMonthlyContribution": 1500.0,
            "appliedVolatilityAnnual": 0.15
          }
        }
    """.trimIndent()
}
