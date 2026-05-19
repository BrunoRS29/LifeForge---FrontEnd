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
 * estrutura de operações (observeAll/upsert/delete/replaceAll), apenas
 * com tabelas e ordenação distintas.
 */

@Dao
interface IncomeDao {

    @Query("SELECT * FROM incomes ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<IncomeEntity>>

    @Upsert
    suspend fun upsert(entity: IncomeEntity)

    @Upsert
    suspend fun upsertAll(entities: List<IncomeEntity>)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM incomes")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<IncomeEntity>) {
        deleteAll()
        upsertAll(entities)
    }
}

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY spentAt DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Upsert
    suspend fun upsert(entity: ExpenseEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ExpenseEntity>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<ExpenseEntity>) {
        deleteAll()
        upsertAll(entities)
    }
}

@Dao
interface AssetDao {

    @Query("SELECT * FROM assets ORDER BY name ASC")
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

    @Transaction
    suspend fun replaceAll(entities: List<AssetEntity>) {
        deleteAll()
        upsertAll(entities)
    }
}
