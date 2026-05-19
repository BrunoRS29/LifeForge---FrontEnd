package com.lifeforge.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.lifeforge.data.db.entity.SimulationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO de simulações Monte Carlo.
 *
 * Diferente dos outros DAOs CRUD, simulações são imutáveis depois de
 * gravadas — não há `update`. Apenas insert e delete.
 */
@Dao
interface SimulationDao {

    @Query("SELECT * FROM simulations WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun observeByGoal(goalId: Long): Flow<List<SimulationEntity>>

    @Query("SELECT * FROM simulations WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): SimulationEntity?

    @Upsert
    suspend fun upsert(entity: SimulationEntity)

    @Upsert
    suspend fun upsertAll(entities: List<SimulationEntity>)

    @Query("DELETE FROM simulations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM simulations WHERE goalId = :goalId")
    suspend fun deleteByGoal(goalId: Long)

    @Query("DELETE FROM simulations")
    suspend fun deleteAll()

    /** Substitui as simulações de uma meta específica (usado em refreshByGoal). */
    @Transaction
    suspend fun replaceForGoal(goalId: Long, entities: List<SimulationEntity>) {
        deleteByGoal(goalId)
        upsertAll(entities)
    }
}
