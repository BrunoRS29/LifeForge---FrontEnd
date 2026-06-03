package com.lifeforge.domain.model

import java.time.Instant
import java.time.ZoneOffset

/**
 * Espelha o `RecurrenceCalculator` do backend. Usado no app para o PREVIEW
 * ("isso vai gerar X registros entre A e B") ANTES de enviar o schedule.
 * Manter a regra idêntica ao servidor evita preview enganoso.
 *
 *  - ONE_TIME     : 1 ocorrência em startDate.
 *  - INSTALLMENTS : exatamente `installmentsTotal` ocorrências mensais.
 *  - MONTHLY      : mensais de startDate até endDate (se houver), senão até
 *                   hoje + [FUTURE_HORIZON_MONTHS].
 */
object RecurrenceCalculator {

    const val FUTURE_HORIZON_MONTHS = 12L
    private const val MAX_OCCURRENCES = 1200

    fun occurrences(
        recurrence: RecurrenceType,
        startDate: Instant,
        endDate: Instant?,
        installmentsTotal: Int?,
        now: Instant = Instant.now(),
    ): List<Instant> = when (recurrence) {
        RecurrenceType.ONE_TIME -> listOf(startDate)

        RecurrenceType.INSTALLMENTS -> {
            val n = (installmentsTotal ?: 0).coerceIn(0, MAX_OCCURRENCES)
            (0 until n).map { startDate.plusMonthsUtc(it.toLong()) }
        }

        RecurrenceType.MONTHLY -> {
            val end = endDate ?: now.plusMonthsUtc(FUTURE_HORIZON_MONTHS)
            buildList {
                var i = 0L
                while (i < MAX_OCCURRENCES) {
                    val occurrence = startDate.plusMonthsUtc(i)
                    if (occurrence.isAfter(end)) break
                    add(occurrence)
                    i++
                }
            }
        }
    }

    /** Quantos registros o schedule geraria (para o preview). */
    fun count(
        recurrence: RecurrenceType,
        startDate: Instant,
        endDate: Instant?,
        installmentsTotal: Int?,
        now: Instant = Instant.now(),
    ): Int = occurrences(recurrence, startDate, endDate, installmentsTotal, now).size

    private fun Instant.plusMonthsUtc(months: Long): Instant =
        atZone(ZoneOffset.UTC).plusMonths(months).toInstant()
}
