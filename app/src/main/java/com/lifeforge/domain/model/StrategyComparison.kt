package com.lifeforge.domain.model

import kotlin.math.abs

/**
 * Comparação lado a lado de duas estratégias para a mesma meta (proposta,
 * Seção 8.3): o que muda nas premissas e o que muda nos resultados.
 */
enum class StrategySide { A, B }

/** Métricas comparadas. [higherIsBetter] null = premissa, sem "melhor" universal. */
enum class StrategyMetric(val higherIsBetter: Boolean?) {
    SUCCESS_PROBABILITY(true),
    MEDIAN(true),
    PESSIMISTIC_P10(true),
    OPTIMISTIC_P90(true),
    MEAN_REAL(true),

    // Premissas: aporte maior custa mais; retorno maior costuma vir com mais risco.
    MONTHLY_CONTRIBUTION(null),
    EXPECTED_RETURN(null),
    VOLATILITY(null),
    HORIZON_MONTHS(null),
    INFLATION(null),
    UNEMPLOYMENT(null),
}

data class MetricComparison(
    val metric: StrategyMetric,
    val a: Double,
    val b: Double,
    /** Lado com o melhor valor; null em empate ou quando a métrica é premissa. */
    val better: StrategySide?,
) {
    val differs: Boolean get() = abs(a - b) > 1e-9
}

data class StrategyComparison(
    val a: SimulationResult,
    val b: SimulationResult,
    val outcomes: List<MetricComparison>,
    /** Premissas conhecidas das duas rodadas (vazia se alguma não as informa). */
    val premises: List<MetricComparison>,
    /** Estratégia recomendada; null quando não há como desempatar. */
    val recommended: StrategySide?,
    /** P(sucesso) de B menos a de A, em pontos percentuais. */
    val probabilityDeltaPp: Double,
    /** A diferença de probabilidade está dentro do ruído amostral das simulações. */
    val technicalTie: Boolean,
)

object StrategyComparator {

    /**
     * Diferenças de probabilidade abaixo de 1 ponto percentual são tratadas
     * como empate técnico: com 10.000 cenários e p ≈ 0,65, o erro-padrão da
     * estimativa é √(p(1−p)/N) ≈ 0,48 p.p., ou seja, ±0,94 p.p. a 95%.
     */
    const val TECHNICAL_TIE_PP = 1.0

    fun compare(a: SimulationResult, b: SimulationResult): StrategyComparison {
        val outcomes = listOf(
            metric(StrategyMetric.SUCCESS_PROBABILITY, a.successProbability, b.successProbability),
            metric(StrategyMetric.MEDIAN, a.median, b.median),
            metric(StrategyMetric.PESSIMISTIC_P10, a.p10(), b.p10()),
            metric(StrategyMetric.OPTIMISTIC_P90, a.p90(), b.p90()),
            metric(StrategyMetric.MEAN_REAL, a.meanReal, b.meanReal),
        )

        val ia = a.inputs
        val ib = b.inputs
        val premises = if (ia != null && ib != null) {
            listOf(
                metric(StrategyMetric.MONTHLY_CONTRIBUTION, ia.monthlyContribution, ib.monthlyContribution),
                metric(StrategyMetric.EXPECTED_RETURN, ia.expectedReturnAnnual, ib.expectedReturnAnnual),
                metric(StrategyMetric.VOLATILITY, ia.volatilityAnnual, ib.volatilityAnnual),
                metric(StrategyMetric.HORIZON_MONTHS, ia.horizonMonths.toDouble(), ib.horizonMonths.toDouble()),
                metric(StrategyMetric.INFLATION, ia.inflationAnnual, ib.inflationAnnual),
                metric(StrategyMetric.UNEMPLOYMENT, ia.unemploymentProbAnnual, ib.unemploymentProbAnnual),
            )
        } else {
            emptyList()
        }

        val deltaPp = (b.successProbability - a.successProbability) * 100.0
        val tie = abs(deltaPp) < TECHNICAL_TIE_PP
        val recommended = when {
            !tie -> if (deltaPp > 0) StrategySide.B else StrategySide.A
            // Empate técnico: a estratégia que exige menos aporte é a mais eficiente...
            ia != null && ib != null && relativeDiff(ia.monthlyContribution, ib.monthlyContribution) > 0.01 ->
                if (ia.monthlyContribution < ib.monthlyContribution) StrategySide.A else StrategySide.B
            // ...senão, a mais robusta (cenário pessimista maior).
            relativeDiff(a.p10(), b.p10()) > 0.01 -> if (a.p10() > b.p10()) StrategySide.A else StrategySide.B
            else -> null
        }

        return StrategyComparison(
            a = a,
            b = b,
            outcomes = outcomes,
            premises = premises,
            recommended = recommended,
            probabilityDeltaPp = deltaPp,
            technicalTie = tie,
        )
    }

    private fun metric(metric: StrategyMetric, a: Double, b: Double): MetricComparison {
        val better = when {
            metric.higherIsBetter == null || abs(a - b) <= 1e-9 -> null
            metric.higherIsBetter == (a > b) -> StrategySide.A
            else -> StrategySide.B
        }
        return MetricComparison(metric, a, b, better)
    }

    private fun relativeDiff(x: Double, y: Double): Double {
        val base = maxOf(abs(x), abs(y))
        return if (base == 0.0) 0.0 else abs(x - y) / base
    }
}

/** Cenário pessimista (P10); recua para o pior caso (P5) se o percentil faltar. */
fun SimulationResult.p10(): Double = percentiles["P10"] ?: worstCase

/** Cenário otimista (P90); recua para o melhor caso (P95) se o percentil faltar. */
fun SimulationResult.p90(): Double = percentiles["P90"] ?: bestCase
