package com.lifeforge.data.mapper

import com.lifeforge.data.model.dto.ReferenceDataResponseDto
import com.lifeforge.data.model.dto.RiskProfileStatsDto
import com.lifeforge.domain.model.ChildCostBracket
import com.lifeforge.domain.model.ReferenceData
import com.lifeforge.domain.model.RiskProfile

/**
 * Conversao do DTO de rede para o modelo de dominio. As chaves de
 * `byRiskProfile` vem como nomes do enum (CONSERVATIVE/MODERATE/AGGRESSIVE);
 * chaves desconhecidas sao ignoradas (tolerante a divergencia de versao).
 */
fun ReferenceDataResponseDto.toDomain(): ReferenceData = ReferenceData(
    inflationAnnualMean = inflationAnnualMean,
    salaryGrowthAnnualMean = salaryGrowthAnnualMean,
    selicAnnual = selicAnnual,
    unexpectedExpenseAnnualFrequency = unexpectedExpenseAnnualFrequency,
    unexpectedExpenseMeanFractionOfIncome = unexpectedExpenseMeanFractionOfIncome,
    lifeExpectancyYears = lifeExpectancyYears,
    vehicleDepreciationAnnual = vehicleDepreciationAnnual,
    realEstateAppreciationAnnual = realEstateAppreciationAnnual,
    safeWithdrawalRate = safeWithdrawalRate,
    returnByRiskProfile = byRiskProfile.toRiskMap { it.expectedReturnAnnual },
    volatilityByRiskProfile = byRiskProfile.toRiskMap { it.volatilityAnnual },
    childCostByAge = childCostByAge.map { ChildCostBracket(it.ageMaxInclusive, it.monthlyCost) },
)

private inline fun Map<String, RiskProfileStatsDto>.toRiskMap(
    select: (RiskProfileStatsDto) -> Double,
): Map<RiskProfile, Double> = mapNotNull { (key, stats) ->
    runCatching { enumValueOf<RiskProfile>(key) }.getOrNull()?.let { it to select(stats) }
}.toMap()
