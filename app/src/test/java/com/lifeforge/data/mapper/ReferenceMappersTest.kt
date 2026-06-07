package com.lifeforge.data.mapper

import com.google.common.truth.Truth.assertThat
import com.lifeforge.data.model.dto.ReferenceDataResponseDto
import com.lifeforge.data.model.dto.RiskProfileStatsDto
import com.lifeforge.domain.model.RiskProfile
import org.junit.Test

/**
 * Testes do mapeamento DTO -> dominio das premissas de referencia, com foco
 * na conversao das chaves de perfil de risco (nomes do enum) e no fallback de
 * [com.lifeforge.domain.model.ReferenceData.returnForRiskProfile].
 */
class ReferenceMappersTest {

    private fun dto(byRisk: Map<String, RiskProfileStatsDto>) = ReferenceDataResponseDto(
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
        byRiskProfile = byRisk,
        byEmploymentType = emptyMap(),
    )

    @Test
    fun `mapeia chaves de perfil de risco e campos planos`() {
        val domain = dto(
            mapOf(
                "CONSERVATIVE" to RiskProfileStatsDto(0.09, 0.03),
                "MODERATE" to RiskProfileStatsDto(0.11, 0.10),
                "AGGRESSIVE" to RiskProfileStatsDto(0.13, 0.18),
            )
        ).toDomain()

        assertThat(domain.inflationAnnualMean).isEqualTo(0.045)
        assertThat(domain.salaryGrowthAnnualMean).isEqualTo(0.06)
        assertThat(domain.returnByRiskProfile[RiskProfile.AGGRESSIVE]).isEqualTo(0.13)
        assertThat(domain.volatilityByRiskProfile[RiskProfile.AGGRESSIVE]).isEqualTo(0.18)
        assertThat(domain.returnForRiskProfile(RiskProfile.CONSERVATIVE)).isEqualTo(0.09)
    }

    @Test
    fun `chave desconhecida e ignorada e fallback usa MODERATE`() {
        val domain = dto(
            mapOf(
                "MODERATE" to RiskProfileStatsDto(0.11, 0.10),
                "BOGUS" to RiskProfileStatsDto(0.99, 0.99),
            )
        ).toDomain()

        assertThat(domain.returnByRiskProfile).hasSize(1)
        // riskProfile nulo -> cai no MODERATE.
        assertThat(domain.returnForRiskProfile(null)).isEqualTo(0.11)
    }
}
