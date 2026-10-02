package com.lifeforge.presentation.screen.goal

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.domain.model.GoalHealth
import com.lifeforge.domain.model.GoalHealthStatus
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/** Subtítulo da barra da lista de metas: total e quantas estão no caminho. */
class GoalsSubtitleTest {

    private fun health(id: Long, status: GoalHealthStatus) = GoalHealth(
        goal = Goal(
            id = id, userId = 1, name = "Meta $id", category = GoalCategory.TRAVEL,
            targetAmount = BigDecimal("1000"), targetDate = Instant.parse("2030-01-01T00:00:00Z"),
            priority = 1, createdAt = Instant.EPOCH,
        ),
        latest = null,
        status = status,
        monthsLeft = 12,
    )

    @Test
    fun `sem metas o subtitulo convida a planejar`() {
        assertThat(goalsSubtitle(emptyList())).isEqualTo("Planeje e simule seus objetivos")
    }

    @Test
    fun `conta as metas e as que estao no caminho`() {
        val goals = listOf(
            health(1, GoalHealthStatus.ON_TRACK),
            health(2, GoalHealthStatus.AT_RISK),
            health(3, GoalHealthStatus.ON_TRACK),
        )
        assertThat(goalsSubtitle(goals)).isEqualTo("3 metas · 2 no caminho")
    }

    @Test
    fun `singular e sem metas no caminho`() {
        assertThat(goalsSubtitle(listOf(health(1, GoalHealthStatus.NOT_SIMULATED)))).isEqualTo("1 meta")
    }
}
