package com.lifeforge.data.sync

import com.lifeforge.data.db.entity.PendingOperationEntity
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Registro da última sincronização completa (persistido entre sessões). */
interface LastSyncStore {
    val lastSyncAt: Flow<Instant?>
    suspend fun markSynced(at: Instant)
}

/**
 * Coração do offline-first: leva a fila de saída ao servidor e atualiza o
 * cache local a partir dele.
 *
 * - **Ordem:** as operações sobem na ordem em que foram feitas (FIFO); a
 *   primeira falha transitória interrompe o envio (a rede caiu — as seguintes
 *   falhariam igual).
 * - **Exclusão mútua:** um [Mutex] impede que o worker e uma escrita da tela
 *   enviem a mesma operação ao mesmo tempo (o que duplicaria registros).
 * - **Corridas com a tela:** se o usuário edita uma entidade enquanto a
 *   criação dela está em trânsito, a operação é reapontada para o id
 *   definitivo como edição; se a exclui, a exclusão do registro recém-criado
 *   é enfileirada.
 * - **Ids temporários:** criações offline usam ids negativos; ao sincronizar,
 *   [resolvedId] permite que telas abertas sigam para o id definitivo.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val outbox: Outbox,
    handlers: Set<@JvmSuppressWildcards EntitySyncHandler>,
    private val extraRefreshers: Set<@JvmSuppressWildcards CacheRefresher>,
    private val tx: TransactionRunner,
    private val lastSync: LastSyncStore,
    private val clock: Clock,
) {
    private val handlersByType = handlers.associateBy { it.type }

    /** Handlers em ordem estável: metas primeiro (o histórico de simulações depende delas). */
    private val orderedHandlers = OutboxEntityType.entries.mapNotNull { handlersByType[it] }

    private val mutex = Mutex()

    private val idRemaps = MutableStateFlow<Map<Pair<OutboxEntityType, Long>, Long>>(emptyMap())

    /** Remapeamentos id temporário → id definitivo feitos nesta execução do app. */
    val remaps: StateFlow<Map<Pair<OutboxEntityType, Long>, Long>> = idRemaps.asStateFlow()

    /**
     * Menor id temporário deste tipo já remapeado nesta execução. Novos ids
     * temporários ficam sempre abaixo dele: reutilizar um id remapeado faria a
     * tela "seguir" o registro antigo para o id definitivo errado.
     */
    fun lowestRemappedTemporaryId(type: OutboxEntityType): Long? =
        idRemaps.value.keys.filter { it.first == type }.minOfOrNull { it.second }

    /** Id atual de uma entidade: segue o remapeamento quando uma criação offline sincroniza. */
    fun resolvedId(type: OutboxEntityType, id: Long): Flow<Long> =
        idRemaps.map { it[type to id] ?: id }.distinctUntilChanged()

    /**
     * Tenta enviar uma operação logo após a escrita local. Não espera: se uma
     * sincronização já está em curso, devolve null e a operação sobe com ela.
     */
    suspend fun tryPush(operationId: Long): PushOutcome? {
        if (!mutex.tryLock()) return null
        return try {
            outbox.find(operationId)?.let { pushLocked(it) }
        } finally {
            mutex.unlock()
        }
    }

    /** Envia toda a fila. Falha na primeira operação que não pôde subir por motivo transitório. */
    suspend fun pushAll(): DataResult<Unit> = mutex.withLock { pushAllLocked() }

    /** Ciclo completo: envia a fila e atualiza todo o cache a partir do servidor. */
    suspend fun syncAll(): DataResult<Unit> {
        val pushed = pushAll()
        if (pushed is DataResult.Failure) return pushed
        val refreshers: List<CacheRefresher> = orderedHandlers + extraRefreshers
        for (refresher in refreshers) {
            val result = refresher.refresh()
            if (result is DataResult.Failure) return result
        }
        lastSync.markSynced(Instant.now(clock))
        return DataResult.Success(Unit)
    }

    /** Atualiza o cache de um único tipo, enviando antes as pendências (puxar para atualizar). */
    suspend fun refresh(type: OutboxEntityType): DataResult<Unit> {
        val pushed = pushAll()
        if (pushed is DataResult.Failure) return pushed
        val handler = handlersByType[type] ?: return DataResult.Success(Unit)
        return handler.refresh()
    }

    private suspend fun pushAllLocked(): DataResult<Unit> {
        for (operation in outbox.pending()) {
            val outcome = pushLocked(operation)
            if (outcome is PushOutcome.Retry) return DataResult.Failure(outcome.error)
        }
        return DataResult.Success(Unit)
    }

    private suspend fun pushLocked(operation: PendingOperationEntity): PushOutcome {
        val type = OutboxEntityType.valueOf(operation.entityType)
        val handler = handlersByType[type]
            ?: return PushOutcome.Rejected(AppError.Unknown(IllegalStateException("Sem handler para $type")))

        val outcome = handler.push(operation)
        when (outcome) {
            is PushOutcome.Synced -> tx.run { settle(operation, outcome.serverId, handler, type) }
            is PushOutcome.Rejected -> tx.run {
                handler.revert(operation, outcome.error)
                outbox.complete(operation.id)
            }
            is PushOutcome.Retry -> outbox.recordFailure(operation.id, outcome.error.message)
        }
        return outcome
    }

    /** Conclui uma operação sincronizada, tratando alterações feitas durante o envio. */
    private suspend fun settle(
        sent: PendingOperationEntity,
        serverId: Long,
        handler: EntitySyncHandler,
        type: OutboxEntityType,
    ) {
        val wasCreate = sent.operation == OutboxOperation.CREATE.name
        if (wasCreate && serverId != sent.entityId) {
            idRemaps.update { it + ((type to sent.entityId) to serverId) }
        }
        val current = outbox.find(sent.id)
        when {
            // Excluída pela tela enquanto a criação subia: remove também no servidor.
            wasCreate && current == null -> {
                outbox.enqueueDelete(type, serverId)
                handler.markPending(serverId, SyncState.PENDING_DELETE)
            }
            // Editada durante o envio: a operação passa a ser a edição do registro definitivo.
            current != null && current.payload != sent.payload &&
                current.operation != OutboxOperation.DELETE.name -> {
                outbox.replace(
                    current.copy(
                        entityId = serverId,
                        operation = OutboxOperation.UPDATE.name,
                        attempts = 0,
                        lastError = null,
                    )
                )
                handler.markPending(serverId, SyncState.PENDING_UPDATE)
            }
            else -> outbox.complete(sent.id)
        }
    }
}
