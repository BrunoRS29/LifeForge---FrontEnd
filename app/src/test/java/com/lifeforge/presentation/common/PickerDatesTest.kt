package com.lifeforge.presentation.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Conversão entre o DatePicker (meia-noite UTC do dia escolhido) e as datas
 * exibidas no fuso de São Paulo. Regressão: escolher 15/03 exibia 14/03.
 */
class PickerDatesTest {

    @Test
    fun `data escolhida no DatePicker e exibida no mesmo dia`() {
        val picked = Instant.parse("2030-03-15T00:00:00Z").toEpochMilli()

        assertThat(formatDate(pickerMillisToInstant(picked))).isEqualTo("15/03/2030")
    }

    @Test
    fun `ida e volta preserva o dia escolhido`() {
        val picked = Instant.parse("2027-01-01T00:00:00Z").toEpochMilli()

        assertThat(instantToPickerMillis(pickerMillisToInstant(picked))).isEqualTo(picked)
    }

    @Test
    fun `DatePicker abre no dia de Sao Paulo, nao no de Greenwich`() {
        // 31/12 às 22h em São Paulo = 01/01 à 1h UTC.
        val lateNight = Instant.parse("2027-01-01T01:00:00Z")

        assertThat(instantToPickerMillis(lateNight))
            .isEqualTo(Instant.parse("2026-12-31T00:00:00Z").toEpochMilli())
    }
}
