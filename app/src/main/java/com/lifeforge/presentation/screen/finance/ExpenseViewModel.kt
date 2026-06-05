package com.lifeforge.presentation.screen.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Expense
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.ExpenseScheduleParams
import com.lifeforge.domain.model.RecurrenceType
import com.lifeforge.domain.model.onFailure
import com.lifeforge.domain.usecase.CreateExpenseScheduleUseCase
import com.lifeforge.domain.usecase.CreateExpenseUseCase
import com.lifeforge.domain.usecase.DeleteExpenseUseCase
import com.lifeforge.domain.usecase.ObserveExpensesUseCase
import com.lifeforge.domain.usecase.RefreshExpensesUseCase
import com.lifeforge.domain.usecase.UpdateExpenseUseCase
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
 * ViewModel da sub-aba de Despesas. Espelha o [IncomeViewModel]: criação
 * única, criação recorrente (mensal/parcelada) e edição de um lançamento.
 */
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    observeExpenses: ObserveExpensesUseCase,
    private val refreshExpenses: RefreshExpensesUseCase,
    private val createExpense: CreateExpenseUseCase,
    private val createExpenseSchedule: CreateExpenseScheduleUseCase,
    private val updateExpense: UpdateExpenseUseCase,
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

    fun openForm(defaultDate: Instant = Instant.now()) {
        localState.update { it.copy(form = ExpenseFormState(startDate = defaultDate)) }
    }

    fun openEditForm(expense: Expense) {
        localState.update {
            it.copy(
                form = ExpenseFormState(
                    editingId = expense.id,
                    description = expense.description,
                    amountInput = expense.amount.toPlainString(),
                    category = expense.category,
                    recurring = expense.recurring,
                    isRecurrent = false,
                    startDate = expense.spentAt,
                ),
            )
        }
    }

    fun closeForm() {
        localState.update { it.copy(form = null) }
    }

    private inline fun updateForm(crossinline mutate: (ExpenseFormState) -> ExpenseFormState) {
        localState.update { local ->
            local.form?.let { local.copy(form = mutate(it)) } ?: local
        }
    }

    fun onFormDescriptionChange(description: String) =
        updateForm { it.copy(description = description, descriptionError = null) }

    fun onFormAmountChange(amount: String) =
        updateForm { it.copy(amountInput = sanitizeCurrencyInput(amount), amountError = null) }

    fun onFormCategoryChange(category: ExpenseCategory) = updateForm { it.copy(category = category) }

    fun onFormRecurringChange(recurring: Boolean) = updateForm { it.copy(recurring = recurring) }

    fun onFormIsRecurrentChange(isRecurrent: Boolean) = updateForm { it.copy(isRecurrent = isRecurrent) }

    fun onFormRecurrenceTypeChange(type: RecurrenceType) = updateForm { it.copy(recurrenceType = type) }

    fun onFormStartDateChange(date: Instant) = updateForm { it.copy(startDate = date) }

    fun onFormEndDateChange(date: Instant?) = updateForm { it.copy(endDate = date) }

    fun onFormInstallmentsChange(value: String) =
        updateForm { it.copy(installmentsInput = value.filter { ch -> ch.isDigit() }.take(3)) }

    fun submitForm() {
        val form = localState.value.form ?: return
        if (localState.value.isSubmitting) return

        val amount = parseCurrencyInput(form.amountInput)
        if (amount == null) {
            updateForm { it.copy(amountError = "Valor inválido") }
            return
        }

        viewModelScope.launch {
            localState.update { it.copy(isSubmitting = true, errorBanner = null) }

            val result: DataResult<*> = when {
                form.editingId != null -> updateExpense(
                    id = form.editingId,
                    description = form.description,
                    amount = amount,
                    category = form.category,
                    recurring = form.recurring,
                    spentAt = form.startDate,
                )
                form.isRecurrent -> {
                    val params = ExpenseScheduleParams(
                        description = form.description,
                        amountPerOccurrence = amount,
                        category = form.category,
                        recurrence = form.recurrenceType,
                        startDate = form.startDate,
                        endDate = if (form.recurrenceType == RecurrenceType.MONTHLY) form.endDate else null,
                        installmentsTotal = if (form.recurrenceType == RecurrenceType.INSTALLMENTS) {
                            form.installmentsInput.toIntOrNull()
                        } else null,
                    )
                    createExpenseSchedule(params).also {
                        if (it is DataResult.Success) refreshExpenses()
                    }
                }
                else -> createExpense(
                    description = form.description,
                    amount = amount,
                    category = form.category,
                    recurring = form.recurring,
                    spentAt = form.startDate,
                )
            }

            when (result) {
                is DataResult.Success -> localState.update { it.copy(form = null, isSubmitting = false) }
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
                "description" -> updateForm { it.copy(descriptionError = error.message) }
                "amount" -> updateForm { it.copy(amountError = error.message) }
                else -> localState.update { it.copy(errorBanner = error.message ?: "Dados inválidos") }
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
    val editingId: Long? = null,
    val description: String = "",
    val amountInput: String = "",
    val category: ExpenseCategory = ExpenseCategory.HOUSING,
    val recurring: Boolean = true,
    // --- Recorrência (schedule) — só no modo criação ---
    val isRecurrent: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.MONTHLY,
    val startDate: Instant = Instant.now(),
    val endDate: Instant? = null,
    val installmentsInput: String = "12",
    val descriptionError: String? = null,
    val amountError: String? = null,
) {
    val isEditing: Boolean get() = editingId != null

    val installmentsValid: Boolean
        get() = (installmentsInput.toIntOrNull() ?: 0) > 0

    val canSubmit: Boolean
        get() = description.isNotBlank() && amountInput.isNotBlank() &&
            (!isRecurrent || recurrenceType != RecurrenceType.INSTALLMENTS || installmentsValid)
}
