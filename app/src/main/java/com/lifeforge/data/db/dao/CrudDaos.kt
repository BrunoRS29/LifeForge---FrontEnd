package com.lifeforge.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.lifeforge.data.db.entity.AssetEntity
import com.lifeforge.data.db.entity.ExpenseEntity
import com.lifeforge.data.db.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAOs CRUD agrupados — IncomeDao, ExpenseDao e AssetDao têm a mesma
 * estrutura de operações, apenas com tabelas e ordenação distintas.
 *
 * Convenção offline-first (igual à de [GoalDao]): as leituras observadas
 * ocultam linhas com exclusão pendente, e o refresh preserva as linhas que
 * ainda não subiram para o servidor.
 */

@Dao
interface IncomeDao {

    @Query("SELECT * FROM incomes WHERE syncState != 'PENDING_DELETE' ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): IncomeEntity?

    @Upsert
    suspend fun upsert(entity: IncomeEntity)

    @Upsert
    suspend fun upsertAll(entities: List<IncomeEntity>)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM incomes")
    suspend fun deleteAll()

    @Query("UPDATE incomes SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: Long, state: String)

    @Query("SELECT MIN(id) FROM incomes")
    suspend fun minId(): Long?

    @Query("SELECT id FROM incomes WHERE syncState != 'SYNCED'")
    suspend fun pendingIds(): List<Long>

    @Query("DELETE FROM incomes WHERE syncState = 'SYNCED'")
    suspend fun deleteSynced()

    @Transaction
    suspend fun replaceAllPreservingPending(entities: List<IncomeEntity>) {
        val pending = pendingIds().toSet()
        deleteSynced()
        upsertAll(entities.filter { it.id !in pending })
    }
}

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE syncState != 'PENDING_DELETE' ORDER BY spentAt DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ExpenseEntity?

    @Upsert
    suspend fun upsert(entity: ExpenseEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ExpenseEntity>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Query("UPDATE expenses SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: Long, state: String)

    @Query("SELECT MIN(id) FROM expenses")
    suspend fun minId(): Long?

    @Query("SELECT id FROM expenses WHERE syncState != 'SYNCED'")
    suspend fun pendingIds(): List<Long>

    @Query("DELETE FROM expenses WHERE syncState = 'SYNCED'")
    suspend fun deleteSynced()

    @Transaction
    suspend fun replaceAllPreservingPending(entities: List<ExpenseEntity>) {
        val pending = pendingIds().toSet()
        deleteSynced()
        upsertAll(entities.filter { it.id !in pending })
    }
}

@Dao
interface AssetDao {

    @Query("SELECT * FROM assets WHERE syncState != 'PENDING_DELETE' ORDER BY name ASC")
    fun observeAll(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): AssetEntity?

    @Upsert
    suspend fun upsert(entity: AssetEntity)

    @Upsert
    suspend fun upsertAll(entities: List<AssetEntity>)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM assets")
    suspend fun deleteAll()

    @Query("UPDATE assets SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: Long, state: String)

    @Query("SELECT MIN(id) FROM assets")
    suspend fun minId(): Long?

    @Query("SELECT id FROM assets WHERE syncState != 'SYNCED'")
    suspend fun pendingIds(): List<Long>

    @Query("DELETE FROM assets WHERE syncState = 'SYNCED'")
    suspend fun deleteSynced()

    @Transaction
    suspend fun replaceAllPreservingPending(entities: List<AssetEntity>) {
        val pending = pendingIds().toSet()
        deleteSynced()
        upsertAll(entities.filter { it.id !in pending })
    }
}
