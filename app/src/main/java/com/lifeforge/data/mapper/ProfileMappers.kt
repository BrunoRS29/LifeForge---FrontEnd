package com.lifeforge.data.mapper

import com.lifeforge.data.model.dto.UserProfileDto
import com.lifeforge.domain.model.EmploymentType
import com.lifeforge.domain.model.HousingStatus
import com.lifeforge.domain.model.MaritalStatus
import com.lifeforge.domain.model.RiskLevel
import com.lifeforge.domain.model.TaxRegime
import com.lifeforge.domain.model.UserProfile

/**
 * Conversões entre [UserProfileDto] (rede) e [UserProfile] (domínio). Enums
 * desconhecidos viram null (tolerante a divergências de versão).
 */
fun UserProfileDto.toDomain(): UserProfile = UserProfile(
    age = age,
    monthlySalary = monthlySalary,
    employmentType = employmentType.toEnum<EmploymentType>(),
    retirementAge = retirementAge,
    monthlyContribution = monthlyContribution,
    maritalStatus = maritalStatus.toEnum<MaritalStatus>(),
    dependents = dependents,
    childrenAges = childrenAges,
    state = state,
    lifeExpectancy = lifeExpectancy,
    expectedSalaryGrowth = expectedSalaryGrowth,
    unemploymentRisk = unemploymentRisk.toEnum<RiskLevel>(),
    housingStatus = housingStatus.toEnum<HousingStatus>(),
    housingMonthlyCost = housingMonthlyCost,
    propertyValue = propertyValue,
    vehiclesValue = vehiclesValue,
    taxRegime = taxRegime.toEnum<TaxRegime>(),
    emergencyReserve = emergencyReserve,
    totalDebt = totalDebt,
    plansChildren = plansChildren,
    plansProperty = plansProperty,
)

fun UserProfile.toDto(): UserProfileDto = UserProfileDto(
    age = age,
    monthlySalary = monthlySalary,
    employmentType = employmentType?.name,
    retirementAge = retirementAge,
    monthlyContribution = monthlyContribution,
    maritalStatus = maritalStatus?.name,
    dependents = dependents,
    childrenAges = childrenAges,
    state = state,
    lifeExpectancy = lifeExpectancy,
    expectedSalaryGrowth = expectedSalaryGrowth,
    unemploymentRisk = unemploymentRisk?.name,
    housingStatus = housingStatus?.name,
    housingMonthlyCost = housingMonthlyCost,
    propertyValue = propertyValue,
    vehiclesValue = vehiclesValue,
    taxRegime = taxRegime?.name,
    emergencyReserve = emergencyReserve,
    totalDebt = totalDebt,
    plansChildren = plansChildren,
    plansProperty = plansProperty,
)

private inline fun <reified T : Enum<T>> String?.toEnum(): T? =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
