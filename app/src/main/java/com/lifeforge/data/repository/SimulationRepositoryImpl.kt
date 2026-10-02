package com.lifeforge.data.repository

import com.lifeforge.data.api.SimulationApi
import com.lifeforge.data.db.dao.GoalDao
import com.lifeforge.data.db.dao.SimulationDao
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.mapper.toEntity
import com.lifeforge.data.mapper.toRequestDto
import com.lifeforge.data.mapper.toSummaryEntity
import com.lifeforge.data.sync.CacheRefresher
import com.lifeforge.data.sync.TransactionRunner
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.SimulationParameters
import com.lifeforge.domain.model.SimulationResult
import com.lifeforge.domain.model.SimulationSummary
import com.lifeforge.domain.model.mapCatching
import com.lifeforge.domain.repository.SimulationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do repositório de simulações.
 *
 * Simulações têm semântica de compute-on-demand: cada `run()` executa o motor
 * no backend e o resultado é imutável depois de gravado. Executar exige
 * conexão; CONSULTAR não:
 *
 * - `run()`: API → cache do resultado completo (com as bandas do fan chart) e
 *   do resumo no histórico da meta.
 * - `getById()`: cache local primeiro; sem cache, busca na API e guarda.
 * - `observeByGoal()` / `observeLatestByGoal()`: Flow do histórico resumido —
 *   alimenta a lista de simulações e a "saúde" de cada meta, mesmo offline.
 * - `refreshByGoal()` / [refresh]: substituem o histórico local pelo do servidor
 *   (inclui simulações feitas em outro aparelho e as calibradas por IA).
 */
@Singleton
class SimulationRepositoryImpl @Inject constructor(
    private val api: SimulationApi,
    private val dao: SimulationDao,
    private val goalDao: GoalDao,
    private val tx: TransactionRunner,
    private val json: Json,
) : SimulationRepository, CacheRefresher {

    override suspend fun run(parameters: SimulationParameters): DataResult<SimulationResult> =
        safeApiCall(json) { api.run(parameters.toRequestDto()) }
            .mapCatching { response ->
                tx.run {
                    dao.upsert(response.toEntity())
                    dao.upsertSummary(response.toSummaryEntity())
                }
                response.toDomain()
            }

    override suspend fun getById(id: Long): DataResult<SimulationResult> {
        val cached = dao.findById(id)
        // Cache completo (com o fan chart): reabre sem custo de rede.
        if (cached != null && cached.trajectory.isNotEmpty()) return DataResult.Success(cached.toDomain())
        val remote = safeApiCall(json) { api.getById(id) }
            .mapCatching { response ->
                dao.upsert(response.toEntity())
                response.toDomain()
            }
        // Sem rede, um cache antigo (sem bandas) ainda é melhor que nada.
        return if (remote is DataResult.Failure && cached != null) DataResult.Success(cached.toDomain()) else remote
    }

    override fun observeByGoal(goalId: Long): Flow<List<SimulationSummary>> =
        dao.observeSummariesByGoal(goalId).map { entities -> entities.map { it.toDomain() } }

    override fun observeLatestByGoal(): Flow<Map<Long, SimulationSummary>> =
        dao.observeAllSummaries().map { entities ->
            // A lista vem da mais recente para a mais antiga: a primeira de cada meta é a última rodada.
            entities.groupBy { it.goalId }.mapValues { (_, sims) -> sims.first().toDomain() }
        }

    override suspend fun refreshByGoal(goalId: Long): DataResult<Unit> {
        if (goalId <= 0) return DataResult.Success(Unit) // meta ainda não sincronizada
        return safeApiCall(json) { api.listByGoal(goalId) }
            .mapCatching { dtos -> dao.replaceSummariesForGoal(goalId, dtos.map { it.toEntity() }) }
    }

    override suspend fun refreshAllHistories(): DataResult<Unit> = refresh()

    /** Atualiza o histórico de todas as metas sincronizadas (ciclo de sincronização). */
    override suspend fun refresh(): DataResult<Unit> {
        val goalIds = goalDao.observeAll().first().map { it.id }.filter { it > 0 }
        for (goalId in goalIds) {
            val result = refreshByGoal(goalId)
            if (result is DataResult.Failure) return result
        }
        return DataResult.Success(Unit)
    }

    override suspend fun delete(id: Long): DataResult<Unit> =
        safeApiCall(json) { api.delete(id) }
            .mapCatching {
                tx.run {
                    dao.deleteById(id)
                    dao.deleteSummaryById(id)
                }
            }
}
