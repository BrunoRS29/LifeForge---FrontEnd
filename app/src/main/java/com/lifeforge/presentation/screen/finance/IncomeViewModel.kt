package com.lifeforge.presentation.screen.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Income
import com.lifeforge.domain.model.IncomeScheduleParams
import com.lifeforge.domain.model.IncomeType
import com.lifeforge.domain.model.RecurrenceType
import com.lifeforge.domain.model.onFailure
import com.lifeforge.domain.usecase.CreateIncomeScheduleUseCase
import com.lifeforge.domain.usecase.CreateIncomeUseCase
import com.lifeforge.domain.usecase.DeleteIncomeUseCase
import com.lifeforge.domain.usecase.ObserveIncomesUseCase
import com.lifeforge.domain.usecase.RefreshIncomesUseCase
import com.lifeforge.domain.usecase.UpdateIncomeUseCase
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
 * ViewModel da sub-aba de Receitas.
 *
 * O form de criação/edição (ModalBottomSheet) tem três modos:
 *  - **Criar único** (default): cria UMA receita.
 *  - **Criar recorrente**: cria um SCHEDULE (mensal/parcelado) e refresca a lista.
 *  - **Editar** (`editingId != null`): atualiza um registro existente; sem
 *    opções de recorrência (edita-se um lançamento, não um schedule).
 */
@HiltViewModel
class IncomeViewModel @Inject constructor(
    observeIncomes: ObserveIncomesUseCase,
    private val refreshIncomes: RefreshIncomesUseCase,
    private val createIncome: CreateIncomeUseCase,
    private val createIncomeSchedule: CreateIncomeScheduleUseCase,
    private val updateIncome: UpdateIncomeUseCase,
    private val deleteIncome: DeleteIncomeUseCase,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalUiState())

    val state: StateFlow<IncomeUiState> = combine(
        observeIncomes(),
        localState,
    ) { incomes, local ->
        IncomeUiState(
            incomes = incomes,
            isRefreshing = local.isRefreshing,
            errorBanner = local.errorBanner,
            form = local.form,
            isSubmitting = local.isSubmitting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = IncomeUiState(),
    )

    init { refresh() }

    fun refresh() {
        if (localState.value.isRefreshing) return
        viewModelScope.launch {
            localState.update { it.copy(isRefreshing = true, errorBanner = null) }
            refreshIncomes().onFailure { error ->
                localState.update { it.copy(errorBanner = error.toUserMessage()) }
            }
            localState.update { it.copy(isRefreshing = false) }
        }
    }

    // ------------------------------------------------------------------------
    // Form modal — open/close/edit/submit
    // ------------------------------------------------------------------------

    /** Abre o form em modo criação, com a data inicial sugerida (mês selecionado). */
    fun openForm(defaultDate: Instant = Instant.now()) {
        localState.update { it.copy(form = IncomeFormState(startDate = defaultDate)) }
    }

    /** Abre o form em modo edição, pré-preenchido com o registro. */
    fun openEditForm(income: Income) {
        localState.update {
            it.copy(
                form = IncomeFormState(
                    editingId = income.id,
                    source = income.source,
                    amountInput = income.amount.toPlainString(),
                    incomeType = income.incomeType,
                    recurring = income.recurring,
                    isRecurrent = false,
                    startDate = income.receivedAt,
                ),
            )
        }
    }

    fun closeForm() {
        localState.update { it.copy(form = null) }
    }

    private inline fun updateForm(crossinline mutate: (IncomeFormState) -> IncomeFormState) {
        localState.update { local ->
            local.form?.let { local.copy(form = mutate(it)) } ?: local
        }
    }

    fun onFormSourceChange(source: String) = updateForm { it.copy(source = source, sourceError = null) }

    fun onFormAmountChange(amount: String) =
        updateForm { it.copy(amountInput = sanitizeCurrencyInput(amount), amountError = null) }

    fun onFormTypeChange(type: IncomeType) = updateForm { it.copy(incomeType = type) }

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
                form.editingId != null -> updateIncome(
                    id = form.editingId,
                    source = form.source,
                    amount = amount,
                    incomeType = form.incomeType,
                    recurring = form.recurring,
                    receivedAt = form.startDate,
                )
                form.isRecurrent -> {
                    val params = IncomeScheduleParams(
                        source = form.source,
                        amountPerOccurrence = amount,
                        incomeType = form.incomeType,
                        recurrence = form.recurrenceType,
                        startDate = form.startDate,
                        endDate = if (form.recurrenceType == RecurrenceType.MONTHLY) form.endDate else null,
                        installmentsTotal = if (form.recurrenceType == RecurrenceType.INSTALLMENTS) {
                            form.installmentsInput.toIntOrNull()
                        } else null,
                    )
                    createIncomeSchedule(params).also {
                        if (it is DataResult.Success) refreshIncomes()
                    }
                }
                else -> createIncome(
                    source = form.source,
                    amount = amount,
                    incomeType = form.incomeType,
                    recurring = form.recurring,
                    receivedAt = form.startDate,
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
                "source" -> updateForm { it.copy(sourceError = error.message) }
                "amount" -> updateForm { it.copy(amountError = error.message) }
                else -> localState.update { it.copy(errorBanner = error.message ?: "Dados inválidos") }
            }
            else -> localState.update { it.copy(errorBanner = error.toUserMessage()) }
        }
    }

    // ------------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------------

    fun delete(id: Long) {
        viewModelScope.launch {
            deleteIncome(id).onFailure { error ->
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
        val form: IncomeFormState? = null,
        val isSubmitting: Boolean = false,
    )
}

data class IncomeUiState(
    val incomes: List<Income> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorBanner: String? = null,
    val form: IncomeFormState? = null,
    val isSubmitting: Boolean = false,
)

data class IncomeFormState(
    /** null = criação; != null = editando este registro. */
    val editingId: Long? = null,
    val source: String = "",
    val amountInput: String = "",
    val incomeType: IncomeType = IncomeType.SALARY,
    /** Flag do dashboard (single): conta como recorrente mensal. */
    val recurring: Boolean = true,
    // --- Recorrência (schedule) — só no modo criação ---
    val isRecurrent: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.MONTHLY,
    val startDate: Instant = Instant.now(),
    val endDate: Instant? = null,
    val installmentsInput: String = "12",
    val sourceError: String? = null,
    val amountError: String? = null,
) {
    val isEditing: Boolean get() = editingId != null

    val installmentsValid: Boolean
        get() = (installmentsInput.toIntOrNull() ?: 0) > 0

    val canSubmit: Boolean
        get() = source.isNotBlank() && amountInput.isNotBlank() &&
            (!isRecurrent || recurrenceType != RecurrenceType.INSTALLMENTS || installmentsValid)
}
