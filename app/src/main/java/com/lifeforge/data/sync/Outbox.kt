package com.lifeforge.data.sync

import com.lifeforge.data.db.dao.PendingOperationDao
import com.lifeforge.data.db.entity.PendingOperationEntity
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fila de saída (outbox) com consolidação: guarda no máximo UMA operação por
 * entidade, de modo que o servidor recebe apenas o estado final de cada
 * registro, na ordem em que o usuário trabalhou.
 *
 * | Pendente | Nova operação | Fica na fila                                 |
 * |----------|---------------|----------------------------------------------|
 * | —        | CREATE        | CREATE                                       |
 * | —        | UPDATE        | UPDATE                                       |
 * | —        | DELETE        | DELETE                                       |
 * | CREATE   | UPDATE        | CREATE com os dados novos                    |
 * | CREATE   | DELETE        | nada — a entidade nunca chegou ao servidor   |
 * | UPDATE   | UPDATE        | UPDATE com os dados novos                    |
 * | UPDATE   | DELETE        | DELETE                                       |
 */
@Singleton
class Outbox @Inject constructor(
    private val dao: PendingOperationDao,
    private val clock: Clock,
) {

    /** Registra a criação offline de uma entidade com id temporário. */
    suspend fun enqueueCreate(type: OutboxEntityType, localId: Long, payload: String): Long =
        dao.insert(
            PendingOperationEntity(
                entityType = type.name,
                entityId = localId,
                operation = OutboxOperation.CREATE.name,
                payload = payload,
                createdAt = Instant.now(clock),
            )
        )

    /**
     * Registra uma edição. Se a criação da entidade ainda está pendente, apenas
     * substitui os dados que serão enviados nela.
     *
     * @return id da operação que levará a edição ao servidor.
     */
    suspend fun enqueueUpdate(type: OutboxEntityType, entityId: Long, payload: String): Long {
        val existing = dao.findFor(type.name, entityId)
        return if (existing != null) {
            dao.update(existing.copy(payload = payload, attempts = 0, lastError = null))
            existing.id
        } else {
            dao.insert(
                PendingOperationEntity(
                    entityType = type.name,
                    entityId = entityId,
                    operation = OutboxOperation.UPDATE.name,
                    payload = payload,
                    createdAt = Instant.now(clock),
                )
            )
        }
    }

    /**
     * Registra uma exclusão.
     *
     * @return id da operação a enviar, ou null quando a entidade só existia no
     *   aparelho (a criação pendente é descartada e não há o que enviar).
     */
    suspend fun enqueueDelete(type: OutboxEntityType, entityId: Long): Long? {
        val existing = dao.findFor(type.name, entityId)
        return when (existing?.operation?.let(OutboxOperation::valueOf)) {
            OutboxOperation.CREATE -> {
                dao.delete(existing.id)
                null
            }
            OutboxOperation.UPDATE, OutboxOperation.DELETE -> {
                dao.update(
                    existing.copy(
                        operation = OutboxOperation.DELETE.name,
                        payload = null,
                        attempts = 0,
                        lastError = null,
                    )
                )
                existing.id
            }
            null -> dao.insert(
                PendingOperationEntity(
                    entityType = type.name,
                    entityId = entityId,
                    operation = OutboxOperation.DELETE.name,
                    payload = null,
                    createdAt = Instant.now(clock),
                )
            )
        }
    }

    /** Operações pendentes em ordem de criação (FIFO). */
    suspend fun pending(): List<PendingOperationEntity> = dao.all()

    suspend fun find(operationId: Long): PendingOperationEntity? = dao.findById(operationId)

    /** Remove da fila uma operação concluída (ou descartada). */
    suspend fun complete(operationId: Long) = dao.delete(operationId)

    /** Substitui uma operação (ex.: reapontar para o id definitivo). */
    suspend fun replace(operation: PendingOperationEntity) = dao.update(operation)

    /** Registra uma tentativa sem sucesso (falha transitória). */
    suspend fun recordFailure(operationId: Long, message: String?) {
        val op = dao.findById(operationId) ?: return
        dao.update(op.copy(attempts = op.attempts + 1, lastError = message))
    }

    /** Descarta as pendências de um tipo (ex.: exclusão em lote confirmada no servidor). */
    suspend fun discardAll(type: OutboxEntityType) = dao.deleteByType(type.name)

    fun observeCount(): Flow<Int> = dao.observeCount()
}
