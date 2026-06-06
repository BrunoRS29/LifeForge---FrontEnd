package com.lifeforge.presentation.screen.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.usecase.DeleteAllExpensesUseCase
import com.lifeforge.domain.usecase.DeleteAllIncomesUseCase
import com.lifeforge.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel da tela host de Finanças. Cuida das ações de "limpar tudo"
 * (excluir todas as receitas / despesas) — útil ao reimportar extratos.
 * As abas continuam com seus próprios ViewModels; como tudo observa o Room,
 * a exclusão reflete automaticamente nas listas.
 */
@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val deleteIncomes: DeleteAllIncomesUseCase,
    private val deleteExpenses: DeleteAllExpensesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(FinanceUiState())
    val state: StateFlow<FinanceUiState> = _state.asStateFlow()

    fun deleteAllIncomes() = runDelete("receitas") { deleteIncomes() }

    fun deleteAllExpenses() = runDelete("despesas") { deleteExpenses() }

    private fun runDelete(label: String, action: suspend () -> DataResult<Unit>) {
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, message = null, error = null) }
            when (val result = action()) {
                is DataResult.Success -> _state.update {
                    it.copy(isBusy = false, message = "Todas as $label foram excluídas.")
                }
                is DataResult.Failure -> _state.update {
                    it.copy(isBusy = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun onMessageShown() = _state.update { it.copy(message = null, error = null) }
}

data class FinanceUiState(
    val isBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)
