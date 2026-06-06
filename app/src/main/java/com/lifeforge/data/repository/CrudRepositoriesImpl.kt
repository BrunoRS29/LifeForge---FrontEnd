package com.lifeforge.data.repository

import com.lifeforge.data.api.AssetApi
import com.lifeforge.data.api.ExpenseApi
import com.lifeforge.data.api.IncomeApi
import com.lifeforge.data.db.dao.AssetDao
import com.lifeforge.data.db.dao.ExpenseDao
import com.lifeforge.data.db.dao.IncomeDao
import com.lifeforge.data.mapper.assetRequestDto
import com.lifeforge.data.mapper.expenseRequestDto
import com.lifeforge.data.mapper.incomeRequestDto
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.mapper.toEntity
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
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementações CRUD agrupadas — três classes que seguem o template do
 * [GoalRepositoryImpl] com diferenças apenas nos campos.
 */

// ============================================================================
// Income
// ============================================================================

@Singleton
class IncomeRepositoryImpl @Inject constructor(
    private val api: IncomeApi,
    private val dao: IncomeDao,
    private val json: Json,
) : IncomeRepository {

    override fun observeAll(): Flow<List<Income>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh(): DataResult<Unit> =
        safeApiCall(json) { api.list() }
            .mapCatching { dtos -> dao.replaceAll(dtos.map { it.toEntity() }) }

    override suspend fun create(
        source: String,
        amount: BigDecimal,
        incomeType: IncomeType,
        recurring: Boolean,
        receivedAt: Instant,
    ): DataResult<Income> = safeApiCall(json) {
        api.create(incomeRequestDto(source, amount, incomeType, recurring, receivedAt))
    }.mapCatching { dto ->
        val entity = dto.toEntity()
        dao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun update(
        id: Long,
        source: String,
        amount: BigDecimal,
        incomeType: IncomeType,
        recurring: Boolean,
        receivedAt: Instant,
    ): DataResult<Income> = safeApiCall(json) {
        api.update(id, incomeRequestDto(source, amount, incomeType, recurring, receivedAt))
    }.mapCatching { dto ->
        val entity = dto.toEntity()
        dao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun delete(id: Long): DataResult<Unit> =
        safeApiCall(json) { api.delete(id) }
            .mapCatching { dao.deleteById(id) }

    override suspend fun deleteAll(): DataResult<Unit> =
        safeApiCall(json) { api.deleteAll() }
            .mapCatching { dao.deleteAll() }
}

// ============================================================================
// Expense
// ============================================================================

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val api: ExpenseApi,
    private val dao: ExpenseDao,
    private val json: Json,
) : ExpenseRepository {

    override fun observeAll(): Flow<List<Expense>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh(): DataResult<Unit> =
        safeApiCall(json) { api.list() }
            .mapCatching { dtos -> dao.replaceAll(dtos.map { it.toEntity() }) }

    override suspend fun create(
        description: String,
        amount: BigDecimal,
        category: ExpenseCategory,
        recurring: Boolean,
        spentAt: Instant,
    ): DataResult<Expense> = safeApiCall(json) {
        api.create(expenseRequestDto(description, amount, category, recurring, spentAt))
    }.mapCatching { dto ->
        val entity = dto.toEntity()
        dao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun update(
        id: Long,
        description: String,
        amount: BigDecimal,
        category: ExpenseCategory,
        recurring: Boolean,
        spentAt: Instant,
    ): DataResult<Expense> = safeApiCall(json) {
        api.update(id, expenseRequestDto(description, amount, category, recurring, spentAt))
    }.mapCatching { dto ->
        val entity = dto.toEntity()
        dao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun delete(id: Long): DataResult<Unit> =
        safeApiCall(json) { api.delete(id) }
            .mapCatching { dao.deleteById(id) }

    override suspend fun deleteAll(): DataResult<Unit> =
        safeApiCall(json) { api.deleteAll() }
            .mapCatching { dao.deleteAll() }
}

// ============================================================================
// Asset
// ============================================================================

@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val api: AssetApi,
    private val dao: AssetDao,
    private val json: Json,
) : AssetRepository {

    override fun observeAll(): Flow<List<Asset>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh(): DataResult<Unit> =
        safeApiCall(json) { api.list() }
            .mapCatching { dtos -> dao.replaceAll(dtos.map { it.toEntity() }) }

    override suspend fun create(
        name: String,
        assetType: AssetType,
        currentValue: BigDecimal,
        expectedReturn: BigDecimal,
        volatility: BigDecimal,
    ): DataResult<Asset> = safeApiCall(json) {
        api.create(assetRequestDto(name, assetType, currentValue, expectedReturn, volatility))
    }.mapCatching { dto ->
        val entity = dto.toEntity()
        dao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun update(
        id: Long,
        name: String,
        assetType: AssetType,
        currentValue: BigDecimal,
        expectedReturn: BigDecimal,
        volatility: BigDecimal,
    ): DataResult<Asset> = safeApiCall(json) {
        api.update(id, assetRequestDto(name, assetType, currentValue, expectedReturn, volatility))
    }.mapCatching { dto ->
        val entity = dto.toEntity()
        dao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun delete(id: Long): DataResult<Unit> =
        safeApiCall(json) { api.delete(id) }
            .mapCatching { dao.deleteById(id) }
}
