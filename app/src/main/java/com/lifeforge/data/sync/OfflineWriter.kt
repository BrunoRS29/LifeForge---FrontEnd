package com.lifeforge.data.sync

import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Escrita offline-first comum a metas, receitas, despesas e ativos:
 *
 * 1. grava no banco local e enfileira a operação, numa única transação — a
 *    tela já vê a alteração (fonte única de verdade);
 * 2. se há rede, tenta enviar na hora;
 * 3. sem rede (ou com falha transitória), agenda a sincronização em segundo
 *    plano e devolve o registro local marcado como pendente;
 * 4. se o servidor recusa (validação/conflito), a alteração local é desfeita
 *    e o erro volta à tela, como antes.
 */
@Singleton
class OfflineWriter @Inject constructor(
    private val outbox: Outbox,
    private val engine: SyncEngine,
    private val tx: TransactionRunner,
    private val network: NetworkMonitor,
    private val scheduler: SyncScheduler,
) {

    /**
     * Cria uma entidade. [build] recebe o id temporário (negativo) e devolve a
     * linha local; [load] lê o resultado final (id definitivo, se sincronizou).
     */
    suspend fun <E : SyncableEntity, D> create(
        type: OutboxEntityType,
        store: LocalStore<E>,
        payload: String,
        build: (temporaryId: Long) -> E,
        load: suspend (id: Long) -> D?,
    ): DataResult<D> {
        val (operationId, temporaryId) = tx.run {
            val temporaryId = minOf(
                store.nextTemporaryId(),
                (engine.lowestRemappedTemporaryId(type) ?: 0L) - 1L,
            )
            store.upsert(store.withState(build(temporaryId), SyncState.PENDING_CREATE))
            outbox.enqueueCreate(type, temporaryId, payload) to temporaryId
        }
        return afterWrite(type, operationId, temporaryId, load)
    }

    /**
     * Edita uma entidade. [apply] recebe a linha atual e devolve a nova versão
     * (o estado de sincronização é ajustado aqui).
     */
    suspend fun <E : SyncableEntity, D> update(
        type: OutboxEntityType,
        store: LocalStore<E>,
        id: Long,
        payload: String,
        apply: (current: E) -> E,
        load: suspend (id: Long) -> D?,
    ): DataResult<D> {
        val current = store.findById(id)
            ?: return DataResult.Failure(AppError.NotFound("Registro não encontrado"))
        val operationId = tx.run {
            // Criação ainda pendente continua sendo criação (com os dados novos).
            val newState = if (current.state == SyncState.PENDING_CREATE) {
                SyncState.PENDING_CREATE
            } else {
                SyncState.PENDING_UPDATE
            }
            store.upsert(store.withState(apply(current), newState))
            outbox.enqueueUpdate(type, id, payload)
        }
        return afterWrite(type, operationId, id, load)
    }

    /** Exclui uma entidade (some da tela na hora; o servidor é avisado depois, se preciso). */
    suspend fun <E : SyncableEntity> delete(
        type: OutboxEntityType,
        store: LocalStore<E>,
        id: Long,
    ): DataResult<Unit> {
        store.findById(id) ?: return DataResult.Success(Unit)
        val operationId = tx.run {
            val operationId = outbox.enqueueDelete(type, id)
            // Sem operação = só existia no aparelho: basta apagar a linha.
            if (operationId == null) store.deleteById(id) else store.setSyncState(id, SyncState.PENDING_DELETE)
            operationId
        } ?: return DataResult.Success(Unit)

        return when (val outcome = pushNow(operationId)) {
            is PushOutcome.Rejected -> DataResult.Failure(outcome.error)
            is PushOutcome.Synced -> DataResult.Success(Unit)
            is PushOutcome.Retry, null -> {
                scheduler.requestSync()
                DataResult.Success(Unit)
            }
        }
    }

    private suspend fun <D> afterWrite(
        type: OutboxEntityType,
        operationId: Long,
        localId: Long,
        load: suspend (id: Long) -> D?,
    ): DataResult<D> = when (val outcome = pushNow(operationId)) {
        is PushOutcome.Synced -> load(outcome.serverId).asResult()
        is PushOutcome.Rejected -> DataResult.Failure(outcome.error)
        is PushOutcome.Retry, null -> {
            scheduler.requestSync()
            // A sincronização em segundo plano pode ter enviado a criação entre a
            // gravação e este ponto: nesse caso o registro já tem o id definitivo.
            val currentId = engine.remaps.value[type to localId] ?: localId
            load(currentId).asResult()
        }
    }

    /** Envia agora se houver rede; null = não tentou (offline ou sincronização em curso). */
    private suspend fun pushNow(operationId: Long): PushOutcome? =
        if (network.isOnlineNow()) engine.tryPush(operationId) else null

    private fun <D> D?.asResult(): DataResult<D> =
        if (this != null) {
            DataResult.Success(this)
        } else {
            DataResult.Failure(AppError.Unknown(IllegalStateException("Registro não encontrado após a gravação")))
        }
}
