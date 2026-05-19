package com.lifeforge.presentation.screen.optimization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.OptimizationResult
import com.lifeforge.domain.model.RebalanceResult
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.usecase.OptimizeContributionUseCase
import com.lifeforge.domain.usecase.OptimizeHorizonUseCase
import com.lifeforge.domain.usecase.RebalanceUseCase
import com.lifeforge.presentation.common.parseCurrencyInputAsDouble
import com.lifeforge.presentation.common.sanitizeCurrencyInput
import com.lifeforge.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel da tela de Otimização.
 *
 * Gerencia 3 forms independentes (um para cada modo: contribution,
 * horizon, rebalance) e seus respectivos resultados. O modo
 * selecionado é parte do estado para sobreviver à recomposição.
 *
 * Resultados ficam em campos separados (`contributionResult`,
 * `horizonResult`, `rebalanceResult`) para que trocar de aba não
 * apague o resultado anterior — o usuário pode rodar uma otimização
 * de aporte, ver, ir pra outra aba, voltar e o resultado ainda
 * estará lá.
 */
@HiltViewModel
class OptimizationViewModel @Inject constructor(
    private val optimizeContribution: OptimizeContributionUseCase,
    private val optimizeHorizon: OptimizeHorizonUseCase,
    private val rebalance: RebalanceUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OptimizationUiState())
    val state: StateFlow<OptimizationUiState> = _state.asStateFlow()

    fun selectMode(mode: OptimizationMode) {
        _state.update { it.copy(selectedMode = mode, errorBanner = null) }
    }

    fun onErrorBannerDismiss() {
        _state.update { it.copy(errorBanner = null) }
    }

    // ------------------------------------------------------------------------
    // Form de Contribution
    // ------------------------------------------------------------------------

    fun onContributionForm(mutate: (ContributionForm) -> ContributionForm) {
        _state.update { it.copy(contributionForm = mutate(it.contributionForm)) }
    }

    fun runContribution() {
        val form = _state.value.contributionForm
        val initialCapital = parseCurrencyInputAsDouble(form.initialCapital) ?: 0.0
        val expectedReturn = parseCurrencyInputAsDouble(form.expectedReturnAnnual)
        val volatility = parseCurrencyInputAsDouble(form.volatilityAnnual)
        val targetAmount = parseCurrencyInputAsDouble(form.targetAmount)
        val horizon = form.horizonMonths.toIntOrNull()
        val targetProb = parseCurrencyInputAsDouble(form.targetSuccessProbability)

        if (expectedReturn == null || volatility == null || targetAmount == null
            || horizon == null || targetProb == null) {
            _state.update { it.copy(errorBanner = "Preencha todos os campos corretamente") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isRunning = true, errorBanner = null) }
            val result = optimizeContribution(
                initialCapital = initialCapital,
                expectedReturnAnnual = expectedReturn,
                volatilityAnnual = volatility,
                targetAmount = targetAmount,
                horizonMonths = horizon,
                targetSuccessProbability = targetProb,
            )
            handleOptimizationResult(result) { res ->
                _state.update { it.copy(contributionResult = res) }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Form de Horizon
    // ------------------------------------------------------------------------

    fun onHorizonForm(mutate: (HorizonForm) -> HorizonForm) {
        _state.update { it.copy(horizonForm = mutate(it.horizonForm)) }
    }

    fun runHorizon() {
        val form = _state.value.horizonForm
        val initialCapital = parseCurrencyInputAsDouble(form.initialCapital) ?: 0.0
        val expectedReturn = parseCurrencyInputAsDouble(form.expectedReturnAnnual)
        val volatility = parseCurrencyInputAsDouble(form.volatilityAnnual)
        val targetAmount = parseCurrencyInputAsDouble(form.targetAmount)
        val monthly = parseCurrencyInputAsDouble(form.monthlyContribution)
        val targetProb = parseCurrencyInputAsDouble(form.targetSuccessProbability)

        if (expectedReturn == null || volatility == null || targetAmount == null
            || monthly == null || targetProb == null) {
            _state.update { it.copy(errorBanner = "Preencha todos os campos corretamente") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isRunning = true, errorBanner = null) }
            val result = optimizeHorizon(
                initialCapital = initialCapital,
                expectedReturnAnnual = expectedReturn,
                volatilityAnnual = volatility,
                targetAmount = targetAmount,
                monthlyContribution = monthly,
                targetSuccessProbability = targetProb,
            )
            handleOptimizationResult(result) { res ->
                _state.update { it.copy(horizonResult = res) }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Form de Rebalance
    // ------------------------------------------------------------------------

    fun onRebalanceForm(mutate: (RebalanceForm) -> RebalanceForm) {
        _state.update { it.copy(rebalanceForm = mutate(it.rebalanceForm)) }
    }

    fun runRebalance() {
        val form = _state.value.rebalanceForm
        val currentCapital = parseCurrencyInputAsDouble(form.currentCapital) ?: 0.0
        val targetAmount = parseCurrencyInputAsDouble(form.targetAmount)
        val months = form.monthsToGoal.toIntOrNull()

        if (targetAmount == null || months == null) {
            _state.update { it.copy(errorBanner = "Preencha todos os campos corretamente") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isRunning = true, errorBanner = null) }
            when (val result = rebalance(
                riskProfile = form.riskProfile,
                currentCapital = currentCapital,
                targetAmount = targetAmount,
                monthsToGoal = months,
            )) {
                is DataResult.Success -> _state.update {
                    it.copy(rebalanceResult = result.data, isRunning = false)
                }
                is DataResult.Failure -> {
                    _state.update {
                        it.copy(
                            errorBanner = mapError(result.error),
                            isRunning = false,
                        )
                    }
                }
            }
        }
    }

    private fun handleOptimizationResult(
        result: DataResult<OptimizationResult>,
        onSuccess: (OptimizationResult) -> Unit,
    ) {
        when (result) {
            is DataResult.Success -> {
                onSuccess(result.data)
                _state.update { it.copy(isRunning = false) }
            }
            is DataResult.Failure -> _state.update {
                it.copy(errorBanner = mapError(result.error), isRunning = false)
            }
        }
    }

    private fun mapError(error: AppError): String = when (error) {
        is AppError.Validation -> error.message ?: "Dados inválidos"
        else -> error.toUserMessage()
    }
}

enum class OptimizationMode { CONTRIBUTION, HORIZON, REBALANCE }

data class OptimizationUiState(
    val selectedMode: OptimizationMode = OptimizationMode.CONTRIBUTION,
    val isRunning: Boolean = false,
    val errorBanner: String? = null,
    val contributionForm: ContributionForm = ContributionForm(),
    val contributionResult: OptimizationResult? = null,
    val horizonForm: HorizonForm = HorizonForm(),
    val horizonResult: OptimizationResult? = null,
    val rebalanceForm: RebalanceForm = RebalanceForm(),
    val rebalanceResult: RebalanceResult? = null,
)

/**
 * Forms de otimização — todos os campos como String para permitir
 * input livre, parse acontece na hora de submeter. Valores default
 * pre-populados refletem cenários típicos brasileiros (Selic ~8%,
 * volatilidade Ibovespa ~20%).
 */

data class ContributionForm(
    val initialCapital: String = "10000",
    val expectedReturnAnnual: String = "0,08",
    val volatilityAnnual: String = "0,15",
    val targetAmount: String = "500000",
    val horizonMonths: String = "120",
    val targetSuccessProbability: String = "0,90",
) {
    val canRun: Boolean
        get() = expectedReturnAnnual.isNotBlank() && volatilityAnnual.isNotBlank() &&
            targetAmount.isNotBlank() && horizonMonths.isNotBlank() &&
            targetSuccessProbability.isNotBlank()
}

data class HorizonForm(
    val initialCapital: String = "10000",
    val expectedReturnAnnual: String = "0,08",
    val volatilityAnnual: String = "0,15",
    val targetAmount: String = "500000",
    val monthlyContribution: String = "2000",
    val targetSuccessProbability: String = "0,90",
) {
    val canRun: Boolean
        get() = expectedReturnAnnual.isNotBlank() && volatilityAnnual.isNotBlank() &&
            targetAmount.isNotBlank() && monthlyContribution.isNotBlank() &&
            targetSuccessProbability.isNotBlank()
}

data class RebalanceForm(
    val riskProfile: RiskProfile = RiskProfile.MODERATE,
    val currentCapital: String = "50000",
    val targetAmount: String = "500000",
    val monthsToGoal: String = "120",
) {
    val canRun: Boolean
        get() = targetAmount.isNotBlank() && monthsToGoal.isNotBlank()
}

/** Helper para campos do form aplicar sanitização compartilhada. */
fun String.asMonetaryInput(): String = sanitizeCurrencyInput(this)
