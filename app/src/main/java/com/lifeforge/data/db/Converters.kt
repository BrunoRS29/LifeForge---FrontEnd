package com.lifeforge.data.db

import androidx.room.TypeConverter
import com.lifeforge.data.model.dto.HistogramBucketDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant

/**
 * Type converters compartilhados pelo Room para serializar tipos
 * não-primitivos em colunas SQLite.
 *
 * Decisões:
 * - **BigDecimal → String** (`toPlainString`): preserva precisão sem
 *   notação científica. Nunca usamos Double no banco para valores
 *   monetários — perda de precisão em soma de aportes ao longo de anos
 *   é inaceitável.
 * - **Instant → Long** (epoch milli): comparação numérica eficiente em
 *   ORDER BY. `Long.MIN_VALUE` é livre porque Instant nunca chega lá.
 * - **Map/List → JSON String**: campos compostos da [SimulationEntity]
 *   (`percentiles` e `histogram`) são gravados como JSON. Tradeoff:
 *   não dá para filtrar por percentil em SQL, mas isso nunca é
 *   necessário — sempre lemos a Simulation inteira.
 *
 * Enums NÃO são convertidos aqui — guardamos como String em colunas
 * dedicadas e fazemos o `valueOf()` no mapper Entity → Domain. Mantém
 * o schema legível em ferramentas de debug e evita acoplar Room aos
 * enums de domínio.
 */
class Converters {

    /** Json dedicado aos converters — mesma config que o NetworkModule mas instância separada. */
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    // ------------------------------------------------------------------------
    // BigDecimal
    // ------------------------------------------------------------------------

    @TypeConverter
    fun bigDecimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    // ------------------------------------------------------------------------
    // Instant
    // ------------------------------------------------------------------------

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    // ------------------------------------------------------------------------
    // Map<String, Double> — usado em SimulationEntity.percentiles
    // ------------------------------------------------------------------------

    @TypeConverter
    fun percentilesToJson(value: Map<String, Double>?): String? =
        value?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToPercentiles(value: String?): Map<String, Double>? =
        value?.let { json.decodeFromString<Map<String, Double>>(it) }

    // ------------------------------------------------------------------------
    // List<HistogramBucketDto> — usado em SimulationEntity.histogram
    // Reaproveita o DTO (já @Serializable) para evitar criar
    // HistogramBucketEntity duplicado.
    // ------------------------------------------------------------------------

    @TypeConverter
    fun histogramToJson(value: List<HistogramBucketDto>?): String? =
        value?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToHistogram(value: String?): List<HistogramBucketDto>? =
        value?.let { json.decodeFromString<List<HistogramBucketDto>>(it) }
}
