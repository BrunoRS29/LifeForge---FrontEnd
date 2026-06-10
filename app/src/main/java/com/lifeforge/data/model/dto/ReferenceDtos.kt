package com.lifeforge.data.model.dto

import kotlinx.serialization.Serializable

/**
 * Contrato do GET /api/v1/reference-data (publico). Espelha o
 * `com.lifeforge.dto.ReferenceDataResponse` do backend: as premissas de longo
 * prazo (inflacao, retorno por perfil, etc.) usadas para calibrar projecoes e
 * Monte Carlo. Todos os valores anuais em fracao (0.045 = 4,5%).
 */
@Serializable
data class ReferenceDataResponseDto(
    val inflationAnnualMean: Double,
    val inflationAnnualStdDev: Double,
    val salaryGrowthAnnualMean: Double,
    val salaryGrowthAnnualStdDev: Double,
    val selicAnnual: Double,
    val riskFreeAnnual: Double,
    val cdiVolatilityAnnual: Double = 0.005,
    val unemploymentDurationMonths: Int,
    val defaultUnemploymentProbAnnual: Double,
    val unexpectedExpenseAnnualFrequency: Double,
    val unexpectedExpenseMeanFractionOfIncome: Double,
    val lifeExpectancyYears: Int,
    val vehicleDepreciationAnnual: Double = 0.0,
    val realEstateAppreciationAnnual: Double = 0.0,
    val safeWithdrawalRate: Double = 0.04,
    val byRiskProfile: Map<String, RiskProfileStatsDto> = emptyMap(),
    val byEmploymentType: Map<String, EmploymentStatsDto> = emptyMap(),
    val childCostByAge: List<ChildCostBracketDto> = emptyList(),
)

@Serializable
data class RiskProfileStatsDto(
    val expectedReturnAnnual: Double,
    val volatilityAnnual: Double,
)

@Serializable
data class EmploymentStatsDto(
    val unemploymentProbAnnual: Double,
    val incomeVolatilityAnnual: Double,
)

@Serializable
data class ChildCostBracketDto(
    val ageMaxInclusive: Int,
    val monthlyCost: Double,
)
