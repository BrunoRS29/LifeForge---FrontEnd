package com.lifeforge.presentation.screen.prediction

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.PredictionMetrics
import com.lifeforge.domain.model.WealthHistoryPoint
import com.lifeforge.domain.model.WealthPrediction
import org.junit.Test
import java.time.Instant

/** Aumento médio previsto do patrimônio, em R$ por mês. */
class PredictionTextsTest {

    private fun prediction(lastRealized: Double?, finalWealth: Double, horizon: Int = 12) = WealthPrediction(
        predictionId = 1,
        modelName = "WEALTH_ARIMA",
        horizonMonths = horizon,
        history = listOfNotNull(lastRealized?.let { WealthHistoryPoint(monthIndex = 17, amount = it) }),
        projection = emptyList(),
        expectedFinalWealth = finalWealth,
        monthlyGrowthRate = 0.12,
        metrics = PredictionMetrics(mae = 0.0, rmse = 0.0, r2 = 0.0),
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `aumento medio do ultimo mes realizado ao fim do horizonte`() {
        // De R$ 60.000 para R$ 103.605,82 em 12 meses.
        assertThat(averageMonthlyIncrease(prediction(60_000.0, 103_605.82)))
            .isWithin(1e-6).of((103_605.82 - 60_000.0) / 12)
    }

    @Test
    fun `sem historico realizado nao ha base para o aumento`() {
        assertThat(averageMonthlyIncrease(prediction(null, 103_605.82))).isNull()
    }
}
