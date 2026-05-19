package com.lifeforge.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Entidades de domínio puras (POJOs Kotlin, sem dependência de framework).
 *
 * - `BigDecimal` para todos os valores financeiros — Double tem perda de
 *   precisão inaceitável em soma de aportes ao longo de anos.
 * - `Instant` para timestamps em UTC. Conversão para fuso local é
 *   responsabilidade da camada de UI.
 */

data class User(
    val id: Long,
    val email: String,
    val name: String,
    val riskProfile: RiskProfile,
    val createdAt: Instant,
)

data class Goal(
    val id: Long,
    val userId: Long,
    val name: String,
    val category: GoalCategory,
    val targetAmount: BigDecimal,
    val targetDate: Instant,
    val priority: Int,
    val createdAt: Instant,
)

data class Income(
    val id: Long,
    val userId: Long,
    val source: String,
    val amount: BigDecimal,
    val incomeType: IncomeType,
    val recurring: Boolean,
    val receivedAt: Instant,
    val createdAt: Instant,
)

data class Expense(
    val id: Long,
    val userId: Long,
    val description: String,
    val amount: BigDecimal,
    val category: ExpenseCategory,
    val recurring: Boolean,
    val spentAt: Instant,
    val createdAt: Instant,
)

data class Asset(
    val id: Long,
    val userId: Long,
    val name: String,
    val assetType: AssetType,
    val currentValue: BigDecimal,
    val expectedReturn: BigDecimal,
    val volatility: BigDecimal,
    val createdAt: Instant,
)

/**
 * Sessão autenticada — combina o token JWT e o usuário decodificado.
 * Não é persistida como entidade de banco; vive apenas no DataStore.
 */
data class AuthSession(
    val token: String,
    val user: User,
)
