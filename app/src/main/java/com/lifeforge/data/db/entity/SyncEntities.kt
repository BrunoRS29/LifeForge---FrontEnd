package com.lifeforge.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Fila de saída (outbox) do padrão offline-first: cada linha é uma alteração
 * feita no aparelho que ainda precisa ser enviada à API.
 *
 * - [entityType] / [operation]: nomes de
 *   [com.lifeforge.data.sync.OutboxEntityType] e
 *   [com.lifeforge.data.sync.OutboxOperation].
 * - [entityId]: id da linha local afetada (negativo para criações offline).
 * - [payload]: corpo JSON do request de criação/edição; nulo na exclusão.
 *
 * A fila guarda no máximo UMA operação por entidade — as alterações
 * sucessivas são consolidadas ao enfileirar (ver [com.lifeforge.data.sync.Outbox]).
 */
@Entity(
    tableName = "pending_operations",
    indices = [Index(value = ["entityType", "entityId"], unique = true)],
)
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: Long,
    val operation: String,
    val payload: String?,
    val createdAt: Instant,
    val attempts: Int = 0,
    val lastError: String? = null,
)

/**
 * Resumo de uma simulação de Monte Carlo (histórico por meta). Mais leve que
 * [SimulationEntity] — sem histograma nem bandas —, o que permite cachear o
 * histórico inteiro devolvido por `GET /simulation/by-goal/{id}` e mostrar a
 * "saúde" de cada meta mesmo sem conexão.
 */
@Entity(
    tableName = "simulation_summaries",
    indices = [Index("goalId")],
)
data class SimulationSummaryEntity(
    @PrimaryKey val id: Long,
    val goalId: Long,
    val successProbability: Double,
    val mean: Double,
    val median: Double,
    val targetAmount: Double,
    val createdAt: Instant,
)
