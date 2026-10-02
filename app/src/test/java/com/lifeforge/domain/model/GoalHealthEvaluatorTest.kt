package com.lifeforge.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/** Semáforo de saúde das metas a partir da última simulação. */
class GoalHealthEvaluatorTest {

    private val now = Instant.parse("2026-10-02T00:00:00Z")

    private fun goal(id: Long = 1, targetDate: String = "2028-04-02T00:00:00Z") = Goal(
        id = id, userId = 1, name = "Meta $id", category = GoalCategory.TRAVEL,
        targetAmount = BigDecimal("50000"), targetDate = Instant.parse(targetDate), priority = 1,
        createdAt = Instant.EPOCH,
    )

    private fun simulation(goalId: Long, probability: Double) = SimulationSummary(
        id = 10, goalId = goalId, successProbability = probability, mean = 0.0, median = 0.0,
        targetAmount = 50_000.0, createdAt = now,
    )

    @Test
    fun `limiares seguem o alvo de 80 por cento da otimizacao`() {
        assertThat(GoalHealthEvaluator.statusFor(0.80)).isEqualTo(GoalHealthStatus.ON_TRACK)
        assertThat(GoalHealthEvaluator.statusFor(0.7999)).isEqualTo(GoalHealthStatus.ATTENTION)
        assertThat(GoalHealthEvaluator.statusFor(0.50)).isEqualTo(GoalHealthStatus.ATTENTION)
        assertThat(GoalHealthEvaluator.statusFor(0.4999)).isEqualTo(GoalHealthStatus.AT_RISK)
    }

    @Test
    fun `meta sem simulacao e meta criada offline tem estados proprios`() {
        assertThat(GoalHealthEvaluator.evaluate(goal(), null, now).status)
            .isEqualTo(GoalHealthStatus.NOT_SIMULATED)
        assertThat(GoalHealthEvaluator.evaluate(goal(id = -1), null, now).status)
            .isEqualTo(GoalHealthStatus.PENDING_SYNC)
    }

    @Test
    fun `meses restantes ate o prazo e zero quando vencido`() {
        assertThat(GoalHealthEvaluator.evaluate(goal(), null, now).monthsLeft).isEqualTo(18)
        assertThat(GoalHealthEvaluator.evaluate(goal(targetDate = "2020-01-01T00:00:00Z"), null, now).monthsLeft)
            .isEqualTo(0)
    }

    @Test
    fun `painel mostra primeiro as metas que pedem acao`() {
        val onTrack = GoalHealthEvaluator.evaluate(goal(1), simulation(1, 0.9), now)
        val atRisk = GoalHealthEvaluator.evaluate(goal(2), simulation(2, 0.3), now)
        val notSimulated = GoalHealthEvaluator.evaluate(goal(3), null, now)
        val attention = GoalHealthEvaluator.evaluate(goal(4), simulation(4, 0.6), now)

        val sorted = GoalHealthEvaluator.sortForDashboard(listOf(onTrack, atRisk, notSimulated, attention))

        assertThat(sorted.map { it.goal.id }).containsExactly(2L, 4L, 3L, 1L).inOrder()
    }
}
