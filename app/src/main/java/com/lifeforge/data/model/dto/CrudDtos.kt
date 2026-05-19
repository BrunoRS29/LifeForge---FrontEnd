package com.lifeforge.data.model.dto

import kotlinx.serialization.Serializable

// ============================================================================
// Goals
// ============================================================================

@Serializable
data class GoalRequestDto(
    val name: String,
    val category: String,
    val targetAmount: String,    // BigDecimal serializado
    val targetDate: String,      // ISO-8601
    val priority: Int = 1,
)

@Serializable
data class GoalDto(
    val id: Long,
    val userId: Long,
    val name: String,
    val category: String,
    val targetAmount: String,
    val targetDate: String,
    val priority: Int,
    val createdAt: String,
)

// ============================================================================
// Incomes
// ============================================================================

@Serializable
data class IncomeRequestDto(
    val source: String,
    val amount: String,
    val incomeType: String,
    val recurring: Boolean = false,
    val receivedAt: String,
)

@Serializable
data class IncomeDto(
    val id: Long,
    val userId: Long,
    val source: String,
    val amount: String,
    val incomeType: String,
    val recurring: Boolean,
    val receivedAt: String,
    val createdAt: String,
)

// ============================================================================
// Expenses
// ============================================================================

@Serializable
data class ExpenseRequestDto(
    val description: String,
    val amount: String,
    val category: String,
    val recurring: Boolean = false,
    val spentAt: String,
)

@Serializable
data class ExpenseDto(
    val id: Long,
    val userId: Long,
    val description: String,
    val amount: String,
    val category: String,
    val recurring: Boolean,
    val spentAt: String,
    val createdAt: String,
)

// ============================================================================
// Assets
// ============================================================================

@Serializable
data class AssetRequestDto(
    val name: String,
    val assetType: String,
    val currentValue: String,
    val expectedReturn: String,
    val volatility: String,
)

@Serializable
data class AssetDto(
    val id: Long,
    val userId: Long,
    val name: String,
    val assetType: String,
    val currentValue: String,
    val expectedReturn: String,
    val volatility: String,
    val createdAt: String,
)
