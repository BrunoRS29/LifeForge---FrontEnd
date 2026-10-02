package com.lifeforge.domain.model

import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * "Saúde" de uma meta: a leitura de uma linha da última Simulação de Monte
 * Carlo feita para ela — o indicador que a proposta pede no resumo das metas
 * ativas do painel (Seção 8.4).
 */
enum class GoalHealthStatus {
    /** P(sucesso) ≥ 80% — o limiar que o módulo de otimização persegue. */
    ON_TRACK,

    /** 50% ≤ P(sucesso) < 80%. */
    ATTENTION,

    /** P(sucesso) < 50%. */
    AT_RISK,

    /** A meta ainda não foi simulada. */
    NOT_SIMULATED,

    /** Criada sem conexão: só pode ser simulada depois de sincronizada. */
    PENDING_SYNC,
}

data class GoalHealth(
    val goal: Goal,
    val latest: SimulationSummary?,
    val status: GoalHealthStatus,
    /** Meses inteiros até o prazo (0 se já venceu). */
    val monthsLeft: Long,
)

object GoalHealthEvaluator {

    /** Mesmo limiar padrão da otimização de aporte (TCC, Seção 4.6). */
    const val ON_TRACK_PROBABILITY = 0.80
    const val ATTENTION_PROBABILITY = 0.50

    fun statusFor(probability: Double): GoalHealthStatus = when {
        probability >= ON_TRACK_PROBABILITY -> GoalHealthStatus.ON_TRACK
        probability >= ATTENTION_PROBABILITY -> GoalHealthStatus.ATTENTION
        else -> GoalHealthStatus.AT_RISK
    }

    fun evaluate(goal: Goal, latest: SimulationSummary?, now: Instant): GoalHealth {
        val status = when {
            goal.isLocalOnly() -> GoalHealthStatus.PENDING_SYNC
            latest == null -> GoalHealthStatus.NOT_SIMULATED
            else -> statusFor(latest.successProbability)
        }
        return GoalHealth(goal = goal, latest = latest, status = status, monthsLeft = monthsBetween(now, goal.targetDate))
    }

    /**
     * Ordena para o painel: metas em risco primeiro (pedem ação), depois as que
     * pedem atenção, as não simuladas e as no caminho; empate pelo prazo.
     */
    fun sortForDashboard(items: List<GoalHealth>): List<GoalHealth> =
        items.sortedWith(compareBy<GoalHealth> { it.status.ordinalForDashboard() }.thenBy { it.goal.targetDate })

    private fun GoalHealthStatus.ordinalForDashboard(): Int = when (this) {
        GoalHealthStatus.AT_RISK -> 0
        GoalHealthStatus.ATTENTION -> 1
        GoalHealthStatus.NOT_SIMULATED -> 2
        GoalHealthStatus.PENDING_SYNC -> 3
        GoalHealthStatus.ON_TRACK -> 4
    }

    private fun monthsBetween(from: Instant, to: Instant): Long {
        if (!to.isAfter(from)) return 0
        return ChronoUnit.MONTHS.between(from.atZone(ZoneOffset.UTC), to.atZone(ZoneOffset.UTC))
    }
}
