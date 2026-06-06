package com.lifeforge.presentation.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Formatadores para a UI em PT-BR.
 *
 * `Locale("pt", "BR")` em vez do extinto `Locale.forLanguageTag("pt-BR")`
 * para max compatibilidade com APIs antigas. Os formatadores são thread-safe
 * para `NumberFormat` (instância nova a cada chamada — pequeno overhead
 * mas evita armadilhas com `DecimalFormat`).
 */

private val ptBR: Locale = Locale("pt", "BR")
private val zoneBR: ZoneId = ZoneId.of("America/Sao_Paulo")
private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBR)
private val dayMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM", ptBR)
private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", ptBR)

/**
 * Formata BigDecimal como moeda brasileira: `R$ 1.234.567,89`.
 *
 * Aceita BigDecimal direto (preferido para valores monetários) ou
 * Double (para valores vindos da engine Monte Carlo).
 */
fun formatBrl(amount: BigDecimal): String =
    NumberFormat.getCurrencyInstance(ptBR).format(amount)

fun formatBrl(amount: Double): String =
    NumberFormat.getCurrencyInstance(ptBR).format(amount)

/**
 * Versão compacta para valores grandes em cards do dashboard:
 * `R$ 1,5 mi`, `R$ 850,3 mil`. Útil quando o espaço é limitado.
 */
fun formatBrlCompact(amount: BigDecimal): String {
    val abs = amount.abs()
    val sign = if (amount.signum() < 0) "-" else ""
    return when {
        abs >= BigDecimal("1000000000") ->
            "${sign}R\$ ${formatNumber(abs.divide(BigDecimal("1000000000"), 1, RoundingMode.HALF_UP))} bi"
        abs >= BigDecimal("1000000") ->
            "${sign}R\$ ${formatNumber(abs.divide(BigDecimal("1000000"), 1, RoundingMode.HALF_UP))} mi"
        abs >= BigDecimal("1000") ->
            "${sign}R\$ ${formatNumber(abs.divide(BigDecimal("1000"), 1, RoundingMode.HALF_UP))} mil"
        else -> formatBrl(amount)
    }
}

private fun formatNumber(value: BigDecimal): String =
    NumberFormat.getNumberInstance(ptBR).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(value)

/** Formata BigDecimal como percentual: `85,4%`. Espera valor em % (ex.: 85.4 = 85,4%). */
fun formatPercent(value: BigDecimal): String =
    NumberFormat.getNumberInstance(ptBR).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(value) + "%"

/** Versão Double — para `successProbability` da Simulation (0.0..1.0). */
fun formatProbability(probability: Double): String =
    NumberFormat.getPercentInstance(ptBR).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(probability)

/** `dd/MM/yyyy` no fuso de São Paulo. */
fun formatDate(instant: Instant): String =
    dateFormatter.format(instant.atZone(zoneBR))

/** `dd/MM` — versão curta para listas já filtradas por mês. */
fun formatDayMonth(instant: Instant): String =
    dayMonthFormatter.format(instant.atZone(zoneBR))

/** `dd/MM/yyyy às HH:mm` — usado em históricos/timestamps de simulação. */
fun formatDateTime(instant: Instant): String =
    dateTimeFormatter.format(instant.atZone(zoneBR))

/** [YearMonth] (no fuso de SP) de um instante — para agrupar/filtrar por mês. */
fun yearMonthOf(instant: Instant): YearMonth =
    YearMonth.from(instant.atZone(zoneBR))

/** Rótulo do mês: `Junho 2026` (primeira letra maiúscula). */
fun formatMonthYear(yearMonth: YearMonth): String {
    val month = yearMonth.month.getDisplayName(TextStyle.FULL, ptBR)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ptBR) else it.toString() }
    return "$month ${yearMonth.year}"
}

/** Instante representando o dia 1 do mês (meio-dia SP) — default de data ao criar no mês. */
fun firstInstantOfMonth(yearMonth: YearMonth): Instant =
    yearMonth.atDay(1).atTime(12, 0).atZone(zoneBR).toInstant()
