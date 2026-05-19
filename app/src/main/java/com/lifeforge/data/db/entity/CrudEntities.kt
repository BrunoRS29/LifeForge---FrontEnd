package com.lifeforge.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant

/**
 * Entidades CRUD financeiras agrupadas por similaridade de shape — todas
 * pertencem a um usuário (via [userId]), todas têm um BigDecimal monetário
 * principal e um Instant de "quando aconteceu".
 *
 * Mantidas juntas pelo mesmo motivo de `CrudDtos.kt` no wire e
 * `CrudApis.kt` no Retrofit — facilita revisar mudanças em conjunto
 * quando o backend altera convenções comuns.
 */

@Entity(
    tableName = "incomes",
    indices = [Index("userId")],
)
data class IncomeEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val source: String,
    val amount: BigDecimal,
    val incomeType: String,    // IncomeType.name
    val recurring: Boolean,
    val receivedAt: Instant,
    val createdAt: Instant,
)

@Entity(
    tableName = "expenses",
    indices = [Index("userId")],
)
data class ExpenseEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val description: String,
    val amount: BigDecimal,
    val category: String,      // ExpenseCategory.name
    val recurring: Boolean,
    val spentAt: Instant,
    val createdAt: Instant,
)

@Entity(
    tableName = "assets",
    indices = [Index("userId")],
)
data class AssetEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val name: String,
    val assetType: String,     // AssetType.name
    val currentValue: BigDecimal,
    val expectedReturn: BigDecimal,
    val volatility: BigDecimal,
    val createdAt: Instant,
)
