package com.lifeforge.domain.model

/**
 * Premissas de longo prazo da base de referencia do backend (GET
 * /reference-data). Substituem as constantes locais nas projecoes: retorno por
 * perfil de risco, inflacao e crescimento salarial vem daqui (calibrados por
 * dados publicos brasileiros e pela literatura), com fallback local quando
 * offline.
 *
 * Valores anuais em fracao (0.045 = 4,5%).
 */
data class ReferenceData(
    val inflationAnnualMean: Double,
    val salaryGrowthAnnualMean: Double,
    val selicAnnual: Double,
    val unexpectedExpenseAnnualFrequency: Double,
    val unexpectedExpenseMeanFractionOfIncome: Double,
    val lifeExpectancyYears: Int,
    val returnByRiskProfile: Map<RiskProfile, Double>,
    val volatilityByRiskProfile: Map<RiskProfile, Double>,
) {
    /**
     * Retorno anual esperado para o perfil informado; cai no MODERATE quando o
     * perfil e nulo, e retorna null se nem o MODERATE estiver presente (deixa o
     * chamador decidir o fallback local).
     */
    fun returnForRiskProfile(riskProfile: RiskProfile?): Double? =
        returnByRiskProfile[riskProfile ?: RiskProfile.MODERATE]
            ?: returnByRiskProfile[RiskProfile.MODERATE]
}
