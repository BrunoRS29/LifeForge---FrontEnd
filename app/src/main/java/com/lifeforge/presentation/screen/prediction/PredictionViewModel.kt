package com.lifeforge.presentation.screen.prediction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.ExpensePrediction
import com.lifeforge.domain.model.IncomePrediction
import com.lifeforge.domain.model.WealthPrediction
import com.lifeforge.domain.usecase.PredictExpensesUseCase
import com.lifeforge.domain.usecase.PredictIncomeUseCase
import com.lifeforge.domain.usecase.PredictWealthUseCase
import com.lifeforge.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel da tela de predicoes standalone (Sprint 5).
 *
 * Esta tela existe para o usuario ESPIAR as predicoes do modelo sem
 * disparar uma simulacao Monte Carlo completa. Util para:
 *  - Validar se as predicoes "fazem sentido" antes de calibrar
 *  - Entender que historico esta sendo usado (transparencia de ML)
 *  - Diagnosticar resultados ruins de simulacao calibrada
 *
 * Padroes seguidos:
 *  - StateFlow exposto, MutableStateFlow privado (encapsulamento)
 *  - Cada botao tem seu proprio loading independente (so trava o card
 *    correspondente, nao a tela inteira)
 *  - Erros mostrados como banner, dispensavel
 *  - Sem persistencia local - cada visita roda de novo se o usuario
 *    quiser dados frescos
 */
@HiltViewModel
class PredictionViewModel @Inject constructor(
    private val predictIncome: PredictIncomeUseCase,
    private val predictExpenses: PredictExpensesUseCase,
    private val predictWealth: PredictWealthUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PredictionUiState())
    val state: StateFlow<PredictionUiState> = _state.asStateFlow()

    // ------------------------------------------------------------------------
    // Form mutations
    // ------------------------------------------------------------------------

    fun onIncomeHorizonChange(months: Int) {
        _state.update {
            it.copy(incomeHorizonMonths = months.coerceIn(1, 60), errorBanner = null)
        }
    }

    fun onExpenseHorizonChange(months: Int) {
        _state.update {
            it.copy(expenseHorizonMonths = months.coerceIn(1, 12), errorBanner = null)
        }
    }

    fun onWealthHorizonChange(months: Int) {
        _state.update {
            it.copy(wealthHorizonMonths = months.coerceIn(1, 60), errorBanner = null)
        }
    }

    fun onErrorBannerDismiss() {
        _state.update { it.copy(errorBanner = null) }
    }

    // ------------------------------------------------------------------------
    // Acoes
    // ------------------------------------------------------------------------

    fun runPredictIncome() {
        if (_state.value.isPredictingIncome) return
        viewModelScope.launch {
            _state.update { it.copy(isPredictingIncome = true, errorBanner = null) }
            when (val r = predictIncome(_state.value.incomeHorizonMonths)) {
                is DataResult.Success -> _state.update {
                    it.copy(isPredictingIncome = false, incomePrediction = r.data)
                }
                is DataResult.Failure -> _state.update {
                    it.copy(
                        isPredictingIncome = false,
                        errorBanner = r.error.toUserMessage(),
                    )
                }
            }
        }
    }

    fun runPredictExpenses() {
        if (_state.value.isPredictingExpenses) return
        viewModelScope.launch {
            _state.update { it.copy(isPredictingExpenses = true, errorBanner = null) }
            when (val r = predictExpenses(_state.value.expenseHorizonMonths)) {
                is DataResult.Success -> _state.update {
                    it.copy(isPredictingExpenses = false, expensePrediction = r.data)
                }
                is DataResult.Failure -> _state.update {
                    it.copy(
                        isPredictingExpenses = false,
                        errorBanner = r.error.toUserMessage(),
                    )
                }
            }
        }
    }

    fun runPredictWealth() {
        if (_state.value.isPredictingWealth) return
        viewModelScope.launch {
            _state.update { it.copy(isPredictingWealth = true, errorBanner = null) }
            when (val r = predictWealth(_state.value.wealthHorizonMonths)) {
                is DataResult.Success -> _state.update {
                    it.copy(isPredictingWealth = false, wealthPrediction = r.data)
                }
                is DataResult.Failure -> _state.update {
                    it.copy(
                        isPredictingWealth = false,
                        errorBanner = r.error.toUserMessage(),
                    )
                }
            }
        }
    }
}

/**
 * Estado da tela de predicoes.
 *
 * Cada card eh independente - flags de loading separadas. Predicoes
 * anteriores PERMANECEM visiveis enquanto uma nova roda (sem flash
 * de tela em branco) - so sao substituidas no Success.
 */
data class PredictionUiState(
    val incomeHorizonMonths: Int = 12,
    val expenseHorizonMonths: Int = 1,
    val wealthHorizonMonths: Int = 12,
    val isPredictingIncome: Boolean = false,
    val isPredictingExpenses: Boolean = false,
    val isPredictingWealth: Boolean = false,
    val incomePrediction: IncomePrediction? = null,
    val expensePrediction: ExpensePrediction? = null,
    val wealthPrediction: WealthPrediction? = null,
    val errorBanner: String? = null,
)
