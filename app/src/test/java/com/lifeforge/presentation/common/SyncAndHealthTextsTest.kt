package com.lifeforge.presentation.common

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.domain.model.GoalHealth
import com.lifeforge.domain.model.GoalHealthStatus
import com.lifeforge.domain.model.SimulationSummary
import com.lifeforge.domain.repository.SyncStatus
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/** Textos exibidos (e lidos pelo TalkBack) da sincronização e da saúde das metas. */
class SyncAndHealthTextsTest {

    @Test
    fun `faixa de sincronizacao explica o estado em portugues`() {
        assertThat(syncStatusMessage(SyncStatus(isOnline = false, pendingOperations = 0)))
            .isEqualTo("Sem conexão · exibindo os dados salvos no aparelho")
        assertThat(syncStatusMessage(SyncStatus(isOnline = false, pendingOperations = 1)))
            .isEqualTo("Sem conexão · 1 alteração aguardando envio")
        assertThat(syncStatusMessage(SyncStatus(isOnline = true, pendingOperations = 3)))
            .isEqualTo("3 alterações aguardando envio")
        assertThat(syncStatusMessage(SyncStatus(isOnline = true, pendingOperations = 0)))
            .isEqualTo("Tudo sincronizado")
    }

    @Test
    fun `prazo restante por extenso`() {
        assertThat(formatMonthsLeft(0)).isEqualTo("prazo encerrado")
        assertThat(formatMonthsLeft(1)).isEqualTo("faltam 1 mês")
        assertThat(formatMonthsLeft(12)).isEqualTo("faltam 1 ano")
        assertThat(formatMonthsLeft(27)).isEqualTo("faltam 2 anos e 3 meses")
    }

    @Test
    fun `selo de saude traz o estado e a probabilidade`() {
        val goal = Goal(
            id = 1, userId = 1, name = "Casa", category = GoalCategory.REAL_ESTATE,
            targetAmount = BigDecimal("300000"), targetDate = Instant.parse("2030-01-01T00:00:00Z"),
            priority = 1, createdAt = Instant.EPOCH,
        )
        val latest = SimulationSummary(
            id = 5, goalId = 1, successProbability = 0.657, mean = 0.0, median = 0.0,
            targetAmount = 300_000.0, createdAt = Instant.EPOCH,
        )

        val label = goalHealthLabel(GoalHealth(goal, latest, GoalHealthStatus.ATTENTION, monthsLeft = 40))

        assertThat(label).startsWith("Atenção · ")
        assertThat(label).contains("65")
        assertThat(goalHealthLabel(GoalHealth(goal, null, GoalHealthStatus.NOT_SIMULATED, 40)))
            .isEqualTo("Ainda não simulada")
    }
}
