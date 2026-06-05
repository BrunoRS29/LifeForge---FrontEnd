package com.lifeforge.domain.usecase

import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.Asset
import com.lifeforge.domain.model.AssetType
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Expense
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.ExpenseSchedule
import com.lifeforge.domain.model.ExpenseScheduleParams
import com.lifeforge.domain.model.Income
import com.lifeforge.domain.model.IncomeSchedule
import com.lifeforge.domain.model.IncomeScheduleParams
import com.lifeforge.domain.model.IncomeType
import com.lifeforge.domain.model.RecurrenceType
import com.lifeforge.domain.model.ScheduleAffect
import com.lifeforge.domain.repository.AssetRepository
import com.lifeforge.domain.repository.ExpenseRepository
import com.lifeforge.domain.repository.ExpenseScheduleRepository
import com.lifeforge.domain.repository.IncomeRepository
import com.lifeforge.domain.repository.IncomeScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import javax.inject.Inject

/**
 * UseCases dos três agregados financeiros (Income, Expense, Asset)
 * agrupados por afinidade. No final do arquivo, [GetFinancialSnapshotUseCase]
 * compõe os três Flows em um snapshot unificado para o Dashboard.
 */

// ============================================================================
// Income
// ============================================================================

class ObserveIncomesUseCase @Inject constructor(
    private val repository: IncomeRepository,
) {
    operator fun invoke(): Flow<List<Income>> = repository.observeAll()
}

class RefreshIncomesUseCase @Inject constructor(
    private val repository: IncomeRepository,
) {
    suspend operator fun invoke(): DataResult<Unit> = repository.refresh()
}

class CreateIncomeUseCase @Inject constructor(
    private val repository: IncomeRepository,
) {
    suspend operator fun invoke(
        source: String,
        amount: BigDecimal,
        incomeType: IncomeType,
        recurring: Boolean,
        receivedAt: Instant,
    ): DataResult<Income> {
        if (source.isBlank()) {
            return DataResult.Failure(AppError.Validation("source", "fonte é obrigatória"))
        }
        if (amount <= BigDecimal.ZERO) {
            return DataResult.Failure(AppError.Validation("amount", "valor deve ser positivo"))
        }
        return repository.create(source.trim(), amount, incomeType, recurring, receivedAt)
    }
}

class UpdateIncomeUseCase @Inject constructor(
    private val repository: IncomeRepository,
) {
    suspend operator fun invoke(
        id: Long,
        source: String,
        amount: BigDecimal,
        incomeType: IncomeType,
        recurring: Boolean,
        receivedAt: Instant,
    ): DataResult<Income> {
        if (source.isBlank()) {
            return DataResult.Failure(AppError.Validation("source", "fonte é obrigatória"))
        }
        if (amount <= BigDecimal.ZERO) {
            return DataResult.Failure(AppError.Validation("amount", "valor deve ser positivo"))
        }
        return repository.update(id, source.trim(), amount, incomeType, recurring, receivedAt)
    }
}

class DeleteIncomeUseCase @Inject constructor(
    private val repository: IncomeRepository,
) {
    suspend operator fun invoke(id: Long): DataResult<Unit> = repository.delete(id)
}

// ============================================================================
// Expense
// ============================================================================

class ObserveExpensesUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    operator fun invoke(): Flow<List<Expense>> = repository.observeAll()
}

class RefreshExpensesUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(): DataResult<Unit> = repository.refresh()
}

class CreateExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(
        description: String,
        amount: BigDecimal,
        category: ExpenseCategory,
        recurring: Boolean,
        spentAt: Instant,
    ): DataResult<Expense> {
        if (description.isBlank()) {
            return DataResult.Failure(AppError.Validation("description", "descrição é obrigatória"))
        }
        if (amount <= BigDecimal.ZERO) {
            return DataResult.Failure(AppError.Validation("amount", "valor deve ser positivo"))
        }
        return repository.create(description.trim(), amount, category, recurring, spentAt)
    }
}

class UpdateExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(
        id: Long,
        description: String,
        amount: BigDecimal,
        category: ExpenseCategory,
        recurring: Boolean,
        spentAt: Instant,
    ): DataResult<Expense> {
        if (description.isBlank()) {
            return DataResult.Failure(AppError.Validation("description", "descrição é obrigatória"))
        }
        if (amount <= BigDecimal.ZERO) {
            return DataResult.Failure(AppError.Validation("amount", "valor deve ser positivo"))
        }
        return repository.update(id, description.trim(), amount, category, recurring, spentAt)
    }
}

class DeleteExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(id: Long): DataResult<Unit> = repository.delete(id)
}

// ============================================================================
// Income / Expense schedules (Sprint 6)
// ============================================================================

class CreateIncomeScheduleUseCase @Inject constructor(
    private val repository: IncomeScheduleRepository,
) {
    suspend operator fun invoke(params: IncomeScheduleParams): DataResult<IncomeSchedule> {
        validateSchedule(params.source, params.amountPerOccurrence, params.recurrence, params.installmentsTotal)
            ?.let { return it }
        return repository.create(params.copy(source = params.source.trim()))
    }
}

class UpdateIncomeScheduleUseCase @Inject constructor(
    private val repository: IncomeScheduleRepository,
) {
    suspend operator fun invoke(
        id: Long,
        params: IncomeScheduleParams,
        affect: ScheduleAffect,
    ): DataResult<IncomeSchedule> {
        validateSchedule(params.source, params.amountPerOccurrence, params.recurrence, params.installmentsTotal)
            ?.let { return it }
        return repository.update(id, params.copy(source = params.source.trim()), affect)
    }
}

class DeleteIncomeScheduleUseCase @Inject constructor(
    private val repository: IncomeScheduleRepository,
) {
    suspend operator fun invoke(id: Long, affect: ScheduleAffect): DataResult<Unit> =
        repository.delete(id, affect)
}

class CreateExpenseScheduleUseCase @Inject constructor(
    private val repository: ExpenseScheduleRepository,
) {
    suspend operator fun invoke(params: ExpenseScheduleParams): DataResult<ExpenseSchedule> {
        validateSchedule(params.description, params.amountPerOccurrence, params.recurrence, params.installmentsTotal)
            ?.let { return it }
        return repository.create(params.copy(description = params.description.trim()))
    }
}

class UpdateExpenseScheduleUseCase @Inject constructor(
    private val repository: ExpenseScheduleRepository,
) {
    suspend operator fun invoke(
        id: Long,
        params: ExpenseScheduleParams,
        affect: ScheduleAffect,
    ): DataResult<ExpenseSchedule> {
        validateSchedule(params.description, params.amountPerOccurrence, params.recurrence, params.installmentsTotal)
            ?.let { return it }
        return repository.update(id, params.copy(description = params.description.trim()), affect)
    }
}

class DeleteExpenseScheduleUseCase @Inject constructor(
    private val repository: ExpenseScheduleRepository,
) {
    suspend operator fun invoke(id: Long, affect: ScheduleAffect): DataResult<Unit> =
        repository.delete(id, affect)
}

/** Validação comum aos schedules (label = source/description). */
private fun validateSchedule(
    label: String,
    amount: BigDecimal,
    recurrence: RecurrenceType,
    installmentsTotal: Int?,
): DataResult.Failure? {
    if (label.isBlank()) {
        return DataResult.Failure(AppError.Validation(null, "campo de texto é obrigatório"))
    }
    if (amount <= BigDecimal.ZERO) {
        return DataResult.Failure(AppError.Validation("amount", "valor deve ser positivo"))
    }
    if (recurrence == RecurrenceType.INSTALLMENTS && (installmentsTotal == null || installmentsTotal <= 0)) {
        return DataResult.Failure(AppError.Validation("installmentsTotal", "informe o número de parcelas"))
    }
    return null
}

// ============================================================================
// Asset
// ============================================================================

class ObserveAssetsUseCase @Inject constructor(
    private val repository: AssetRepository,
) {
    operator fun invoke(): Flow<List<Asset>> = repository.observeAll()
}

class RefreshAssetsUseCase @Inject constructor(
    private val repository: AssetRepository,
) {
    suspend operator fun invoke(): DataResult<Unit> = repository.refresh()
}

class CreateAssetUseCase @Inject constructor(
    private val repository: AssetRepository,
) {
    suspend operator fun invoke(
        name: String,
        assetType: AssetType,
        currentValue: BigDecimal,
        expectedReturn: BigDecimal,
        volatility: BigDecimal,
    ): DataResult<Asset> {
        validateAssetFields(name, currentValue, expectedReturn, volatility)?.let { return it }
        return repository.create(name.trim(), assetType, currentValue, expectedReturn, volatility)
    }
}

class UpdateAssetUseCase @Inject constructor(
    private val repository: AssetRepository,
) {
    suspend operator fun invoke(
        id: Long,
        name: String,
        assetType: AssetType,
        currentValue: BigDecimal,
        expectedReturn: BigDecimal,
        volatility: BigDecimal,
    ): DataResult<Asset> {
        validateAssetFields(name, currentValue, expectedReturn, volatility)?.let { return it }
        return repository.update(id, name.trim(), assetType, currentValue, expectedReturn, volatility)
    }
}

class DeleteAssetUseCase @Inject constructor(
    private val repository: AssetRepository,
) {
    suspend operator fun invoke(id: Long): DataResult<Unit> = repository.delete(id)
}

private fun validateAssetFields(
    name: String,
    currentValue: BigDecimal,
    expectedReturn: BigDecimal,
    volatility: BigDecimal,
): DataResult.Failure? {
    if (name.isBlank()) {
        return DataResult.Failure(AppError.Validation("name", "nome do ativo é obrigatório"))
    }
    if (currentValue < BigDecimal.ZERO) {
        return DataResult.Failure(AppError.Validation("currentValue", "valor atual não pode ser negativo"))
    }
    // Expected return pode ser negativo (perda esperada), mas volatility não.
    if (volatility < BigDecimal.ZERO) {
        return DataResult.Failure(AppError.Validation("volatility", "volatilidade não pode ser negativa"))
    }
    return null
}

// ============================================================================
// GetFinancialSnapshotUseCase — composição para o Dashboard
// ============================================================================

/**
 * Snapshot consolidado para a tela inicial.
 *
 * - [totalAssets]: soma de `currentValue` de todos os ativos.
 * - [monthlyIncome] / [monthlyExpenses]: soma dos lançamentos
 *   marcados como `recurring`. Lançamentos pontuais ficam fora —
 *   o dashboard mostra ritmo mensal estável, não picos.
 * - [savingsRate]: (income − expenses) / income, em percentual.
 *   Indefinido quando income é zero (retorna 0.0).
 */
data class FinancialSnapshot(
    val totalAssets: BigDecimal,
    val monthlyIncome: BigDecimal,
    val monthlyExpenses: BigDecimal,
    val savingsRate: BigDecimal,
)

class GetFinancialSnapshotUseCase @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository,
    private val assetRepository: AssetRepository,
) {

    operator fun invoke(): Flow<FinancialSnapshot> = combine(
        incomeRepository.observeAll(),
        expenseRepository.observeAll(),
        assetRepository.observeAll(),
    ) { incomes, expenses, assets ->
        val totalAssets = assets.fold(BigDecimal.ZERO) { acc, a -> acc + a.currentValue }
        val monthlyIncome = incomes.filter { it.recurring }
            .fold(BigDecimal.ZERO) { acc, i -> acc + i.amount }
        val monthlyExpenses = expenses.filter { it.recurring }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }

        val savingsRate = if (monthlyIncome > BigDecimal.ZERO) {
            (monthlyIncome - monthlyExpenses)
                .divide(monthlyIncome, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }

        FinancialSnapshot(totalAssets, monthlyIncome, monthlyExpenses, savingsRate)
    }
}
