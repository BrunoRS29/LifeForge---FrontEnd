package com.lifeforge.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Templates recorrentes (Sprint 6). No backend eles GERAM Income/Expense
 * individuais; no app são usados para criar/editar o "molde". Os registros
 * materializados chegam pela lista normal de receitas/despesas (já cacheada
 * em Room), por isso os schedules em si são consumidos via rede (sem cache
 * local) — mesmo padrão do PredictionRepository.
 *
 * `generatedCount` vem na resposta do backend e informa quantos registros o
 * schedule materializou (útil para feedback ao usuário).
 */
data class IncomeSchedule(
    val id: Long,
    val userId: Long,
    val source: String,
    val amountPerOccurrence: BigDecimal,
    val incomeType: IncomeType,
    val recurrence: RecurrenceType,
    val startDate: Instant,
    val endDate: Instant?,
    val installmentsTotal: Int?,
    val createdAt: Instant,
    val generatedCount: Int,
)

data class ExpenseSchedule(
    val id: Long,
    val userId: Long,
    val description: String,
    val amountPerOccurrence: BigDecimal,
    val category: ExpenseCategory,
    val recurrence: RecurrenceType,
    val startDate: Instant,
    val endDate: Instant?,
    val installmentsTotal: Int?,
    val createdAt: Instant,
    val generatedCount: Int,
)

/**
 * Parâmetros de criação de um schedule de receita (do form -> use case -> repo).
 * endDate/installmentsTotal são opcionais conforme a recorrência.
 */
data class IncomeScheduleParams(
    val source: String,
    val amountPerOccurrence: BigDecimal,
    val incomeType: IncomeType,
    val recurrence: RecurrenceType,
    val startDate: Instant,
    val endDate: Instant?,
    val installmentsTotal: Int?,
)

data class ExpenseScheduleParams(
    val description: String,
    val amountPerOccurrence: BigDecimal,
    val category: ExpenseCategory,
    val recurrence: RecurrenceType,
    val startDate: Instant,
    val endDate: Instant?,
    val installmentsTotal: Int?,
)
