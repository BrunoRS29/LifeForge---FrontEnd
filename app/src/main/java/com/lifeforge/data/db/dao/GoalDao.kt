package com.lifeforge.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.lifeforge.data.db.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY priority ASC, targetDate ASC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): GoalEntity?

    @Upsert
    suspend fun upsert(entity: GoalEntity)

    @Upsert
    suspend fun upsertAll(entities: List<GoalEntity>)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()

    /**
     * Substitui todo o conteúdo de `goals` num único transaction.
     *
     * Usado por `GoalRepositoryImpl.refresh()` quando a API retorna a
     * lista canônica do servidor — garantimos que o Room reflete
     * exatamente o backend, removendo metas que foram apagadas em
     * outro dispositivo.
     */
    @Transaction
    suspend fun replaceAll(entities: List<GoalEntity>) {
        deleteAll()
        upsertAll(entities)
    }
}
