package com.lifeforge.data.repository

import com.lifeforge.data.api.GoalApi
import com.lifeforge.data.db.dao.GoalDao
import com.lifeforge.data.mapper.goalRequestDto
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.mapper.toEntity
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.domain.model.mapCatching
import com.lifeforge.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação canônica do padrão **offline-first** do LifeForge.
 *
 * Contrato:
 * - **Read** (`observeAll`, `observeById`): sempre Flow do Room. A UI
 *   nunca espera por rede — a tela renderiza com cache imediatamente.
 * - **Refresh** (`refresh`): chama API e substitui o conteúdo local
 *   numa transaction. Flow auto-emite o novo estado.
 * - **Create/Update/Delete**: write-through — chama API primeiro
 *   (porque o backend assina IDs e timestamps), depois grava no Room.
 *   Em falha de rede, retorna [DataResult.Failure] e o Room fica
 *   intacto. Suporte a "pending writes" entra em sprint posterior.
 *
 * Os outros 4 RepositoryImpl seguem este mesmo template.
 */
@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalApi: GoalApi,
    private val goalDao: GoalDao,
    private val json: Json,
) : GoalRepository {

    // ------------------------------------------------------------------------
    // Read — Flow puro do Room
    // ------------------------------------------------------------------------

    override fun observeAll(): Flow<List<Goal>> =
        goalDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Goal?> =
        goalDao.observeById(id).map { it?.toDomain() }

    // ------------------------------------------------------------------------
    // Refresh — API → DB.replaceAll
    // ------------------------------------------------------------------------

    override suspend fun refresh(): DataResult<Unit> =
        safeApiCall(json) { goalApi.list() }
            .mapCatching { dtos ->
                goalDao.replaceAll(dtos.map { it.toEntity() })
            }

    // ------------------------------------------------------------------------
    // Mutations — write-through
    // ------------------------------------------------------------------------

    override suspend fun create(
        name: String,
        category: GoalCategory,
        targetAmount: BigDecimal,
        targetDate: Instant,
        priority: Int,
    ): DataResult<Goal> = safeApiCall(json) {
        goalApi.create(goalRequestDto(name, category, targetAmount, targetDate, priority))
    }.mapCatching { dto ->
        val entity = dto.toEntity()
        goalDao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun update(
        id: Long,
        name: String,
        category: GoalCategory,
        targetAmount: BigDecimal,
        targetDate: Instant,
        priority: Int,
    ): DataResult<Goal> = safeApiCall(json) {
        goalApi.update(id, goalRequestDto(name, category, targetAmount, targetDate, priority))
    }.mapCatching { dto ->
        val entity = dto.toEntity()
        goalDao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun delete(id: Long): DataResult<Unit> =
        safeApiCall(json) { goalApi.delete(id) }
            .mapCatching { goalDao.deleteById(id) }
}
