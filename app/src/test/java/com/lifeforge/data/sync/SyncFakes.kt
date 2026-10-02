package com.lifeforge.data.sync

import com.lifeforge.data.db.dao.GoalDao
import com.lifeforge.data.db.dao.PendingOperationDao
import com.lifeforge.data.db.dao.UserDao
import com.lifeforge.data.db.entity.GoalEntity
import com.lifeforge.data.db.entity.PendingOperationEntity
import com.lifeforge.data.db.entity.UserEntity
import com.lifeforge.data.model.dto.GoalDto
import com.lifeforge.data.model.dto.GoalRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException
import java.time.Instant

/**
 * Dublês em memória para testar a sincronização offline-first na JVM, sem
 * Room nem rede. Os DAOs reproduzem as cláusulas das queries reais que
 * importam para o comportamento (ex.: ocultar exclusões pendentes).
 */

/** Executa o bloco direto — atomicidade não é o que estes testes verificam. */
class DirectTransactionRunner : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R = block()
}

class FakeNetworkMonitor(var online: Boolean = true) : NetworkMonitor {
    override val isOnline: Flow<Boolean> = MutableStateFlow(online)
    override fun isOnlineNow(): Boolean = online
}

class RecordingSyncScheduler : SyncScheduler {
    var syncRequests = 0
    var periodicStarted = 0
    var cancelled = 0
    override fun requestSync() { syncRequests++ }
    override fun startPeriodic() { periodicStarted++ }
    override fun cancelAll() { cancelled++ }
}

class FakeLastSyncStore : LastSyncStore {
    private val state = MutableStateFlow<Instant?>(null)
    override val lastSyncAt: Flow<Instant?> = state
    override suspend fun markSynced(at: Instant) { state.value = at }
    val value: Instant? get() = state.value
}

class FakePendingOperationDao : PendingOperationDao {
    private val rows = MutableStateFlow<List<PendingOperationEntity>>(emptyList())
    private var nextId = 1L

    val snapshot: List<PendingOperationEntity> get() = rows.value

    override suspend fun all() = rows.value.sortedBy { it.id }
    override suspend fun findById(id: Long) = rows.value.firstOrNull { it.id == id }
    override suspend fun findFor(type: String, entityId: Long) =
        rows.value.firstOrNull { it.entityType == type && it.entityId == entityId }

    override suspend fun insert(operation: PendingOperationEntity): Long {
        check(findFor(operation.entityType, operation.entityId) == null) {
            "Índice único (entityType, entityId) violado"
        }
        val id = nextId++
        rows.value = rows.value + operation.copy(id = id)
        return id
    }

    override suspend fun update(operation: PendingOperationEntity) {
        rows.value = rows.value.map { if (it.id == operation.id) operation else it }
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun deleteByType(type: String) {
        rows.value = rows.value.filterNot { it.entityType == type }
    }

    override fun observeCount(): Flow<Int> = rows.map { it.size }
}

class FakeGoalDao : GoalDao {
    private val rows = MutableStateFlow<Map<Long, GoalEntity>>(emptyMap())

    val snapshot: Map<Long, GoalEntity> get() = rows.value

    private fun visible(entity: GoalEntity) = entity.syncState != SyncState.PENDING_DELETE.name

    override fun observeAll(): Flow<List<GoalEntity>> =
        rows.map { all -> all.values.filter(::visible).sortedWith(compareBy({ it.priority }, { it.targetDate })) }

    override fun observeById(id: Long): Flow<GoalEntity?> = rows.map { all -> all[id]?.takeIf(::visible) }
    override suspend fun findById(id: Long) = rows.value[id]
    override suspend fun upsert(entity: GoalEntity) { rows.value = rows.value + (entity.id to entity) }
    override suspend fun upsertAll(entities: List<GoalEntity>) { rows.value = rows.value + entities.associateBy { it.id } }
    override suspend fun deleteById(id: Long) { rows.value = rows.value - id }
    override suspend fun deleteAll() { rows.value = emptyMap() }

    override suspend fun setSyncState(id: Long, state: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(syncState = state)) }
    }

    override suspend fun minId(): Long? = rows.value.keys.minOrNull()
    override suspend fun pendingIds(): List<Long> = rows.value.values.filter { it.syncState != "SYNCED" }.map { it.id }
    override suspend fun deleteSynced() { rows.value = rows.value.filterValues { it.syncState != "SYNCED" } }
}

class FakeUserDao(private val user: UserEntity?) : UserDao {
    override fun observeCurrent(): Flow<UserEntity?> = MutableStateFlow(user)
    override suspend fun findCurrent(): UserEntity? = user
    override suspend fun upsert(entity: UserEntity) = Unit
    override suspend fun deleteAll() = Unit
}

/**
 * API de metas simulada. [mode] decide a resposta de cada chamada; o servidor
 * atribui ids crescentes a partir de 100 e guarda o que recebeu.
 */
class FakeGoalRemote : RemoteCrud<GoalRequestDto, GoalDto> {

    /** LOST_RESPONSE: o servidor processa a criação, mas a resposta se perde no caminho. */
    enum class Mode { OK, OFFLINE, BAD_REQUEST, NOT_FOUND, SERVER_ERROR, LOST_RESPONSE }

    var mode = Mode.OK
    val server = linkedMapOf<Long, GoalDto>()
    val idempotencyKeys = mutableListOf<String>()
    var creates = 0

    /** Gancho executado DURANTE a chamada de criação (simula ação concorrente da tela). */
    var duringCreate: (suspend () -> Unit)? = null

    private var nextId = 100L

    override suspend fun list(): Response<List<GoalDto>> = respond { Response.success(server.values.toList()) }

    override suspend fun create(body: GoalRequestDto, idempotencyKey: String): Response<GoalDto> {
        if (mode == Mode.LOST_RESPONSE) {
            idempotencyKeys += idempotencyKey
            creates++
            throw IOException("timeout depois de o servidor gravar")
        }
        return respond {
            duringCreate?.invoke()
            creates++
            idempotencyKeys += idempotencyKey
            val dto = body.toDto(nextId++)
            server[dto.id] = dto
            Response.success(201, dto)
        }
    }

    override suspend fun update(id: Long, body: GoalRequestDto): Response<GoalDto> = respond {
        if (id !in server) return@respond httpError(404)
        val dto = body.toDto(id)
        server[id] = dto
        Response.success(dto)
    }

    override suspend fun delete(id: Long): Response<Unit> = respond {
        if (server.remove(id) == null) httpError(404) else Response.success(204, Unit)
    }

    private suspend fun <T> respond(block: suspend () -> Response<T>): Response<T> = when (mode) {
        Mode.OFFLINE -> throw IOException("sem rede")
        Mode.BAD_REQUEST -> httpError(400)
        Mode.NOT_FOUND -> httpError(404)
        Mode.SERVER_ERROR -> httpError(503)
        Mode.OK, Mode.LOST_RESPONSE -> block()
    }

    private fun <T> httpError(code: Int): Response<T> = Response.error(
        code,
        """{"error":"X","message":"erro $code"}""".toResponseBody("application/json".toMediaType()),
    )

    private fun GoalRequestDto.toDto(id: Long) = GoalDto(
        id = id,
        userId = 1L,
        name = name,
        category = category,
        targetAmount = targetAmount,
        targetDate = targetDate,
        priority = priority,
        createdAt = "2026-01-01T00:00:00Z",
    )
}
