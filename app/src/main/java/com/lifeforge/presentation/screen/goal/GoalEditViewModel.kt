package com.lifeforge.presentation.screen.goal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.domain.usecase.CreateGoalUseCase
import com.lifeforge.domain.usecase.ObserveGoalUseCase
import com.lifeforge.domain.usecase.UpdateGoalUseCase
import com.lifeforge.presentation.common.toUserMessage
import com.lifeforge.presentation.navigation.GoalEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel compartilhado entre criação e edição. Diferenciação:
 * `goalId == null` → modo criação; caso contrário, modo edição.
 *
 * Em modo edição, carrega o Goal atual via `ObserveGoalUseCase.first()`
 * e popula o estado uma única vez. Edições posteriores no banco
 * (refresh em background) **não** sobrescrevem o que o usuário está
 * digitando — comportamento esperado em forms.
 */
@HiltViewModel
class GoalEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeGoal: ObserveGoalUseCase,
    private val createGoal: CreateGoalUseCase,
    private val updateGoal: UpdateGoalUseCase,
) : ViewModel() {

    private val goalId: Long? = savedStateHandle.toRoute<GoalEdit>().goalId

    private val _state = MutableStateFlow(
        GoalEditUiState(isEdit = goalId != null, isLoading = goalId != null)
    )
    val state: StateFlow<GoalEditUiState> = _state.asStateFlow()

    private val events = Channel<GoalEditEvent>(Channel.BUFFERED)
    val eventsFlow = events.receiveAsFlow()

    init {
        if (goalId != null) loadExistingGoal(goalId)
    }

    private fun loadExistingGoal(id: Long) {
        viewModelScope.launch {
            // Coleta a primeira emissão do Flow do Room para popular o
            // form. Não mantemos coleta contínua para não sobrescrever
            // edições que o usuário esteja fazendo.
            val goal = observeGoal(id).first()
            if (goal != null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        name = goal.name,
                        category = goal.category,
                        targetAmountInput = goal.targetAmount.toPlainString(),
                        targetDate = goal.targetDate,
                        priority = goal.priority,
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorBanner = "Meta não encontrada",
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _state.update { it.copy(name = name, nameError = null, errorBanner = null) }
    }

    fun onCategoryChange(category: GoalCategory) {
        _state.update { it.copy(category = category) }
    }

    fun onTargetAmountChange(input: String) {
        // Aceita apenas dígitos, vírgula e ponto. Outros chars são ignorados
        // — evita o usuário inserir letras por engano.
        val sanitized = input.filter { it.isDigit() || it == ',' || it == '.' }
        _state.update {
            it.copy(targetAmountInput = sanitized, targetAmountError = null, errorBanner = null)
        }
    }

    fun onTargetDateChange(date: Instant) {
        _state.update { it.copy(targetDate = date, targetDateError = null, errorBanner = null) }
    }

    fun onPriorityChange(priority: Int) {
        _state.update { it.copy(priority = priority) }
    }

    fun onErrorBannerDismiss() {
        _state.update { it.copy(errorBanner = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return

        // Parse local do BigDecimal — entrada PT-BR usa vírgula como
        // decimal. Trocamos para ponto antes do BigDecimal aceitar.
        val targetAmount = try {
            val normalized = current.targetAmountInput
                .replace(".", "")  // remove separadores de milhar
                .replace(",", ".") // troca vírgula decimal por ponto
            BigDecimal(normalized)
        } catch (e: NumberFormatException) {
            _state.update {
                it.copy(targetAmountError = "Valor inválido")
            }
            return
        }

        val targetDate = current.targetDate
        if (targetDate == null) {
            _state.update { it.copy(targetDateError = "Selecione uma data") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorBanner = null) }

            val result = if (goalId == null) {
                createGoal(current.name, current.category, targetAmount, targetDate, current.priority)
            } else {
                updateGoal(goalId, current.name, current.category, targetAmount, targetDate, current.priority)
            }

            when (result) {
                is DataResult.Success -> events.send(GoalEditEvent.SavedAndNavigateBack)
                is DataResult.Failure -> handleError(result.error)
            }

            _state.update { it.copy(isSubmitting = false) }
        }
    }

    private fun handleError(error: AppError) {
        when (error) {
            is AppError.Validation -> when (error.field) {
                "name" -> _state.update { it.copy(nameError = error.message) }
                "targetAmount" -> _state.update { it.copy(targetAmountError = error.message) }
                "targetDate" -> _state.update { it.copy(targetDateError = error.message) }
                else -> _state.update {
                    it.copy(errorBanner = error.message ?: "Dados inválidos")
                }
            }
            else -> _state.update { it.copy(errorBanner = error.toUserMessage()) }
        }
    }
}

data class GoalEditUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val name: String = "",
    val category: GoalCategory = GoalCategory.CUSTOM,
    val targetAmountInput: String = "",
    val targetDate: Instant? = null,
    val priority: Int = 5,
    val nameError: String? = null,
    val targetAmountError: String? = null,
    val targetDateError: String? = null,
    val errorBanner: String? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && !isLoading && name.isNotBlank() &&
            targetAmountInput.isNotBlank() && targetDate != null
}

sealed interface GoalEditEvent {
    data object SavedAndNavigateBack : GoalEditEvent
}
