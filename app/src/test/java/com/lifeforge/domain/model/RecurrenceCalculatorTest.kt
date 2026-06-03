package com.lifeforge.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/**
 * Testa a lógica de preview ("Isso vai gerar X registros") — o MESMO cálculo
 * usado pelo backend ao materializar. Garante que o número mostrado ao
 * usuário no form bate com o que o servidor vai criar.
 */
class RecurrenceCalculatorTest {

    private fun plusMonths(i: Long, from: Instant): Instant =
        from.atZone(ZoneOffset.UTC).plusMonths(i).toInstant()

    @Test
    fun `ONE_TIME gera 1`() {
        val n = RecurrenceCalculator.count(
            RecurrenceType.ONE_TIME, Instant.parse("2024-05-10T00:00:00Z"), null, null,
        )
        assertThat(n).isEqualTo(1)
    }

    @Test
    fun `INSTALLMENTS gera exatamente N`() {
        val n = RecurrenceCalculator.count(
            RecurrenceType.INSTALLMENTS, Instant.parse("2025-03-01T00:00:00Z"), null, 12,
        )
        assertThat(n).isEqualTo(12)
    }

    @Test
    fun `INSTALLMENTS sem total gera 0 (preview vazio ate informar parcelas)`() {
        val n = RecurrenceCalculator.count(
            RecurrenceType.INSTALLMENTS, Instant.parse("2025-01-01T00:00:00Z"), null, null,
        )
        assertThat(n).isEqualTo(0)
    }

    @Test
    fun `MONTHLY com endDate conta inclusive ate o fim`() {
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val end = Instant.parse("2024-06-01T00:00:00Z")
        val n = RecurrenceCalculator.count(RecurrenceType.MONTHLY, start, end, null)
        assertThat(n).isEqualTo(6) // jan..jun
    }

    @Test
    fun `MONTHLY indefinido cobre passado mais 12 meses futuros`() {
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val start = plusMonths(-24, now)
        val occurrences = RecurrenceCalculator.occurrences(
            RecurrenceType.MONTHLY, start, null, null, now,
        )
        assertThat(occurrences.size).isEqualTo(37) // 24 passados + atual + 12 futuros
        assertThat(occurrences.first()).isEqualTo(start)
        assertThat(occurrences.any { it.isAfter(now) }).isTrue()
    }
}
