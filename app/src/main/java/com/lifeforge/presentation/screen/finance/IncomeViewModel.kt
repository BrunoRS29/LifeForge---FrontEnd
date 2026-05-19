package com.lifeforge.presentation.screen.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Income
import com.lifeforge.domain.model.IncomeType
import com.lifeforge.domain.model.onFailure
import com.lifeforge.domain.usecase.CreateIncomeUseCase
import com.lifeforge.domain.usecase.DeleteIncomeUseCase
import com.lifeforge.domain.usecase.ObserveIncomesUseCase
import com.lifeforge.domain.usecase.RefreshIncomesUseCase
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
 * Combina o Flow de incomes do Room com o estado local de UI
 * (form modal aberto, refresh, erros). O form de criação fica dentro
 * desta ViewModel — para Income/Expense o form é simples o bastante
 * (sem datepicker, sem categorias hierárquicas) para não justificar
 * uma tela dedicada com ViewModel separada.
 */
@HiltViewModel
class IncomeViewModel @Inject constructor(
    observeIncomes: ObserveIncomesUseCase,
    private val refreshIncomes: RefreshIncomesUseCase,
    private val createIncome: CreateIncomeUseCase,
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

    fun openForm() {
        localState.update { it.copy(form = IncomeFormState()) }
    }

    fun closeForm() {
        localState.update { it.copy(form = null) }
    }

    fun onFormSourceChange(source: String) {
        localState.update { local ->
            local.form?.let { form ->
                local.copy(form = form.copy(source = source, sourceError = null))
            } ?: local
        }
    }

    fun onFormAmountChange(amount: String) {
        val sanitized = sanitizeCurrencyInput(amount)
        localState.update { local ->
            local.form?.let { form ->
                local.copy(form = form.copy(amountInput = sanitized, amountError = null))
            } ?: local
        }
    }

    fun onFormTypeChange(type: IncomeType) {
        localState.update { local ->
            local.form?.let { form ->
                local.copy(form = form.copy(incomeType = type))
            } ?: local
        }
    }

    fun onFormRecurringChange(recurring: Boolean) {
        localState.update { local ->
            local.form?.let { form ->
                local.copy(form = form.copy(recurring = recurring))
            } ?: local
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
            val result = createIncome(
                source = form.source,
                amount = amount,
                incomeType = form.incomeType,
                recurring = form.recurring,
                // Sem datepicker pra Income — assume recebimento "agora"
                // como simplificação. Edição da data fica em sprint futura
                // se for necessário no escopo do TCC.
                receivedAt = Instant.now(),
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
                "source" -> localState.update { local ->
                    local.form?.let { f ->
                        local.copy(form = f.copy(sourceError = error.message))
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
    val source: String = "",
    val amountInput: String = "",
    val incomeType: IncomeType = IncomeType.SALARY,
    val recurring: Boolean = true,
    val sourceError: String? = null,
    val amountError: String? = null,
) {
    val canSubmit: Boolean
        get() = source.isNotBlank() && amountInput.isNotBlank()
}
