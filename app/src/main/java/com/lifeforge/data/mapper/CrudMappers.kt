package com.lifeforge.data.mapper

import com.lifeforge.data.db.entity.AssetEntity
import com.lifeforge.data.db.entity.ExpenseEntity
import com.lifeforge.data.db.entity.IncomeEntity
import com.lifeforge.data.model.dto.AssetDto
import com.lifeforge.data.model.dto.AssetRequestDto
import com.lifeforge.data.model.dto.ExpenseDto
import com.lifeforge.data.model.dto.ExpenseRequestDto
import com.lifeforge.data.model.dto.IncomeDto
import com.lifeforge.data.model.dto.IncomeRequestDto
import com.lifeforge.domain.model.Asset
import com.lifeforge.domain.model.AssetType
import com.lifeforge.domain.model.Expense
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.Income
import com.lifeforge.domain.model.IncomeType
import java.math.BigDecimal
import java.time.Instant

/**
 * Mappers do trio CRUD financeiro. Todos seguem a mesma estrutura:
 * DTO→Entity, Entity→Domain, e um builder de request para create.
 */

// ============================================================================
// Income
// ============================================================================

fun IncomeDto.toEntity(): IncomeEntity = IncomeEntity(
    id = id,
    userId = userId,
    source = source,
    amount = BigDecimal(amount),
    incomeType = incomeType,
    recurring = recurring,
    receivedAt = Instant.parse(receivedAt),
    createdAt = Instant.parse(createdAt),
)

fun IncomeEntity.toDomain(): Income = Income(
    id = id,
    userId = userId,
    source = source,
    amount = amount,
    incomeType = IncomeType.valueOf(incomeType),
    recurring = recurring,
    receivedAt = receivedAt,
    createdAt = createdAt,
)

fun incomeRequestDto(
    source: String,
    amount: BigDecimal,
    incomeType: IncomeType,
    recurring: Boolean,
    receivedAt: Instant,
): IncomeRequestDto = IncomeRequestDto(
    source = source,
    amount = amount.toPlainString(),
    incomeType = incomeType.name,
    recurring = recurring,
    receivedAt = receivedAt.toString(),
)

// ============================================================================
// Expense
// ============================================================================

fun ExpenseDto.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    userId = userId,
    description = description,
    amount = BigDecimal(amount),
    category = category,
    recurring = recurring,
    spentAt = Instant.parse(spentAt),
    createdAt = Instant.parse(createdAt),
)

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    userId = userId,
    description = description,
    amount = amount,
    category = ExpenseCategory.valueOf(category),
    recurring = recurring,
    spentAt = spentAt,
    createdAt = createdAt,
)

fun expenseRequestDto(
    description: String,
    amount: BigDecimal,
    category: ExpenseCategory,
    recurring: Boolean,
    spentAt: Instant,
): ExpenseRequestDto = ExpenseRequestDto(
    description = description,
    amount = amount.toPlainString(),
    category = category.name,
    recurring = recurring,
    spentAt = spentAt.toString(),
)

// ============================================================================
// Asset
// ============================================================================

fun AssetDto.toEntity(): AssetEntity = AssetEntity(
    id = id,
    userId = userId,
    name = name,
    assetType = assetType,
    currentValue = BigDecimal(currentValue),
    expectedReturn = BigDecimal(expectedReturn),
    volatility = BigDecimal(volatility),
    createdAt = Instant.parse(createdAt),
)

fun AssetEntity.toDomain(): Asset = Asset(
    id = id,
    userId = userId,
    name = name,
    assetType = AssetType.valueOf(assetType),
    currentValue = currentValue,
    expectedReturn = expectedReturn,
    volatility = volatility,
    createdAt = createdAt,
)

fun assetRequestDto(
    name: String,
    assetType: AssetType,
    currentValue: BigDecimal,
    expectedReturn: BigDecimal,
    volatility: BigDecimal,
): AssetRequestDto = AssetRequestDto(
    name = name,
    assetType = assetType.name,
    currentValue = currentValue.toPlainString(),
    expectedReturn = expectedReturn.toPlainString(),
    volatility = volatility.toPlainString(),
)
