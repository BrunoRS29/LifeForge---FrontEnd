package com.lifeforge.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.lifeforge.data.db.entity.SimulationEntity
import com.lifeforge.data.db.entity.SimulationSummaryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO de simulações Monte Carlo.
 *
 * Duas tabelas:
 * - `simulations`: resultado completo (histograma, percentis) das simulações
 *   abertas no aparelho — cache para reabrir sem custo de rede;
 * - `simulation_summaries`: o histórico resumido de TODAS as simulações de
 *   cada meta, como devolvido pelo servidor — alimenta a lista de histórico e
 *   a "saúde" das metas mesmo sem conexão.
 *
 * Simulações são imutáveis depois de gravadas — não há `update`.
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

    /** Substitui as simulações de uma meta específica. */
    @Transaction
    suspend fun replaceForGoal(goalId: Long, entities: List<SimulationEntity>) {
        deleteByGoal(goalId)
        upsertAll(entities)
    }

    // ------------------------------------------------------------------------
    // Resumos (histórico por meta)
    // ------------------------------------------------------------------------

    @Query("SELECT * FROM simulation_summaries WHERE goalId = :goalId ORDER BY createdAt DESC, id DESC")
    fun observeSummariesByGoal(goalId: Long): Flow<List<SimulationSummaryEntity>>

    /** Todos os resumos, do mais recente ao mais antigo (o mais recente de cada meta vem primeiro). */
    @Query("SELECT * FROM simulation_summaries ORDER BY createdAt DESC, id DESC")
    fun observeAllSummaries(): Flow<List<SimulationSummaryEntity>>

    @Upsert
    suspend fun upsertSummary(entity: SimulationSummaryEntity)

    @Upsert
    suspend fun upsertSummaries(entities: List<SimulationSummaryEntity>)

    @Query("DELETE FROM simulation_summaries WHERE id = :id")
    suspend fun deleteSummaryById(id: Long)

    @Query("DELETE FROM simulation_summaries WHERE goalId = :goalId")
    suspend fun deleteSummariesByGoal(goalId: Long)

    /** Substitui o histórico de uma meta pela lista do servidor. */
    @Transaction
    suspend fun replaceSummariesForGoal(goalId: Long, entities: List<SimulationSummaryEntity>) {
        deleteSummariesByGoal(goalId)
        upsertSummaries(entities)
    }
}
