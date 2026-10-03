package com.lifeforge.presentation.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.Month

/** Cabeçalhos de dia das listas de lançamentos e nomes curtos de mês. */
class DayHeaderTest {

    private val today = LocalDate.of(2026, 10, 2) // sexta-feira

    @Test
    fun `hoje e ontem por extenso`() {
        assertThat(formatDayHeader(today, today)).isEqualTo("Hoje")
        assertThat(formatDayHeader(today.minusDays(1), today)).isEqualTo("Ontem")
    }

    @Test
    fun `outros dias com dia da semana e mes`() {
        assertThat(formatDayHeader(LocalDate.of(2026, 9, 28), today)).isEqualTo("Segunda, 28 de setembro")
        assertThat(formatDayHeader(LocalDate.of(2026, 9, 26), today)).isEqualTo("Sábado, 26 de setembro")
    }

    @Test
    fun `ano aparece so fora do ano corrente`() {
        assertThat(formatDayHeader(LocalDate.of(2025, 12, 31), today)).isEqualTo("Quarta, 31 de dezembro de 2025")
    }

    @Test
    fun `mes curto sem ponto e com maiuscula`() {
        assertThat(formatMonthShort(Month.JANUARY)).isEqualTo("Jan")
        assertThat(formatMonthShort(Month.MARCH)).isEqualTo("Mar")
        assertThat(formatMonthShort(Month.DECEMBER)).isEqualTo("Dez")
    }
}
