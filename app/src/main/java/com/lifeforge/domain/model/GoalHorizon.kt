package com.lifeforge.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Horizonte, em meses, de hoje até a data-alvo de uma meta — pré-preenche a
 * simulação, a simulação com IA e a otimização.
 *
 * Meses de calendário (no fuso de São Paulo), arredondando a sobra de dias:
 * 3 anos, 7 meses e 30 dias → 44 meses. A conta anterior (dias ÷ 30)
 * superestimava cerca de 5 meses em 27 anos, porque o mês médio tem 30,44 dias.
 */
object GoalHorizon {

    private val ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")

    /** No mínimo 1 mês (data-alvo hoje ou no passado) e no máximo 100 anos. */
    fun months(from: Instant, to: Instant, zone: ZoneId = ZONE): Int {
        val start = from.atZone(zone).toLocalDate()
        val end = to.atZone(zone).toLocalDate()
        if (!end.isAfter(start)) return 1
        val whole = ChronoUnit.MONTHS.between(start, end)
        val leftoverDays = ChronoUnit.DAYS.between(start.plusMonths(whole), end)
        val rounded = if (leftoverDays >= 15) whole + 1 else whole
        return rounded.coerceIn(1L, 1200L).toInt()
    }
}
