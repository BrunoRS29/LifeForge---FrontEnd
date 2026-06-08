package com.lifeforge.domain.model

import kotlin.math.abs
import kotlin.math.pow

/**
 * Projeção determinística de patrimônio (juros compostos com aportes).
 *
 * Implementa a fórmula da Seção 6.1 da proposta do TCC:
 *
 *   P(t) = P0·(1+r)^t + A·[((1+r)^t − 1) / r]    (r ≠ 0)
 *   P(t) = P0 + A·t                               (r = 0)
 *
 * Onde P0 = patrimônio inicial, A = aporte mensal, r = taxa de retorno
 * mensal equivalente à anual informada, t = mês.
 *
 * É a base "projetada" do gráfico real × projetado do Dashboard. É
 * determinística (sem componente estocástico) — a versão probabilística é a
 * simulação de Monte Carlo da tela de Simulação.
 */
object WealthProjection {

    /**
     * Retorna o patrimônio projetado para cada mês de 0 a [months] (inclusive),
     * ou seja, [months] + 1 pontos. O índice 0 é o patrimônio atual ([initial]).
     */
    fun project(
        initial: Double,
        monthlyContribution: Double,
        annualReturnRate: Double,
        months: Int,
    ): List<Double> {
        require(months >= 0) { "months deve ser >= 0" }
        // Taxa mensal equivalente à anual: (1+r_a)^(1/12) − 1
        val r = (1.0 + annualReturnRate).pow(1.0 / 12.0) - 1.0

        return (0..months).map { t ->
            if (abs(r) < 1e-12) {
                initial + monthlyContribution * t
            } else {
                val growth = (1.0 + r).pow(t.toDouble())
                initial * growth + monthlyContribution * (growth - 1.0) / r
            }
        }
    }

    /**
     * Projeção DINÂMICA usada quando há perfil: a renda cresce ao ritmo do
     * crescimento salarial e as despesas pela inflação, mês a mês. O aporte de
     * cada mês é `renda − despesa` (nunca negativo), e o patrimônio rende à
     * taxa informada. Devolve duas séries de [months]+1 pontos (índice 0 = hoje):
     *  - [ProjectionSeries.projected]: com rendimento dos investimentos
     *  - [ProjectionSeries.contributionsOnly]: só os aportes acumulados
     *
     * Diferente de [project] (aporte fixo, fórmula fechada), aqui o aporte
     * evolui, então a soma é iterativa.
     */
    fun projectDynamic(inputs: ProjectionInputs): ProjectionSeries {
        require(inputs.months >= 0) { "months deve ser >= 0" }
        val rRet = monthlyRate(inputs.annualReturn)
        val gSal = monthlyRate(inputs.annualSalaryGrowth)
        val gInf = monthlyRate(inputs.annualInflation)
        // Imóvel valoriza; veículos depreciam (fator mensal de retenção do valor).
        val propMonthlyFactor = (1.0 + inputs.annualPropertyAppreciation).pow(1.0 / 12.0)
        val vehMonthlyFactor = (1.0 - inputs.annualVehicleDepreciation).coerceIn(0.0, 1.0).pow(1.0 / 12.0)
        // Os ativos reais entram igual nas duas linhas (base), então a distância
        // entre elas continua refletindo o efeito de investir.
        val realAssetsBaseline = inputs.initialPropertyValue + inputs.initialVehiclesValue

        val projected = ArrayList<Double>(inputs.months + 1)
        val contributionsOnly = ArrayList<Double>(inputs.months + 1)
        var liquid = inputs.initialWealth
        var property = inputs.initialPropertyValue
        var vehicles = inputs.initialVehiclesValue
        var accumulatedLiquid = inputs.initialWealth
        projected.add(liquid + property + vehicles)
        contributionsOnly.add(accumulatedLiquid + realAssetsBaseline)

        for (m in 1..inputs.months) {
            val inflationFactor = (1.0 + gInf).pow((m - 1).toDouble())
            val income = inputs.monthlyIncome * (1.0 + gSal).pow((m - 1).toDouble())
            // Custo dos filhos: cada um envelhece ao longo do horizonte e muda de
            // faixa; o valor base também é corrigido pela inflação.
            val yearsElapsed = (m - 1) / 12
            val childCost = inputs.childrenAges.sumOf {
                childMonthlyCost(it + yearsElapsed, inputs.childCostByAge)
            } * inflationFactor
            val expenses = inputs.monthlyExpenses * inflationFactor + childCost
            val contribution = (income - expenses).coerceAtLeast(0.0)
            liquid = liquid * (1.0 + rRet) + contribution
            property *= propMonthlyFactor
            vehicles *= vehMonthlyFactor
            accumulatedLiquid += contribution
            projected.add(liquid + property + vehicles)
            contributionsOnly.add(accumulatedLiquid + realAssetsBaseline)
        }
        return ProjectionSeries(projected, contributionsOnly)
    }

    /**
     * Retorno anual nominal típico por perfil de risco (premissa-base).
     * Mesmos valores da base de referência do backend (ReferenceData):
     * conservador 9% · moderado 11% · arrojado 13%.
     */
    fun returnForRiskProfile(riskProfile: RiskProfile?): Double = when (riskProfile) {
        RiskProfile.CONSERVATIVE -> 0.09
        RiskProfile.AGGRESSIVE -> 0.13
        else -> 0.11 // MODERATE / não informado
    }

    /** Taxa mensal equivalente à anual: (1+a)^(1/12) − 1. */
    private fun monthlyRate(annual: Double): Double =
        if (abs(annual) < 1e-12) 0.0 else (1.0 + annual).pow(1.0 / 12.0) - 1.0

    /** Custo mensal de um filho na idade [ageYears] conforme [brackets]; 0 se ausente. */
    private fun childMonthlyCost(ageYears: Int, brackets: List<ChildCostBracket>): Double =
        brackets.firstOrNull { ageYears <= it.ageMaxInclusive }?.monthlyCost ?: 0.0
}

/** Entradas da projeção dinâmica (todas anuais, exceto valores mensais). */
data class ProjectionInputs(
    val initialWealth: Double,
    val monthlyIncome: Double,        // renda usada para o aporte (salário)
    val monthlyExpenses: Double,
    val annualReturn: Double,
    val annualSalaryGrowth: Double,
    val annualInflation: Double,
    val months: Int,
    // Ativos reais (opcionais; 0 = sem efeito): imóvel valoriza, veículos depreciam.
    val initialPropertyValue: Double = 0.0,
    val annualPropertyAppreciation: Double = 0.0,
    val initialVehiclesValue: Double = 0.0,
    val annualVehicleDepreciation: Double = 0.0,
    // Custo de filhos por faixa etária (opcional): idades atuais + tabela de custos.
    val childrenAges: List<Int> = emptyList(),
    val childCostByAge: List<ChildCostBracket> = emptyList(),
)

data class ProjectionSeries(
    val projected: List<Double>,
    val contributionsOnly: List<Double>,
) {
    val finalProjected: Double get() = projected.last()
    val finalContributionsOnly: Double get() = contributionsOnly.last()
}
