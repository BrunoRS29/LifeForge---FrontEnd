package com.lifeforge.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * Valores mensais do painel (renda e despesa do mês) a partir dos lançamentos.
 *
 * - **Recorrentes** (marcados como tal pelo usuário ou pela importação): cada
 *   SÉRIE conta uma única vez. Lançamentos com a mesma descrição (normalizada:
 *   sem acentos, caixa ou espaços extras) e a mesma categoria formam uma série,
 *   cujo valor mensal é o do lançamento mais recente. Assim, quem lança o
 *   aluguel todo mês marcando "recorrente" não vê a despesa multiplicada pelo
 *   número de meses. Uma série com vários meses deixa de contar se não aparece
 *   há mais de [ACTIVE_MONTHS] meses (foi encerrada); um lançamento recorrente
 *   único vale como modelo mensal enquanto existir.
 * - **Pontuais**: média mensal dos [RECENT_MONTHS] meses mais recentes com dados.
 *
 * Em ambos os casos só entram lançamentos até o fim do mês corrente: as
 * ocorrências futuras que os agendamentos materializam (até 12 meses à frente)
 * não inflam o mês nem deslocam a média para meses que ainda não aconteceram.
 */
object MonthlyAmounts {

    const val ACTIVE_MONTHS = 3L
    const val RECENT_MONTHS = 3

    data class Entry(
        /** Identifica a série: descrição + categoria/tipo (ver [seriesKey]). */
        val key: String,
        val amount: BigDecimal,
        val at: Instant,
        val recurring: Boolean,
    )

    /** Recorrentes (uma vez por série ativa) + média recente dos pontuais. */
    fun monthly(entries: List<Entry>, now: Instant, zone: ZoneId): BigDecimal =
        recurringTotal(entries, now, zone) + recentAverage(entries.filterNot { it.recurring }, now, zone)

    fun recurringTotal(entries: List<Entry>, now: Instant, zone: ZoneId): BigDecimal {
        val currentMonth = YearMonth.from(now.atZone(zone))
        val cutoff = endOfMonth(currentMonth, zone)
        return entries
            .filter { it.recurring && it.at.isBefore(cutoff) }
            .groupBy { it.key }
            .values
            .mapNotNull { series ->
                val latest = series.maxBy { it.at }
                val lastMonth = YearMonth.from(latest.at.atZone(zone))
                val active = series.size == 1 || !lastMonth.plusMonths(ACTIVE_MONTHS).isBefore(currentMonth)
                latest.amount.takeIf { active }
            }
            .fold(BigDecimal.ZERO) { acc, amount -> acc + amount }
    }

    /**
     * Média mensal sobre os [months] meses mais recentes COM dados (até o fim
     * do mês corrente). Ignora meses vazios: um histórico com lacunas não dilui
     * o valor.
     */
    fun recentAverage(entries: List<Entry>, now: Instant, zone: ZoneId, months: Int = RECENT_MONTHS): BigDecimal {
        val cutoff = endOfMonth(YearMonth.from(now.atZone(zone)), zone)
        val byMonth = entries
            .filter { it.at.isBefore(cutoff) }
            .groupBy { YearMonth.from(it.at.atZone(zone)) }
            .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount } }
        val recent = byMonth.keys.sortedDescending().take(months)
        if (recent.isEmpty()) return BigDecimal.ZERO
        val total = recent.fold(BigDecimal.ZERO) { acc, month -> acc + byMonth.getValue(month) }
        return total.divide(BigDecimal(recent.size), 2, RoundingMode.HALF_UP)
    }

    /** Primeiro instante do mês seguinte (limite exclusivo). */
    private fun endOfMonth(month: YearMonth, zone: ZoneId): Instant =
        month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()

    /** Chave da série: "Aluguel " e "aluguel" (HOUSING) são a mesma série. */
    fun seriesKey(text: String, kind: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
        return "$kind|$normalized"
    }
}
