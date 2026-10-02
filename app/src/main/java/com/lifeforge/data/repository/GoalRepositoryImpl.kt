package com.lifeforge.data.repository

import com.lifeforge.data.db.dao.GoalDao
import com.lifeforge.data.db.dao.UserDao
import com.lifeforge.data.db.entity.GoalEntity
import com.lifeforge.data.mapper.goalRequestDto
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.model.dto.GoalRequestDto
import com.lifeforge.data.sync.OfflineWriter
import com.lifeforge.data.sync.OutboxEntityType
import com.lifeforge.data.sync.SyncEngine
import com.lifeforge.data.sync.asLocalStore
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação canônica do padrão **offline-first** do LifeForge.
 *
 * Contrato:
 * - **Leitura** (`observeAll`, `observeById`): sempre Flow do Room — a tela
 *   renderiza o cache imediatamente e nunca espera pela rede.
 * - **Escrita** (`create`/`update`/`delete`): grava no Room e na fila de saída
 *   numa única transação e tenta enviar na hora ([OfflineWriter]). Sem
 *   conexão, a alteração fica marcada como pendente e o `SyncWorker` a envia
 *   quando a rede voltar; recusa do servidor desfaz a alteração local.
 * - **Refresh**: envia as pendências e substitui o cache pela lista do
 *   servidor, preservando o que ainda não subiu.
 *
 * Os repositórios de receitas, despesas e ativos seguem este mesmo template.
 */
@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val userDao: UserDao,
    private val writer: OfflineWriter,
    private val engine: SyncEngine,
    private val json: Json,
    private val clock: Clock,
) : GoalRepository {

    private val store = goalDao.asLocalStore()

    // ------------------------------------------------------------------------
    // Leitura — Flow puro do Room
    // ------------------------------------------------------------------------

    override fun observeAll(): Flow<List<Goal>> =
        goalDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    /**
     * Segue o id definitivo quando uma meta criada offline é sincronizada com a
     * tela aberta. A linha temporária some no instante da sincronização (antes
     * de o remapeamento chegar): esse "null" transitório não é propagado.
     */
    override fun observeById(id: Long): Flow<Goal?> =
        engine.resolvedId(OutboxEntityType.GOAL, id)
            .flatMapLatest { currentId ->
                val rows = goalDao.observeById(currentId)
                if (currentId < 0) rows.filterNotNull() else rows
            }
            .map { it?.toDomain() }

    override suspend fun refresh(): DataResult<Unit> = engine.refresh(OutboxEntityType.GOAL)

    // ------------------------------------------------------------------------
    // Escrita — local primeiro, servidor em seguida (ou depois, sem rede)
    // ------------------------------------------------------------------------

    override suspend fun create(
        name: String,
        category: GoalCategory,
        targetAmount: BigDecimal,
        targetDate: Instant,
        priority: Int,
    ): DataResult<Goal> {
        val userId = userDao.findCurrent()?.id ?: 0L
        return writer.create(
            type = OutboxEntityType.GOAL,
            store = store,
            payload = encode(goalRequestDto(name, category, targetAmount, targetDate, priority)),
            build = { temporaryId ->
                GoalEntity(
                    id = temporaryId,
                    userId = userId,
                    name = name,
                    category = category.name,
                    targetAmount = targetAmount,
                    targetDate = targetDate,
                    priority = priority,
                    createdAt = Instant.now(clock),
                )
            },
            load = { id -> goalDao.findById(id)?.toDomain() },
        )
    }

    override suspend fun update(
        id: Long,
        name: String,
        category: GoalCategory,
        targetAmount: BigDecimal,
        targetDate: Instant,
        priority: Int,
    ): DataResult<Goal> = writer.update(
        type = OutboxEntityType.GOAL,
        store = store,
        id = id,
        payload = encode(goalRequestDto(name, category, targetAmount, targetDate, priority)),
        apply = { current ->
            current.copy(
                name = name,
                category = category.name,
                targetAmount = targetAmount,
                targetDate = targetDate,
                priority = priority,
            )
        },
        load = { goalDao.findById(it)?.toDomain() },
    )

    override suspend fun delete(id: Long): DataResult<Unit> =
        writer.delete(OutboxEntityType.GOAL, store, id)

    private fun encode(request: GoalRequestDto): String =
        json.encodeToString(GoalRequestDto.serializer(), request)
}
