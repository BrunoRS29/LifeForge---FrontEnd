package com.lifeforge.data.mapper

import com.google.common.truth.Truth.assertThat
import com.lifeforge.data.model.dto.UserProfileDto
import com.lifeforge.domain.model.EmploymentType
import com.lifeforge.domain.model.HousingStatus
import com.lifeforge.domain.model.MaritalStatus
import com.lifeforge.domain.model.RiskLevel
import com.lifeforge.domain.model.TaxRegime
import com.lifeforge.domain.model.UserProfile
import org.junit.Test

/**
 * Testa o mapeamento do perfil estendido (DTO <-> dominio), incluindo os
 * campos novos (idades dos filhos, expectativa de vida, valor do imovel e dos
 * veiculos) e a conversao tolerante de enums desconhecidos.
 */
class ProfileMappersTest {

    @Test
    fun `round-trip preserva todos os campos do perfil`() {
        val profile = UserProfile(
            age = 30,
            monthlySalary = "18480",
            employmentType = EmploymentType.CLT,
            retirementAge = 60,
            monthlyContribution = "2000",
            maritalStatus = MaritalStatus.MARRIED,
            dependents = 2,
            childrenAges = "3, 7",
            state = "SP",
            lifeExpectancy = 85,
            expectedSalaryGrowth = "6",
            unemploymentRisk = RiskLevel.LOW,
            housingStatus = HousingStatus.FINANCED,
            housingMonthlyCost = "2500",
            propertyValue = "500000",
            vehiclesValue = "80000",
            taxRegime = TaxRegime.CLT,
            emergencyReserve = "30000",
            totalDebt = "15000",
            plansChildren = true,
            plansProperty = false,
        )

        assertThat(profile.toDto().toDomain()).isEqualTo(profile)
    }

    @Test
    fun `enum desconhecido vira null no toDomain (tolerante a versao)`() {
        val dto = UserProfileDto(employmentType = "BOGUS", maritalStatus = "INVALID")

        val domain = dto.toDomain()

        assertThat(domain.employmentType).isNull()
        assertThat(domain.maritalStatus).isNull()
    }
}
