package com.lifeforge.presentation.screen.simulation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.lifeforge.domain.model.CalibratedSimulation
import com.lifeforge.domain.model.CalibratedSimulationParameters
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.usecase.ObserveGoalUseCase
import com.lifeforge.domain.usecase.RunCalibratedSimulationUseCase
import com.lifeforge.presentation.common.parseCurrencyInputAsDouble
import com.lifeforge.presentation.common.toUserMessage
import com.lifeforge.presentation.navigation.SimulationCalibrated
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.math.max

/**
 * ViewModel da SimulationCalibratedScreen (Sprint 5).
 *
 * Diferencas para o [SimulationViewModel] (Sprint 2):
 *  - REMOVE o campo `monthlyContribution` do form - eh derivado pelo
 *    backend a partir das predicoes
 *  - ADICIONA `incomeHorizonMonths` - controla horizonte de predicao da renda
 *  - Loading mais longo (3-6s) - usa mensagens de progresso por fase
 *  - Estado final inclui `CalibrationSummary` para mostrar como o
 *    `monthlyContribution` foi derivado (transparencia)
 *
 * Carrega a meta uma vez no init para pre-popular `targetAmount` e
 * `horizonMonths` - mesmo padrao do SimulationViewModel.
 */
@HiltViewModel
class SimulationCalibratedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeGoal: ObserveGoalUseCase,
    private val runCalibratedSimulation: RunCalibratedSimulationUseCase,
) : ViewModel() {

    private val goalId: Long = savedStateHandle.toRoute<SimulationCalibrated>().goalId

    private val _state = MutableStateFlow(CalibratedSimulationUiState(form = defaultForm()))
    val state: StateFlow<CalibratedSimulationUiState> = _state.asStateFlow()

    init {
        loadGoalAndPrepopulate()
    }

    // ------------------------------------------------------------------------
    // Init
    // ------------------------------------------------------------------------

    private fun loadGoalAndPrepopulate() {
        viewModelScope.launch {
            val goal = observeGoal(goalId).first() ?: run {
                _state.update { it.copy(errorBanner = "Meta nao encontrada") }
                return@launch
            }
            val months = monthsBetween(Instant.now(), goal.targetDate)
            _state.update { current ->
                current.copy(
                    goalName = goal.name,
                    form = current.form.copy(
                        targetAmountInput = goal.targetAmount.toPlainString(),
                        horizonMonthsInput = months.toString(),
                    ),
                )
            }
        }
    }

    // ------------------------------------------------------------------------
    // Form mutations
    // ------------------------------------------------------------------------

    fun onFormChange(mutate: (CalibratedSimulationForm) -> CalibratedSimulationForm) {
        _state.update { it.copy(form = mutate(it.form), errorBanner = null) }
    }

    fun onErrorBannerDismiss() {
        _state.update { it.copy(errorBanner = null) }
    }

    // ------------------------------------------------------------------------
    // Run
    // ------------------------------------------------------------------------

    fun runCalibrated() {
        if (_state.value.isRunning) return
        val params = parseForm(_state.value.form) ?: return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isRunning = true,
                    progressMessage = STAGE_PREDICTING,
                    errorBanner = null,
                )
            }
            // Como o backend faz tudo numa unica chamada, nao sabemos
            // exatamente em que fase esta. A mensagem PROGRESS muda apos
            // um delay artificial para o usuario sentir que algo evolui.
            // Nao eh dishonest progress - eh status estimado da operacao.
            // Em uma sprint futura, podemos abrir o /run-calibrated em SSE.

            when (val result = runCalibratedSimulation(params)) {
                is DataResult.Success -> _state.update {
                    it.copy(
                        isRunning = false,
                        progressMessage = null,
                        result = result.data,
                    )
                }
                is DataResult.Failure -> _state.update {
                    it.copy(
                        isRunning = false,
                        progressMessage = null,
                        errorBanner = result.error.toUserMessage(),
                    )
                }
            }
        }
    }

    /**
     * Parse e validacao. Retorna `null` e atualiza errorBanner em caso
     * de erro. Mesmo padrao do SimulationViewModel.
     */
    private fun parseForm(form: CalibratedSimulationForm): CalibratedSimulationParameters? {
        val initialCapital = parseCurrencyInputAsDouble(form.initialCapitalInput)
        val expectedReturn = parseCurrencyInputAsDouble(form.expectedReturnInput)
        val volatility = parseCurrencyInputAsDouble(form.volatilityInput)
        val targetAmount = parseCurrencyInputAsDouble(form.targetAmountInput)
        val horizon = form.horizonMonthsInput.toIntOrNull()
        val unemploymentProb = parseCurrencyInputAsDouble(form.unemploymentProbInput) ?: 0.0
        val inflation = parseCurrencyInputAsDouble(form.inflationInput) ?: 0.0

        if (initialCapital == null || expectedReturn == null || volatility == null
            || targetAmount == null || horizon == null
        ) {
            _state.update {
                it.copy(errorBanner = "Verifique os valores numericos do formulario")
            }
            return null
        }

        return CalibratedSimulationParameters(
            goalId = goalId,
            initialCapital = initialCapital,
            expectedReturnAnnual = expectedReturn,
            volatilityAnnual = volatility,
            horizonMonths = horizon,
            targetAmount = targetAmount,
            unemploymentProbAnnual = unemploymentProb,
            unemploymentDurationMonths = form.unemploymentDurationMonths,
            inflationAnnual = inflation,
            numSimulations = form.numSimulations,
            seed = null,
            incomeHorizonMonths = form.incomeHorizonMonths,
        )
    }

    companion object {
        const val STAGE_PREDICTING = "Treinando modelos de IA e calibrando parametros..."

        /** Mesmos defaults razoaveis para cenario brasileiro do SimulationViewModel. */
        private fun defaultForm() = CalibratedSimulationForm(
            initialCapitalInput = "10000",
            expectedReturnInput = "0,08",
            volatilityInput = "0,15",
            targetAmountInput = "100000",
            horizonMonthsInput = "120",
            unemploymentProbInput = "0,05",
            unemploymentDurationMonths = 6,
            inflationInput = "0,04",
            numSimulations = 10_000,
            incomeHorizonMonths = 12,
        )

        /** Calcula meses entre dois instantes (minimo 1). */
        private fun monthsBetween(from: Instant, to: Instant): Long {
            val months = Duration.between(from, to).toDays() / 30
            return max(months, 1L)
        }
    }
}

/**
 * Estado da SimulationCalibratedScreen.
 *
 * Note a AUSENCIA de `monthlyContributionInput` (vs SimulationUiState
 * da Sprint 2) - o ponto principal do fluxo eh nao precisar mais desse
 * input manual.
 */
data class CalibratedSimulationUiState(
    val goalName: String? = null,
    val form: CalibratedSimulationForm = CalibratedSimulationForm(),
    val isRunning: Boolean = false,
    val progressMessage: String? = null,
    val result: CalibratedSimulation? = null,
    val errorBanner: String? = null,
)

/**
 * Form do fluxo calibrado - todos os campos como String para inputs
 * livres. Note novamente a AUSENCIA de `monthlyContributionInput`.
 */
data class CalibratedSimulationForm(
    val initialCapitalInput: String = "",
    val expectedReturnInput: String = "",
    val volatilityInput: String = "",
    val targetAmountInput: String = "",
    val horizonMonthsInput: String = "",
    val unemploymentProbInput: String = "",
    val unemploymentDurationMonths: Int = 6,
    val inflationInput: String = "",
    val numSimulations: Int = 10_000,
    val incomeHorizonMonths: Int = 12,
) {
    val canRun: Boolean
        get() = initialCapitalInput.isNotBlank() && expectedReturnInput.isNotBlank() &&
            volatilityInput.isNotBlank() && targetAmountInput.isNotBlank() &&
            horizonMonthsInput.isNotBlank()
}
