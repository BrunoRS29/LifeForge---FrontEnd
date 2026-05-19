package com.lifeforge.data.db

import com.google.common.truth.Truth.assertThat
import com.lifeforge.data.model.dto.HistogramBucketDto
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * Garante que os converters preservam dados sem perda — especialmente
 * crítico para BigDecimal (precisão monetária) e os campos compostos
 * da SimulationEntity.
 */
class ConvertersTest {

    private val converters = Converters()

    // ------------------------------------------------------------------------
    // BigDecimal — preservação de precisão
    // ------------------------------------------------------------------------

    @Test
    fun `BigDecimal com muitas casas decimais sobrevive ao round-trip`() {
        val original = BigDecimal("1234567.890123456789")

        val str = converters.bigDecimalToString(original)
        val recovered = converters.stringToBigDecimal(str)

        assertThat(recovered).isEqualTo(original)
        // toPlainString garante ausência de notação científica
        assertThat(str).doesNotContain("E")
    }

    @Test
    fun `BigDecimal preserva escala original`() {
        // Valor monetário típico — 2 casas decimais devem ser preservadas
        val original = BigDecimal("100.00")

        val recovered = converters.stringToBigDecimal(
            converters.bigDecimalToString(original)
        )

        // Tanto o valor quanto a escala devem coincidir; "100.00" != "100"
        // em comparação por compareTo, mas equals checa a escala também.
        assertThat(recovered).isEqualTo(original)
        assertThat(recovered?.scale()).isEqualTo(2)
    }

    @Test
    fun `BigDecimal null e tratado corretamente`() {
        assertThat(converters.bigDecimalToString(null)).isNull()
        assertThat(converters.stringToBigDecimal(null)).isNull()
    }

    // ------------------------------------------------------------------------
    // Instant — round-trip via epoch milli
    // ------------------------------------------------------------------------

    @Test
    fun `Instant sobrevive ao round-trip`() {
        val original = Instant.parse("2026-05-09T12:34:56Z")

        val recovered = converters.longToInstant(
            converters.instantToLong(original)
        )

        assertThat(recovered).isEqualTo(original)
    }

    @Test
    fun `Instant null e tratado corretamente`() {
        assertThat(converters.instantToLong(null)).isNull()
        assertThat(converters.longToInstant(null)).isNull()
    }

    // ------------------------------------------------------------------------
    // Map<String, Double> — usado em SimulationEntity.percentiles
    // ------------------------------------------------------------------------

    @Test
    fun `Map de percentiles round-trip preserva chaves e valores`() {
        val original = mapOf(
            "P5" to 12_500.0,
            "P10" to 25_000.0,
            "P50" to 100_000.0,
            "P90" to 350_000.0,
            "P95" to 500_000.0,
        )

        val json = converters.percentilesToJson(original)
        val recovered = converters.jsonToPercentiles(json)

        assertThat(recovered).isEqualTo(original)
    }

    @Test
    fun `Map de percentiles vazio sobrevive ao round-trip`() {
        val recovered = converters.jsonToPercentiles(
            converters.percentilesToJson(emptyMap())
        )

        assertThat(recovered).isEmpty()
    }

    // ------------------------------------------------------------------------
    // List<HistogramBucketDto> — usado em SimulationEntity.histogram
    // ------------------------------------------------------------------------

    @Test
    fun `List de HistogramBucketDto round-trip preserva todos os campos`() {
        val original = listOf(
            HistogramBucketDto(rangeStart = 0.0, rangeEnd = 50_000.0, count = 1_234),
            HistogramBucketDto(rangeStart = 50_000.0, rangeEnd = 100_000.0, count = 5_678),
            HistogramBucketDto(rangeStart = 100_000.0, rangeEnd = 200_000.0, count = 3_088),
        )

        val json = converters.histogramToJson(original)
        val recovered = converters.jsonToHistogram(json)

        assertThat(recovered).isEqualTo(original)
        assertThat(recovered).hasSize(3)
    }

    @Test
    fun `List de buckets vazia sobrevive ao round-trip`() {
        val recovered = converters.jsonToHistogram(
            converters.histogramToJson(emptyList())
        )

        assertThat(recovered).isEmpty()
    }
}
