package com.lifeforge.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * Ritmo mensal do painel: cada série recorrente conta uma vez (não N meses) e
 * os pontuais entram pela média recente.
 */
class MonthlyAmountsTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = Instant.parse("2026-10-02T15:00:00Z")

    private fun at(month: YearMonth, day: Int = 10): Instant =
        month.atDay(day).atTime(12, 0).atZone(zone).toInstant()

    private fun entry(text: String, kind: String, amount: String, at: Instant, recurring: Boolean = true) =
        MonthlyAmounts.Entry(MonthlyAmounts.seriesKey(text, kind), BigDecimal(amount), at, recurring)

    /** Os últimos [count] meses até setembro/2026. */
    private fun months(count: Int): List<YearMonth> =
        (count - 1 downTo 0).map { YearMonth.of(2026, 9).minusMonths(it.toLong()) }

    @Test
    fun `aluguel lancado todo mes como recorrente conta uma vez so`() {
        // Regressão: 18 meses de aluguel + plano de saúde somavam 18 × 3.180.
        val entries = months(18).flatMap { m ->
            listOf(
                entry("Aluguel", "HOUSING", "2800", at(m)),
                entry("Plano de saúde", "HEALTH", "380", at(m, 8)),
            )
        }

        assertThat(MonthlyAmounts.recurringTotal(entries, now, zone)).isEqualToIgnoringScale("3180")
    }

    @Test
    fun `usa o valor mais recente da serie`() {
        val entries = listOf(
            entry("Aluguel", "HOUSING", "2500", at(YearMonth.of(2026, 7))),
            entry("Aluguel", "HOUSING", "2500", at(YearMonth.of(2026, 8))),
            entry("Aluguel", "HOUSING", "2800", at(YearMonth.of(2026, 9))),
        )

        assertThat(MonthlyAmounts.recurringTotal(entries, now, zone)).isEqualToIgnoringScale("2800")
    }

    @Test
    fun `descricao com acento, caixa ou espacos diferentes e a mesma serie`() {
        val entries = listOf(
            entry("Plano de saúde", "HEALTH", "380", at(YearMonth.of(2026, 8))),
            entry("  plano de  SAUDE ", "HEALTH", "390", at(YearMonth.of(2026, 9))),
        )

        assertThat(MonthlyAmounts.recurringTotal(entries, now, zone)).isEqualToIgnoringScale("390")
    }

    @Test
    fun `mesma descricao em categorias diferentes sao series distintas`() {
        val entries = listOf(
            entry("Cartão", "FOOD", "300", at(YearMonth.of(2026, 9))),
            entry("Cartão", "LEISURE", "200", at(YearMonth.of(2026, 9))),
        )

        assertThat(MonthlyAmounts.recurringTotal(entries, now, zone)).isEqualToIgnoringScale("500")
    }

    @Test
    fun `serie encerrada ha mais de tres meses deixa de contar`() {
        // Academia de jan a mai/2026; outubro está 5 meses depois do último.
        val gym = (1..5).map { entry("Academia", "HEALTH", "120", at(YearMonth.of(2026, it))) }
        val rent = entry("Aluguel", "HOUSING", "2800", at(YearMonth.of(2026, 9)))

        assertThat(MonthlyAmounts.recurringTotal(gym + rent, now, zone)).isEqualToIgnoringScale("2800")
    }

    @Test
    fun `serie com ultimo lancamento ha tres meses ainda conta`() {
        val entries = listOf(
            entry("Escola", "EDUCATION", "950", at(YearMonth.of(2026, 6))),
            entry("Escola", "EDUCATION", "950", at(YearMonth.of(2026, 7))),
        )

        assertThat(MonthlyAmounts.recurringTotal(entries, now, zone)).isEqualToIgnoringScale("950")
    }

    @Test
    fun `lancamento recorrente unico vale como modelo mensal`() {
        val template = entry("Internet", "HOUSING", "120", at(YearMonth.of(2025, 2)))

        assertThat(MonthlyAmounts.recurringTotal(listOf(template), now, zone)).isEqualToIgnoringScale("120")
    }

    @Test
    fun `ocorrencias de meses futuros nao entram, as do mes corrente sim`() {
        val entries = listOf(
            entry("Aluguel", "HOUSING", "2800", at(YearMonth.of(2026, 9))),
            entry("Aluguel", "HOUSING", "3000", at(YearMonth.of(2026, 10), 10)), // ainda em outubro
            entry("Aluguel", "HOUSING", "9999", at(YearMonth.of(2026, 11))),
        )

        assertThat(MonthlyAmounts.recurringTotal(entries, now, zone)).isEqualToIgnoringScale("3000")
    }

    @Test
    fun `pontuais entram pela media dos tres meses mais recentes com dados`() {
        val entries = listOf(
            entry("Mercado", "FOOD", "900", at(YearMonth.of(2026, 3)), recurring = false),
            entry("Mercado", "FOOD", "1200", at(YearMonth.of(2026, 7)), recurring = false),
            entry("Mercado", "FOOD", "1500", at(YearMonth.of(2026, 8)), recurring = false),
            entry("Farmácia", "HEALTH", "300", at(YearMonth.of(2026, 9)), recurring = false),
            entry("Mercado", "FOOD", "1500", at(YearMonth.of(2026, 9)), recurring = false),
            // agendamento materializado para o futuro: fora da média
            entry("Parcela", "OTHER", "5000", at(YearMonth.of(2027, 1)), recurring = false),
        )

        // (1200 + 1500 + 1800) / 3 = 1500
        assertThat(MonthlyAmounts.recentAverage(entries, now, zone)).isEqualToIgnoringScale("1500")
    }

    @Test
    fun `monthly soma as series recorrentes e a media dos pontuais`() {
        val entries = months(6).flatMap { m ->
            listOf(
                entry("Aluguel", "HOUSING", "2800", at(m)),
                entry("Mercado", "FOOD", "1000", at(m, 15), recurring = false),
            )
        }

        assertThat(MonthlyAmounts.monthly(entries, now, zone)).isEqualToIgnoringScale("3800")
    }

    @Test
    fun `sem lancamentos o ritmo e zero`() {
        assertThat(MonthlyAmounts.monthly(emptyList(), now, zone)).isEqualToIgnoringScale("0")
    }
}
