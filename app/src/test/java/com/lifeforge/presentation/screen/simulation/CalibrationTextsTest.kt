package com.lifeforge.presentation.screen.simulation

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.CalibrationSummary
import org.junit.Test

/** Texto que explica como a incerteza entra na simulação calibrada pela IA. */
class CalibrationTextsTest {

    private fun summary(volatility: Double, contributionVariation: Double) = CalibrationSummary(
        incomePredictionId = 46,
        expensePredictionId = 47,
        predictedMonthlyIncome = 10_844.14,
        predictedMonthlyExpense = 6_912.34,
        rawMonthlyContribution = 3_931.80,
        appliedMonthlyContribution = 3_931.80,
        appliedVolatilityAnnual = volatility,
        contributionVariationMonthly = contributionVariation,
    )

    @Test
    fun `carteira com a volatilidade de mercado e aporte com a incerteza da renda`() {
        assertThat(calibrationRiskText(summary(volatility = 0.10, contributionVariation = 0.526)))
            .isEqualTo("Volatilidade da carteira: 10% a.a. (mercado). Incerteza da renda: o aporte varia ±52,6% ao mês.")
    }

    @Test
    fun `sem incerteza de renda estimada, so a volatilidade da carteira`() {
        assertThat(calibrationRiskText(summary(volatility = 0.03, contributionVariation = 0.0)))
            .isEqualTo("Volatilidade da carteira: 3% a.a. (mercado).")
    }
}
