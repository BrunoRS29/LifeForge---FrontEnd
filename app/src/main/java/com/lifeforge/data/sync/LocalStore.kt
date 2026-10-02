package com.lifeforge.data.sync

import com.lifeforge.data.db.dao.AssetDao
import com.lifeforge.data.db.dao.ExpenseDao
import com.lifeforge.data.db.dao.GoalDao
import com.lifeforge.data.db.dao.IncomeDao
import com.lifeforge.data.db.entity.AssetEntity
import com.lifeforge.data.db.entity.ExpenseEntity
import com.lifeforge.data.db.entity.GoalEntity
import com.lifeforge.data.db.entity.IncomeEntity

/** Linha do banco local editável sem conexão. */
interface SyncableEntity {
    val id: Long
    val syncState: String
}

/**
 * Operações locais de que a sincronização precisa sobre a tabela de uma
 * entidade. Uma implementação por DAO; nos testes, implementações em memória.
 */
interface LocalStore<E : SyncableEntity> {
    suspend fun findById(id: Long): E?
    suspend fun upsert(entity: E)
    suspend fun deleteById(id: Long)
    suspend fun setSyncState(id: Long, state: SyncState)
    suspend fun minId(): Long?
    suspend fun replaceAllPreservingPending(entities: List<E>)

    /** Cópia da entidade com outro estado de sincronização. */
    fun withState(entity: E, state: SyncState): E
}

/** Próximo id temporário (negativo e único na tabela) para uma criação offline. */
suspend fun LocalStore<*>.nextTemporaryId(): Long = minOf(minId() ?: 0L, 0L) - 1L

/** Estado de sincronização tipado de uma linha. */
val SyncableEntity.state: SyncState
    get() = runCatching { SyncState.valueOf(syncState) }.getOrDefault(SyncState.SYNCED)

// ============================================================================
// Adaptadores DAO → LocalStore
// ============================================================================

fun GoalDao.asLocalStore(): LocalStore<GoalEntity> = object : LocalStore<GoalEntity> {
    override suspend fun findById(id: Long) = this@asLocalStore.findById(id)
    override suspend fun upsert(entity: GoalEntity) = this@asLocalStore.upsert(entity)
    override suspend fun deleteById(id: Long) = this@asLocalStore.deleteById(id)
    override suspend fun setSyncState(id: Long, state: SyncState) = this@asLocalStore.setSyncState(id, state.name)
    override suspend fun minId() = this@asLocalStore.minId()
    override suspend fun replaceAllPreservingPending(entities: List<GoalEntity>) =
        this@asLocalStore.replaceAllPreservingPending(entities)
    override fun withState(entity: GoalEntity, state: SyncState) = entity.copy(syncState = state.name)
}

fun IncomeDao.asLocalStore(): LocalStore<IncomeEntity> = object : LocalStore<IncomeEntity> {
    override suspend fun findById(id: Long) = this@asLocalStore.findById(id)
    override suspend fun upsert(entity: IncomeEntity) = this@asLocalStore.upsert(entity)
    override suspend fun deleteById(id: Long) = this@asLocalStore.deleteById(id)
    override suspend fun setSyncState(id: Long, state: SyncState) = this@asLocalStore.setSyncState(id, state.name)
    override suspend fun minId() = this@asLocalStore.minId()
    override suspend fun replaceAllPreservingPending(entities: List<IncomeEntity>) =
        this@asLocalStore.replaceAllPreservingPending(entities)
    override fun withState(entity: IncomeEntity, state: SyncState) = entity.copy(syncState = state.name)
}

fun ExpenseDao.asLocalStore(): LocalStore<ExpenseEntity> = object : LocalStore<ExpenseEntity> {
    override suspend fun findById(id: Long) = this@asLocalStore.findById(id)
    override suspend fun upsert(entity: ExpenseEntity) = this@asLocalStore.upsert(entity)
    override suspend fun deleteById(id: Long) = this@asLocalStore.deleteById(id)
    override suspend fun setSyncState(id: Long, state: SyncState) = this@asLocalStore.setSyncState(id, state.name)
    override suspend fun minId() = this@asLocalStore.minId()
    override suspend fun replaceAllPreservingPending(entities: List<ExpenseEntity>) =
        this@asLocalStore.replaceAllPreservingPending(entities)
    override fun withState(entity: ExpenseEntity, state: SyncState) = entity.copy(syncState = state.name)
}

fun AssetDao.asLocalStore(): LocalStore<AssetEntity> = object : LocalStore<AssetEntity> {
    override suspend fun findById(id: Long) = this@asLocalStore.findById(id)
    override suspend fun upsert(entity: AssetEntity) = this@asLocalStore.upsert(entity)
    override suspend fun deleteById(id: Long) = this@asLocalStore.deleteById(id)
    override suspend fun setSyncState(id: Long, state: SyncState) = this@asLocalStore.setSyncState(id, state.name)
    override suspend fun minId() = this@asLocalStore.minId()
    override suspend fun replaceAllPreservingPending(entities: List<AssetEntity>) =
        this@asLocalStore.replaceAllPreservingPending(entities)
    override fun withState(entity: AssetEntity, state: SyncState) = entity.copy(syncState = state.name)
}
