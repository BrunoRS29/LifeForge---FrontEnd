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
    /** Retorno livre de risco (CDI/Tesouro Selic) — "100% do CDI". */
    val cdiAnnual: Double,
    /** Volatilidade anual do CDI (quase nula; cenário mais seguro). */
    val cdiVolatilityAnnual: Double,
    /** Probabilidade anual de desemprego por vínculo (chaves do EmploymentType). */
    val unemploymentProbByEmploymentType: Map<String, Double>,
    val defaultUnemploymentProbAnnual: Double,
    val unemploymentDurationMonths: Int,
    val unexpectedExpenseAnnualFrequency: Double,
    val unexpectedExpenseMeanFractionOfIncome: Double,
    val lifeExpectancyYears: Int,
    val vehicleDepreciationAnnual: Double,
    val realEstateAppreciationAnnual: Double,
    val safeWithdrawalRate: Double,
    val returnByRiskProfile: Map<RiskProfile, Double>,
    val volatilityByRiskProfile: Map<RiskProfile, Double>,
    val childCostByAge: List<ChildCostBracket>,
) {
    /**
     * Retorno anual esperado para o perfil informado; cai no MODERATE quando o
     * perfil e nulo, e retorna null se nem o MODERATE estiver presente (deixa o
     * chamador decidir o fallback local).
     */
    fun returnForRiskProfile(riskProfile: RiskProfile?): Double? =
        returnByRiskProfile[riskProfile ?: RiskProfile.MODERATE]
            ?: returnByRiskProfile[RiskProfile.MODERATE]

    /** Custo mensal medio de um filho na idade [ageYears]; 0 quando independente. */
    fun childMonthlyCost(ageYears: Int): Double =
        childCostByAge.firstOrNull { ageYears <= it.ageMaxInclusive }?.monthlyCost ?: 0.0

    /**
     * Probabilidade anual de desemprego para o vinculo informado (nome do
     * enum EmploymentType); cai no default da base quando desconhecido/nulo.
     */
    fun unemploymentProbFor(employmentType: String?): Double =
        employmentType?.let { unemploymentProbByEmploymentType[it] }
            ?: defaultUnemploymentProbAnnual
}

/** Faixa etaria (ate [ageMaxInclusive] anos) e custo mensal medio associado. */
data class ChildCostBracket(
    val ageMaxInclusive: Int,
    val monthlyCost: Double,
)
