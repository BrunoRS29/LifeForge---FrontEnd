package com.lifeforge.data.model.dto

import kotlinx.serialization.Serializable

/**
 * DTOs da importação em lote (POST /api/v1/finance/import). Reaproveita
 * [IncomeRequestDto] e [ExpenseRequestDto] do CRUD avulso.
 */

@Serializable
data class ImportRequestDto(
    val incomes: List<IncomeRequestDto> = emptyList(),
    val expenses: List<ExpenseRequestDto> = emptyList(),
)

@Serializable
data class ImportResultDto(
    val incomesCreated: Int,
    val expensesCreated: Int,
    val skipped: Int,
)
