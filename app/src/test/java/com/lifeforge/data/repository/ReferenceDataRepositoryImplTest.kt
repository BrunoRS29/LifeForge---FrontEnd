package com.lifeforge.data.repository

import com.google.common.truth.Truth.assertThat
import com.lifeforge.data.api.ReferenceApi
import com.lifeforge.data.model.dto.ReferenceDataResponseDto
import com.lifeforge.data.model.dto.RiskProfileStatsDto
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.RiskProfile
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import retrofit2.Response

/**
 * Testa o repositorio das premissas de referencia: mapeamento DTO -> dominio e,
 * sobretudo, o cache em memoria (a primeira chamada bem sucedida e reaproveitada
 * pelas seguintes, sem novo acesso a rede).
 */
class ReferenceDataRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeReferenceApi(private val dto: ReferenceDataResponseDto) : ReferenceApi {
        var calls = 0
        override suspend fun getReferenceData(): Response<ReferenceDataResponseDto> {
            calls++
            return Response.success(dto)
        }
    }

    private fun dto() = ReferenceDataResponseDto(
        inflationAnnualMean = 0.045,
        inflationAnnualStdDev = 0.025,
        salaryGrowthAnnualMean = 0.06,
        salaryGrowthAnnualStdDev = 0.03,
        selicAnnual = 0.105,
        riskFreeAnnual = 0.10,
        unemploymentDurationMonths = 6,
        defaultUnemploymentProbAnnual = 0.10,
        unexpectedExpenseAnnualFrequency = 1.5,
        unexpectedExpenseMeanFractionOfIncome = 0.5,
        lifeExpectancyYears = 77,
        byRiskProfile = mapOf("MODERATE" to RiskProfileStatsDto(0.11, 0.10)),
        byEmploymentType = emptyMap(),
        childCostByAge = emptyList(),
    )

    @Test
    fun `mapeia o DTO e devolve sucesso`() = runTest {
        val repo = ReferenceDataRepositoryImpl(FakeReferenceApi(dto()), json)

        val result = repo.getReferenceData()

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        val data = (result as DataResult.Success).data
        assertThat(data.inflationAnnualMean).isEqualTo(0.045)
        assertThat(data.returnForRiskProfile(RiskProfile.MODERATE)).isEqualTo(0.11)
    }

    @Test
    fun `busca uma vez e cacheia para chamadas seguintes`() = runTest {
        val api = FakeReferenceApi(dto())
        val repo = ReferenceDataRepositoryImpl(api, json)

        repo.getReferenceData()
        repo.getReferenceData()
        repo.getReferenceData()

        assertThat(api.calls).isEqualTo(1)
    }
}
