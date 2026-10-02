package com.lifeforge.data.mapper

import com.google.common.truth.Truth.assertThat
import com.lifeforge.data.db.Converters
import com.lifeforge.data.model.dto.CalibrationSummaryResponseDto
import com.lifeforge.data.model.dto.HistogramBucketDto
import com.lifeforge.data.model.dto.IncomeDto
import com.lifeforge.data.model.dto.SimulationResultResponseDto
import com.lifeforge.data.model.dto.SimulationSummaryResponseDto
import com.lifeforge.data.model.dto.TrajectoryBandDto
import com.lifeforge.data.sync.SyncState
import com.lifeforge.domain.model.CalibrationSource
import kotlinx.serialization.json.Json
import org.junit.Test
import java.time.Instant

/**
 * Mapeadores ligados ao offline-first e ao histórico de simulações:
 * estado de sincronização → `pendingSync`, bandas do fan chart preservadas no
 * cache, resumos do histórico e a origem dos insumos da simulação calibrada.
 */
class SyncMappersTest {

    private val result = SimulationResultResponseDto(
        id = "42",
        goalId = "7",
        numSimulations = 10_000,
        seed = 42L,
        targetAmount = 1_000_000.0,
        successProbability = 0.653,
        mean = 1_370_544.0,
        median = 1_203_651.0,
        standardDeviation = 600_000.0,
        percentiles = mapOf("P10" to 669_982.0, "P50" to 1_203_651.0, "P90" to 2_270_223.0),
        worstCase = 500_000.0,
        bestCase = 2_800_000.0,
        meanReal = 600_000.0,
        histogram = listOf(HistogramBucketDto(0.0, 100.0, 3)),
        trajectory = listOf(
            TrajectoryBandDto(0, 50_000.0, 50_000.0, 50_000.0, 50_000.0, 50_000.0),
            TrajectoryBandDto(1, 49_000.0, 50_500.0, 52_000.0, 53_500.0, 55_000.0),
        ),
        executionTimeMs = 5,
        createdAt = "2026-10-02T12:00:00Z",
    )

    @Test
    fun `entidade sincronizada nao fica pendente e alterada localmente fica`() {
        val dto = IncomeDto(1, 1, "Salário", "5000.00", "SALARY", true, "2026-10-05T00:00:00Z", "2026-10-05T00:00:00Z")
        val synced = dto.toEntity()

        assertThat(synced.syncState).isEqualTo(SyncState.SYNCED.name)
        assertThat(synced.toDomain().pendingSync).isFalse()
        assertThat(synced.copy(syncState = SyncState.PENDING_UPDATE.name).toDomain().pendingSync).isTrue()
    }

    @Test
    fun `bandas do fan chart sobrevivem ao cache local da simulacao`() {
        val entity = result.toEntity()

        // Ida e volta pelos conversores do Room (coluna JSON).
        val converters = Converters()
        val stored = converters.trajectoryToJson(entity.trajectory)
        val restored = converters.jsonToTrajectory(stored)!!

        val domain = entity.copy(trajectory = restored).toDomain()
        assertThat(domain.trajectory).hasSize(2)
        assertThat(domain.trajectory[1].p90).isEqualTo(55_000.0)
        assertThat(domain.id).isEqualTo(42L)
    }

    @Test
    fun `resumo do historico vem da rodada completa e da listagem do servidor`() {
        val fromRun = result.toSummaryEntity()
        val fromList = SimulationSummaryResponseDto("43", "7", 0.7, 1.0, 2.0, 3.0, "2026-10-03T00:00:00Z").toEntity()

        assertThat(fromRun.id).isEqualTo(42L)
        assertThat(fromRun.goalId).isEqualTo(7L)
        assertThat(fromRun.successProbability).isEqualTo(0.653)
        assertThat(fromList.toDomain().createdAt).isEqualTo(Instant.parse("2026-10-03T00:00:00Z"))
    }

    @Test
    fun `calibracao com recuo informa a origem de cada insumo`() {
        val json = Json { ignoreUnknownKeys = true }
        val dto = json.decodeFromString(
            CalibrationSummaryResponseDto.serializer(),
            """
            {"incomePredictionId": null, "expensePredictionId": 9,
             "predictedMonthlyIncome": 6000.0, "predictedMonthlyExpense": 3500.0,
             "rawMonthlyContribution": 2500.0, "appliedMonthlyContribution": 2500.0,
             "appliedVolatilityAnnual": 0.05,
             "incomeSource": "PROFILE", "expenseSource": "ML_MODEL",
             "contributionSource": "PREDICTIONS",
             "fallbackNotes": ["Modelo de renda não aplicado: histórico insuficiente"]}
            """.trimIndent(),
        )

        val summary = dto.toDomain()

        assertThat(summary.incomePredictionId).isNull()
        assertThat(summary.incomeSource).isEqualTo(CalibrationSource.PROFILE)
        assertThat(summary.expenseSource).isEqualTo(CalibrationSource.ML_MODEL)
        assertThat(summary.usedFallback).isTrue()
        assertThat(summary.contributionFromProfile).isFalse()
        assertThat(summary.fallbackNotes).hasSize(1)
    }

    @Test
    fun `resposta de backend antigo (sem campos de recuo) continua valida`() {
        val json = Json { ignoreUnknownKeys = true }
        val dto = json.decodeFromString(
            CalibrationSummaryResponseDto.serializer(),
            """
            {"incomePredictionId": 1, "expensePredictionId": 2,
             "predictedMonthlyIncome": 6000.0, "predictedMonthlyExpense": 3500.0,
             "rawMonthlyContribution": 2500.0, "appliedMonthlyContribution": 2500.0,
             "appliedVolatilityAnnual": 0.05}
            """.trimIndent(),
        )

        val summary = dto.toDomain()

        // Sem os campos de origem, os ids de predição indicam que os dois modelos rodaram.
        assertThat(summary.usedFallback).isFalse()
        assertThat(summary.incomeSource).isEqualTo(CalibrationSource.ML_MODEL)
        assertThat(summary.incomePredictionId).isEqualTo(1L)
    }
}
