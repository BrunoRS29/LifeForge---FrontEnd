package com.lifeforge.domain.model

import java.math.BigDecimal
import java.text.Normalizer
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * Detecta lançamentos RECORRENTES no histórico (importado ou manual).
 *
 * Heurística: agrupa transações por uma "assinatura" da descrição (sem
 * números/datas/pontuação, primeiras palavras significativas) e considera
 * recorrente o grupo que aparece em pelo menos [MIN_MONTHS] meses distintos.
 * O valor mensal típico é a mediana das somas mensais do grupo — robusta a
 * meses atípicos.
 *
 * Pura e testável — sem dependência de Android/rede.
 */
data class RecurringPattern(
    val label: String,
    val monthlyAmount: BigDecimal,  // valor típico por mês (mediana)
    val months: Int,                // em quantos meses distintos apareceu
    val tag: String,                // tipo de renda ou categoria de despesa (para exibir)
)

object RecurrenceDetector {

    private const val MIN_MONTHS = 3
    private val ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")

    fun detectIncome(incomes: List<Income>): List<RecurringPattern> =
        detect(incomes.map { Item(it.source, it.amount, it.receivedAt, it.incomeType.name) })

    fun detectExpense(expenses: List<Expense>): List<RecurringPattern> =
        detect(expenses.map { Item(it.description, it.amount, it.spentAt, it.category.name) })

    private data class Item(
        val description: String,
        val amount: BigDecimal,
        val at: Instant,
        val tag: String,
    )

    private fun detect(items: List<Item>): List<RecurringPattern> {
        if (items.isEmpty()) return emptyList()

        return items
            .groupBy { signature(it.description) }
            .values
            .mapNotNull { group ->
                val byMonth = group.groupBy { YearMonth.from(it.at.atZone(ZONE)) }
                if (byMonth.size < MIN_MONTHS) return@mapNotNull null

                val monthlySums = byMonth.values
                    .map { list -> list.fold(BigDecimal.ZERO) { acc, i -> acc + i.amount } }
                    .sorted()
                val median = monthlySums[monthlySums.size / 2]

                // Descrição representativa = a mais longa do grupo (mais informativa).
                val label = group.maxByOrNull { it.description.length }?.description
                    ?: group.first().description

                RecurringPattern(
                    label = label.trim().take(60),
                    monthlyAmount = median,
                    months = byMonth.size,
                    tag = mostCommonTag(group),
                )
            }
            .sortedByDescending { it.monthlyAmount }
    }

    private fun mostCommonTag(group: List<Item>): String =
        group.groupingBy { it.tag }.eachCount().maxByOrNull { it.value }?.key
            ?: group.first().tag

    /** Assinatura estável da descrição: sem números, pontuação, e só as
     *  primeiras palavras significativas — junta "PIX X 10/02" e "PIX X 26/01". */
    private fun signature(description: String): String {
        val noAccents = Normalizer
            .normalize(description.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return noAccents
            .replace(Regex("[0-9]+"), " ")
            .replace(Regex("[^a-z ]"), " ")
            .split(" ")
            .filter { it.length >= 3 }
            .take(6)
            .joinToString(" ")
    }
}
