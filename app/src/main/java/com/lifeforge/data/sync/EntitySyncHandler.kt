package com.lifeforge.data.sync

import com.lifeforge.data.api.AssetApi
import com.lifeforge.data.api.ExpenseApi
import com.lifeforge.data.api.GoalApi
import com.lifeforge.data.api.IncomeApi
import com.lifeforge.data.db.entity.PendingOperationEntity
import com.lifeforge.data.model.dto.AssetDto
import com.lifeforge.data.model.dto.AssetRequestDto
import com.lifeforge.data.model.dto.ExpenseDto
import com.lifeforge.data.model.dto.ExpenseRequestDto
import com.lifeforge.data.model.dto.GoalDto
import com.lifeforge.data.model.dto.GoalRequestDto
import com.lifeforge.data.model.dto.IncomeDto
import com.lifeforge.data.model.dto.IncomeRequestDto
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.mapCatching
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import retrofit2.Response

/** Algo que sabe atualizar uma parte do cache local a partir da API. */
interface CacheRefresher {
    suspend fun refresh(): DataResult<Unit>
}

/**
 * Sincronização de um tipo de entidade: envia as operações pendentes dele e
 * atualiza o cache a partir do servidor.
 */
interface EntitySyncHandler : CacheRefresher {

    val type: OutboxEntityType

    /** Envia uma operação; quando aplicada no servidor, reflete o resultado no banco local. */
    suspend fun push(operation: PendingOperationEntity): PushOutcome

    /** Desfaz no banco local uma operação rejeitada pelo servidor. */
    suspend fun revert(operation: PendingOperationEntity, error: AppError)

    /** Marca uma linha como pendente (ex.: editada enquanto a criação era enviada). */
    suspend fun markPending(id: Long, state: SyncState)
}

/** Endpoints CRUD de uma entidade na API. */
interface RemoteCrud<Req, Dto> {
    suspend fun list(): Response<List<Dto>>
    suspend fun create(body: Req, idempotencyKey: String): Response<Dto>
    suspend fun update(id: Long, body: Req): Response<Dto>
    suspend fun delete(id: Long): Response<Unit>
}

/**
 * Implementação única para metas, receitas, despesas e ativos — muda apenas o
 * que é injetado (tabela local, endpoints, serializador do corpo e mapeador).
 *
 * Criações levam um cabeçalho `Idempotency-Key` estável por operação: se a
 * resposta se perder (timeout depois de o servidor gravar), o reenvio não
 * duplica o registro.
 */
class RemoteEntitySyncHandler<E : SyncableEntity, Req, Dto>(
    override val type: OutboxEntityType,
    private val store: LocalStore<E>,
    private val remote: RemoteCrud<Req, Dto>,
    private val requestSerializer: KSerializer<Req>,
    private val toEntity: (Dto) -> E,
    private val json: Json,
    private val tx: TransactionRunner,
) : EntitySyncHandler {

    override suspend fun push(operation: PendingOperationEntity): PushOutcome =
        when (OutboxOperation.valueOf(operation.operation)) {
            OutboxOperation.CREATE -> {
                val body = decode(operation)
                when (val result = safeApiCall(json) { remote.create(body, idempotencyKeyOf(operation)) }) {
                    is DataResult.Success -> {
                        val entity = toEntity(result.data)
                        // Troca a linha temporária (id negativo) pela definitiva.
                        tx.run {
                            store.deleteById(operation.entityId)
                            store.upsert(entity)
                        }
                        PushOutcome.Synced(entity.id)
                    }
                    is DataResult.Failure -> result.error.toPushOutcome()
                }
            }
            OutboxOperation.UPDATE -> {
                when (val result = safeApiCall(json) { remote.update(operation.entityId, decode(operation)) }) {
                    is DataResult.Success -> {
                        val entity = toEntity(result.data)
                        store.upsert(entity)
                        PushOutcome.Synced(entity.id)
                    }
                    is DataResult.Failure -> result.error.toPushOutcome()
                }
            }
            OutboxOperation.DELETE -> {
                when (val result = safeApiCall(json) { remote.delete(operation.entityId) }) {
                    is DataResult.Success -> {
                        store.deleteById(operation.entityId)
                        PushOutcome.Synced(operation.entityId)
                    }
                    // Já não existe no servidor: o objetivo da exclusão foi atingido.
                    is DataResult.Failure -> if (result.error is AppError.NotFound) {
                        store.deleteById(operation.entityId)
                        PushOutcome.Synced(operation.entityId)
                    } else {
                        result.error.toPushOutcome()
                    }
                }
            }
        }

    override suspend fun revert(operation: PendingOperationEntity, error: AppError) {
        when (OutboxOperation.valueOf(operation.operation)) {
            // Criação recusada: o registro local não deve existir.
            OutboxOperation.CREATE -> store.deleteById(operation.entityId)
            // Edição recusada: some se o servidor não tem mais o registro; senão a
            // próxima atualização do cache restaura a versão do servidor.
            OutboxOperation.UPDATE ->
                if (error is AppError.NotFound) {
                    store.deleteById(operation.entityId)
                } else {
                    store.setSyncState(operation.entityId, SyncState.SYNCED)
                }
            // Exclusão recusada: a linha volta a aparecer.
            OutboxOperation.DELETE -> store.setSyncState(operation.entityId, SyncState.SYNCED)
        }
    }

    override suspend fun markPending(id: Long, state: SyncState) = store.setSyncState(id, state)

    override suspend fun refresh(): DataResult<Unit> =
        safeApiCall(json) { remote.list() }
            .mapCatching { dtos -> store.replaceAllPreservingPending(dtos.map(toEntity)) }

    private fun decode(operation: PendingOperationEntity): Req =
        json.decodeFromString(requestSerializer, requireNotNull(operation.payload) {
            "Operação ${operation.operation} sem corpo na fila"
        })

    companion object {
        /** Chave estável por operação: id local da fila + instante em que foi criada. */
        fun idempotencyKeyOf(operation: PendingOperationEntity): String =
            "lifeforge-${operation.entityType.lowercase()}-${operation.id}-${operation.createdAt.toEpochMilli()}"
    }
}

// ============================================================================
// Adaptadores Retrofit → RemoteCrud
// ============================================================================

fun GoalApi.asRemoteCrud(): RemoteCrud<GoalRequestDto, GoalDto> = object : RemoteCrud<GoalRequestDto, GoalDto> {
    override suspend fun list() = this@asRemoteCrud.list()
    override suspend fun create(body: GoalRequestDto, idempotencyKey: String) = this@asRemoteCrud.create(body, idempotencyKey)
    override suspend fun update(id: Long, body: GoalRequestDto) = this@asRemoteCrud.update(id, body)
    override suspend fun delete(id: Long) = this@asRemoteCrud.delete(id)
}

fun IncomeApi.asRemoteCrud(): RemoteCrud<IncomeRequestDto, IncomeDto> = object : RemoteCrud<IncomeRequestDto, IncomeDto> {
    override suspend fun list() = this@asRemoteCrud.list()
    override suspend fun create(body: IncomeRequestDto, idempotencyKey: String) = this@asRemoteCrud.create(body, idempotencyKey)
    override suspend fun update(id: Long, body: IncomeRequestDto) = this@asRemoteCrud.update(id, body)
    override suspend fun delete(id: Long) = this@asRemoteCrud.delete(id)
}

fun ExpenseApi.asRemoteCrud(): RemoteCrud<ExpenseRequestDto, ExpenseDto> = object : RemoteCrud<ExpenseRequestDto, ExpenseDto> {
    override suspend fun list() = this@asRemoteCrud.list()
    override suspend fun create(body: ExpenseRequestDto, idempotencyKey: String) = this@asRemoteCrud.create(body, idempotencyKey)
    override suspend fun update(id: Long, body: ExpenseRequestDto) = this@asRemoteCrud.update(id, body)
    override suspend fun delete(id: Long) = this@asRemoteCrud.delete(id)
}

fun AssetApi.asRemoteCrud(): RemoteCrud<AssetRequestDto, AssetDto> = object : RemoteCrud<AssetRequestDto, AssetDto> {
    override suspend fun list() = this@asRemoteCrud.list()
    override suspend fun create(body: AssetRequestDto, idempotencyKey: String) = this@asRemoteCrud.create(body, idempotencyKey)
    override suspend fun update(id: Long, body: AssetRequestDto) = this@asRemoteCrud.update(id, body)
    override suspend fun delete(id: Long) = this@asRemoteCrud.delete(id)
}
