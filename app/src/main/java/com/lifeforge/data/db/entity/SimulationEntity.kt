package com.lifeforge.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifeforge.data.model.dto.HistogramBucketDto
import java.time.Instant

/**
 * Resultado completo de uma simulação Monte Carlo persistido localmente.
 *
 * Campos compostos:
 * - [percentiles]: serializado como JSON (chaves "P5", "P10", ..., "P95").
 * - [histogram]: serializado como JSON. Reaproveita [HistogramBucketDto]
 *   (já @Serializable) para evitar criar uma entidade-bucket duplicada
 *   apenas para o Room.
 *
 * Os valores monetários ficam como Double (não BigDecimal) porque a
 * engine Monte Carlo opera em Double — converter para BigDecimal aqui
 * apenas adicionaria conversões sem ganho de precisão (a precisão já
 * foi perdida na simulação).
 *
 * Index em `goalId` garante que `observeByGoal` é O(log n).
 */
@Entity(
    tableName = "simulations",
    indices = [Index("goalId")],
)
data class SimulationEntity(
    @PrimaryKey val id: Long,
    val goalId: Long,
    val numSimulations: Int,
    val seed: Long,
    val targetAmount: Double,
    val successProbability: Double,
    val mean: Double,
    val median: Double,
    val standardDeviation: Double,
    val percentiles: Map<String, Double>,
    val worstCase: Double,
    val bestCase: Double,
    val meanReal: Double,
    val histogram: List<HistogramBucketDto>,
    val executionTimeMs: Long,
    val createdAt: Instant,
)
