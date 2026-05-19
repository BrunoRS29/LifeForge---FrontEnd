package com.lifeforge.presentation.screen.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Expense
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.onFailure
import com.lifeforge.domain.usecase.CreateExpenseUseCase
import com.lifeforge.domain.usecase.DeleteExpenseUseCase
import com.lifeforge.domain.usecase.ObserveExpensesUseCase
import com.lifeforge.domain.usecase.RefreshExpensesUseCase
import com.lifeforge.presentation.common.parseCurrencyInput
import com.lifeforge.presentation.common.sanitizeCurrencyInput
import com.lifeforge.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel da sub-aba de Despesas. Espelha o [IncomeViewModel] com
 * campo `description` no lugar de `source` e [ExpenseCategory] no
 * lugar de [com.lifeforge.domain.model.IncomeType].
 *
 * Apesar da repetição estrutural, manter ViewModels separados deixa
 * o estado fortemente tipado (sem `Any` ou generics confusos) e
 * facilita evoluir cada um independentemente — Income pode ganhar
 * recorrência por dia do mês, Expense pode ganhar anexos, etc.
 */
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    observeExpenses: ObserveExpensesUseCase,
    private val refreshExpenses: RefreshExpensesUseCase,
    private val createExpense: CreateExpenseUseCase,
    private val deleteExpense: DeleteExpenseUseCase,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalUiState())

    val state: StateFlow<ExpenseUiState> = combine(
        observeExpenses(),
        localState,
    ) { expenses, local ->
        ExpenseUiState(
            expenses = expenses,
            isRefreshing = local.isRefreshing,
            errorBanner = local.errorBanner,
            form = local.form,
            isSubmitting = local.isSubmitting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpenseUiState(),
    )

    init { refresh() }

    fun refresh() {
        if (localState.value.isRefreshing) return
        viewModelScope.launch {
            localState.update { it.copy(isRefreshing = true, errorBanner = null) }
            refreshExpenses().onFailure { error ->
                localState.update { it.copy(errorBanner = error.toUserMessage()) }
            }
            localState.update { it.copy(isRefreshing = false) }
        }
    }

    fun openForm() {
        localState.update { it.copy(form = ExpenseFormState()) }
    }

    fun closeForm() {
        localState.update { it.copy(form = null) }
    }

    fun onFormDescriptionChange(description: String) {
        localState.update { local ->
            local.form?.let { f ->
                local.copy(form = f.copy(description = description, descriptionError = null))
            } ?: local
        }
    }

    fun onFormAmountChange(amount: String) {
        val sanitized = sanitizeCurrencyInput(amount)
        localState.update { local ->
            local.form?.let { f ->
                local.copy(form = f.copy(amountInput = sanitized, amountError = null))
            } ?: local
        }
    }

    fun onFormCategoryChange(category: ExpenseCategory) {
        localState.update { local ->
            local.form?.let { f -> local.copy(form = f.copy(category = category)) } ?: local
        }
    }

    fun onFormRecurringChange(recurring: Boolean) {
        localState.update { local ->
            local.form?.let { f -> local.copy(form = f.copy(recurring = recurring)) } ?: local
        }
    }

    fun submitForm() {
        val form = localState.value.form ?: return
        if (localState.value.isSubmitting) return

        val amount = parseCurrencyInput(form.amountInput)
        if (amount == null) {
            localState.update { local ->
                local.form?.let { f ->
                    local.copy(form = f.copy(amountError = "Valor inválido"))
                } ?: local
            }
            return
        }

        viewModelScope.launch {
            localState.update { it.copy(isSubmitting = true, errorBanner = null) }
            val result = createExpense(
                description = form.description,
                amount = amount,
                category = form.category,
                recurring = form.recurring,
                spentAt = Instant.now(),
            )
            when (result) {
                is DataResult.Success -> localState.update {
                    it.copy(form = null, isSubmitting = false)
                }
                is DataResult.Failure -> {
                    handleFormError(result.error)
                    localState.update { it.copy(isSubmitting = false) }
                }
            }
        }
    }

    private fun handleFormError(error: AppError) {
        when (error) {
            is AppError.Validation -> when (error.field) {
                "description" -> localState.update { local ->
                    local.form?.let { f ->
                        local.copy(form = f.copy(descriptionError = error.message))
                    } ?: local
                }
                "amount" -> localState.update { local ->
                    local.form?.let { f ->
                        local.copy(form = f.copy(amountError = error.message))
                    } ?: local
                }
                else -> localState.update {
                    it.copy(errorBanner = error.message ?: "Dados inválidos")
                }
            }
            else -> localState.update { it.copy(errorBanner = error.toUserMessage()) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            deleteExpense(id).onFailure { error ->
                localState.update { it.copy(errorBanner = error.toUserMessage()) }
            }
        }
    }

    fun onErrorBannerDismiss() {
        localState.update { it.copy(errorBanner = null) }
    }

    private data class LocalUiState(
        val isRefreshing: Boolean = false,
        val errorBanner: String? = null,
        val form: ExpenseFormState? = null,
        val isSubmitting: Boolean = false,
    )
}

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorBanner: String? = null,
    val form: ExpenseFormState? = null,
    val isSubmitting: Boolean = false,
)

data class ExpenseFormState(
    val description: String = "",
    val amountInput: String = "",
    val category: ExpenseCategory = ExpenseCategory.HOUSING,
    val recurring: Boolean = true,
    val descriptionError: String? = null,
    val amountError: String? = null,
) {
    val canSubmit: Boolean
        get() = description.isNotBlank() && amountInput.isNotBlank()
}
