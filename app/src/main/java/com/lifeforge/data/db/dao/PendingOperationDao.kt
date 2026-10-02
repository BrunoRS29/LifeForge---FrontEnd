package com.lifeforge.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lifeforge.data.db.entity.PendingOperationEntity
import kotlinx.coroutines.flow.Flow

/** Acesso à fila de saída (outbox) da sincronização offline-first. */
@Dao
interface PendingOperationDao {

    /** Operações na ordem em que foram feitas (FIFO). */
    @Query("SELECT * FROM pending_operations ORDER BY id ASC")
    suspend fun all(): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): PendingOperationEntity?

    @Query("SELECT * FROM pending_operations WHERE entityType = :type AND entityId = :entityId LIMIT 1")
    suspend fun findFor(type: String, entityId: Long): PendingOperationEntity?

    @Insert
    suspend fun insert(operation: PendingOperationEntity): Long

    @Update
    suspend fun update(operation: PendingOperationEntity)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_operations WHERE entityType = :type")
    suspend fun deleteByType(type: String)

    @Query("SELECT COUNT(*) FROM pending_operations")
    fun observeCount(): Flow<Int>
}
