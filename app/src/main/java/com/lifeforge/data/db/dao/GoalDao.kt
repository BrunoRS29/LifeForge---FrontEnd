package com.lifeforge.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.lifeforge.data.db.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO das metas. As leituras observadas pela UI ocultam as linhas com exclusão
 * pendente (a exclusão já "aconteceu" para o usuário, só não subiu ainda).
 */
@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE syncState != 'PENDING_DELETE' ORDER BY priority ASC, targetDate ASC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id AND syncState != 'PENDING_DELETE' LIMIT 1")
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

    @Query("UPDATE goals SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: Long, state: String)

    /** Menor id da tabela — base para gerar ids temporários (negativos). */
    @Query("SELECT MIN(id) FROM goals")
    suspend fun minId(): Long?

    @Query("SELECT id FROM goals WHERE syncState != 'SYNCED'")
    suspend fun pendingIds(): List<Long>

    @Query("DELETE FROM goals WHERE syncState = 'SYNCED'")
    suspend fun deleteSynced()

    /**
     * Substitui o cache pela lista canônica do servidor (remove o que foi
     * apagado em outro dispositivo) SEM descartar o que ainda não subiu: linhas
     * com alteração pendente são preservadas e prevalecem sobre a versão do
     * servidor até a sincronização.
     */
    @Transaction
    suspend fun replaceAllPreservingPending(entities: List<GoalEntity>) {
        val pending = pendingIds().toSet()
        deleteSynced()
        upsertAll(entities.filter { it.id !in pending })
    }
}
