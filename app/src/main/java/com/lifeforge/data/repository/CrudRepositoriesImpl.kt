package com.lifeforge.data.repository

import com.lifeforge.data.api.ExpenseApi
import com.lifeforge.data.api.IncomeApi
import com.lifeforge.data.db.dao.AssetDao
import com.lifeforge.data.db.dao.ExpenseDao
import com.lifeforge.data.db.dao.IncomeDao
import com.lifeforge.data.db.dao.UserDao
import com.lifeforge.data.db.entity.AssetEntity
import com.lifeforge.data.db.entity.ExpenseEntity
import com.lifeforge.data.db.entity.IncomeEntity
import com.lifeforge.data.mapper.assetRequestDto
import com.lifeforge.data.mapper.expenseRequestDto
import com.lifeforge.data.mapper.incomeRequestDto
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.model.dto.AssetRequestDto
import com.lifeforge.data.model.dto.ExpenseRequestDto
import com.lifeforge.data.model.dto.IncomeRequestDto
import com.lifeforge.data.sync.OfflineWriter
import com.lifeforge.data.sync.Outbox
import com.lifeforge.data.sync.OutboxEntityType
import com.lifeforge.data.sync.SyncEngine
import com.lifeforge.data.sync.TransactionRunner
import com.lifeforge.data.sync.asLocalStore
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.model.Asset
import com.lifeforge.domain.model.AssetType
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Expense
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.Income
import com.lifeforge.domain.model.IncomeType
import com.lifeforge.domain.model.mapCatching
import com.lifeforge.domain.repository.AssetRepository
import com.lifeforge.domain.repository.ExpenseRepository
import com.lifeforge.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementações CRUD agrupadas — três classes que seguem o template
 * offline-first do [GoalRepositoryImpl], com diferenças apenas nos campos.
 */

// ============================================================================
// Income
// ============================================================================

@Singleton
class IncomeRepositoryImpl @Inject constructor(
    private val api: IncomeApi,
    private val dao: IncomeDao,
    private val userDao: UserDao,
    private val writer: OfflineWriter,
    private val engine: SyncEngine,
    private val outbox: Outbox,
    private val tx: TransactionRunner,
    private val json: Json,
    private val clock: Clock,
) : IncomeRepository {

    private val store = dao.asLocalStore()

    override fun observeAll(): Flow<List<Income>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh(): DataResult<Unit> = engine.refresh(OutboxEntityType.INCOME)

    override suspend fun create(
        source: String,
        amount: BigDecimal,
        incomeType: IncomeType,
        recurring: Boolean,
        receivedAt: Instant,
    ): DataResult<Income> {
        val userId = userDao.findCurrent()?.id ?: 0L
        return writer.create(
            type = OutboxEntityType.INCOME,
            store = store,
            payload = encode(incomeRequestDto(source, amount, incomeType, recurring, receivedAt)),
            build = { temporaryId ->
                IncomeEntity(
                    id = temporaryId,
                    userId = userId,
                    source = source,
                    amount = amount,
                    incomeType = incomeType.name,
                    recurring = recurring,
                    receivedAt = receivedAt,
                    createdAt = Instant.now(clock),
                )
            },
            load = { id -> dao.findById(id)?.toDomain() },
        )
    }

    override suspend fun update(
        id: Long,
        source: String,
        amount: BigDecimal,
        incomeType: IncomeType,
        recurring: Boolean,
        receivedAt: Instant,
    ): DataResult<Income> = writer.update(
        type = OutboxEntityType.INCOME,
        store = store,
        id = id,
        payload = encode(incomeRequestDto(source, amount, incomeType, recurring, receivedAt)),
        apply = { current ->
            current.copy(
                source = source,
                amount = amount,
                incomeType = incomeType.name,
                recurring = recurring,
                receivedAt = receivedAt,
            )
        },
        load = { dao.findById(it)?.toDomain() },
    )

    override suspend fun delete(id: Long): DataResult<Unit> =
        writer.delete(OutboxEntityType.INCOME, store, id)

    /** Exclusão em lote é online-only: só apaga localmente depois de confirmada no servidor. */
    override suspend fun deleteAll(): DataResult<Unit> =
        safeApiCall(json) { api.deleteAll() }
            .mapCatching {
                tx.run {
                    dao.deleteAll()
                    outbox.discardAll(OutboxEntityType.INCOME)
                }
            }

    private fun encode(request: IncomeRequestDto): String =
        json.encodeToString(IncomeRequestDto.serializer(), request)
}

// ============================================================================
// Expense
// ============================================================================

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val api: ExpenseApi,
    private val dao: ExpenseDao,
    private val userDao: UserDao,
    private val writer: OfflineWriter,
    private val engine: SyncEngine,
    private val outbox: Outbox,
    private val tx: TransactionRunner,
    private val json: Json,
    private val clock: Clock,
) : ExpenseRepository {

    private val store = dao.asLocalStore()

    override fun observeAll(): Flow<List<Expense>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh(): DataResult<Unit> = engine.refresh(OutboxEntityType.EXPENSE)

    override suspend fun create(
        description: String,
        amount: BigDecimal,
        category: ExpenseCategory,
        recurring: Boolean,
        spentAt: Instant,
    ): DataResult<Expense> {
        val userId = userDao.findCurrent()?.id ?: 0L
        return writer.create(
            type = OutboxEntityType.EXPENSE,
            store = store,
            payload = encode(expenseRequestDto(description, amount, category, recurring, spentAt)),
            build = { temporaryId ->
                ExpenseEntity(
                    id = temporaryId,
                    userId = userId,
                    description = description,
                    amount = amount,
                    category = category.name,
                    recurring = recurring,
                    spentAt = spentAt,
                    createdAt = Instant.now(clock),
                )
            },
            load = { id -> dao.findById(id)?.toDomain() },
        )
    }

    override suspend fun update(
        id: Long,
        description: String,
        amount: BigDecimal,
        category: ExpenseCategory,
        recurring: Boolean,
        spentAt: Instant,
    ): DataResult<Expense> = writer.update(
        type = OutboxEntityType.EXPENSE,
        store = store,
        id = id,
        payload = encode(expenseRequestDto(description, amount, category, recurring, spentAt)),
        apply = { current ->
            current.copy(
                description = description,
                amount = amount,
                category = category.name,
                recurring = recurring,
                spentAt = spentAt,
            )
        },
        load = { dao.findById(it)?.toDomain() },
    )

    override suspend fun delete(id: Long): DataResult<Unit> =
        writer.delete(OutboxEntityType.EXPENSE, store, id)

    /** Exclusão em lote é online-only: só apaga localmente depois de confirmada no servidor. */
    override suspend fun deleteAll(): DataResult<Unit> =
        safeApiCall(json) { api.deleteAll() }
            .mapCatching {
                tx.run {
                    dao.deleteAll()
                    outbox.discardAll(OutboxEntityType.EXPENSE)
                }
            }

    private fun encode(request: ExpenseRequestDto): String =
        json.encodeToString(ExpenseRequestDto.serializer(), request)
}

// ============================================================================
// Asset
// ============================================================================

@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val dao: AssetDao,
    private val userDao: UserDao,
    private val writer: OfflineWriter,
    private val engine: SyncEngine,
    private val json: Json,
    private val clock: Clock,
) : AssetRepository {

    private val store = dao.asLocalStore()

    override fun observeAll(): Flow<List<Asset>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh(): DataResult<Unit> = engine.refresh(OutboxEntityType.ASSET)

    override suspend fun create(
        name: String,
        assetType: AssetType,
        currentValue: BigDecimal,
        expectedReturn: BigDecimal,
        volatility: BigDecimal,
    ): DataResult<Asset> {
        val userId = userDao.findCurrent()?.id ?: 0L
        return writer.create(
            type = OutboxEntityType.ASSET,
            store = store,
            payload = encode(assetRequestDto(name, assetType, currentValue, expectedReturn, volatility)),
            build = { temporaryId ->
                AssetEntity(
                    id = temporaryId,
                    userId = userId,
                    name = name,
                    assetType = assetType.name,
                    currentValue = currentValue,
                    expectedReturn = expectedReturn,
                    volatility = volatility,
                    createdAt = Instant.now(clock),
                )
            },
            load = { id -> dao.findById(id)?.toDomain() },
        )
    }

    override suspend fun update(
        id: Long,
        name: String,
        assetType: AssetType,
        currentValue: BigDecimal,
        expectedReturn: BigDecimal,
        volatility: BigDecimal,
    ): DataResult<Asset> = writer.update(
        type = OutboxEntityType.ASSET,
        store = store,
        id = id,
        payload = encode(assetRequestDto(name, assetType, currentValue, expectedReturn, volatility)),
        apply = { current ->
            current.copy(
                name = name,
                assetType = assetType.name,
                currentValue = currentValue,
                expectedReturn = expectedReturn,
                volatility = volatility,
            )
        },
        load = { dao.findById(it)?.toDomain() },
    )

    override suspend fun delete(id: Long): DataResult<Unit> =
        writer.delete(OutboxEntityType.ASSET, store, id)

    private fun encode(request: AssetRequestDto): String =
        json.encodeToString(AssetRequestDto.serializer(), request)
}
