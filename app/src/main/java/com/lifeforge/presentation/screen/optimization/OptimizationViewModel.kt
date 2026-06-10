package com.lifeforge.presentation.screen.optimization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.OptimizationResult
import com.lifeforge.domain.model.RebalanceResult
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.usecase.GetReferenceDataUseCase
import com.lifeforge.domain.usecase.ObserveAssetsUseCase
import com.lifeforge.domain.usecase.ObserveGoalsUseCase
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
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.math.max

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
    observeGoals: ObserveGoalsUseCase,
    observeAssets: ObserveAssetsUseCase,
    private val getReferenceData: GetReferenceDataUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OptimizationUiState())
    val state: StateFlow<OptimizationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeGoals().collect { goals -> _state.update { it.copy(goals = goals) } }
        }
        viewModelScope.launch {
            observeAssets().collect { assets ->
                val total = assets.fold(BigDecimal.ZERO) { acc, a -> acc + a.currentValue }
                _state.update { it.copy(totalAssets = total) }
            }
        }
        loadCdiDefaults()
    }

    /**
     * Retorno/volatilidade padrão = 100% do CDI (base de referência), o
     * cenário mais seguro hoje. Só substitui campos ainda nos defaults
     * estáticos — nunca o que o usuário digitou.
     */
    private fun loadCdiDefaults() {
        viewModelScope.launch {
            val ref = (getReferenceData() as? DataResult.Success)?.data ?: return@launch
            val cdi = fraction(ref.cdiAnnual)
            val cdiVol = fraction(ref.cdiVolatilityAnnual)
            val cDef = ContributionForm()
            val hDef = HorizonForm()
            _state.update { s ->
                s.copy(
                    contributionForm = s.contributionForm.copy(
                        expectedReturnAnnual = s.contributionForm.expectedReturnAnnual
                            .ifDefault(cDef.expectedReturnAnnual, cdi),
                        volatilityAnnual = s.contributionForm.volatilityAnnual
                            .ifDefault(cDef.volatilityAnnual, cdiVol),
                    ),
                    horizonForm = s.horizonForm.copy(
                        expectedReturnAnnual = s.horizonForm.expectedReturnAnnual
                            .ifDefault(hDef.expectedReturnAnnual, cdi),
                        volatilityAnnual = s.horizonForm.volatilityAnnual
                            .ifDefault(hDef.volatilityAnnual, cdiVol),
                    ),
                )
            }
        }
    }

    /** Preenche o capital do modo ativo com o patrimônio total (soma dos ativos). */
    fun useTotalAssets(mode: OptimizationMode) {
        val total = _state.value.totalAssets ?: return
        val input = total.toPlainString().replace('.', ',')
        _state.update { s ->
            when (mode) {
                OptimizationMode.CONTRIBUTION ->
                    s.copy(contributionForm = s.contributionForm.copy(initialCapital = input))
                OptimizationMode.HORIZON ->
                    s.copy(horizonForm = s.horizonForm.copy(initialCapital = input))
                OptimizationMode.REBALANCE ->
                    s.copy(rebalanceForm = s.rebalanceForm.copy(currentCapital = input))
            }
        }
    }

    /**
     * Importa uma meta para o modo ativo: valor da meta e, quando o modo tem
     * horizonte como ENTRADA (Aporte/Carteira), os meses até a data alvo.
     * No modo Horizonte o prazo é a SAÍDA do cálculo — só o valor entra.
     */
    fun applyGoal(mode: OptimizationMode, goal: Goal) {
        val amount = goal.targetAmount.toPlainString().replace('.', ',')
        val months = monthsBetween(Instant.now(), goal.targetDate).toString()
        _state.update { s ->
            when (mode) {
                OptimizationMode.CONTRIBUTION -> s.copy(
                    contributionForm = s.contributionForm.copy(
                        targetAmount = amount,
                        horizonMonths = months,
                        selectedGoalName = goal.name,
                    ),
                )
                OptimizationMode.HORIZON -> s.copy(
                    horizonForm = s.horizonForm.copy(
                        targetAmount = amount,
                        selectedGoalName = goal.name,
                    ),
                )
                OptimizationMode.REBALANCE -> s.copy(
                    rebalanceForm = s.rebalanceForm.copy(
                        targetAmount = amount,
                        monthsToGoal = months,
                        selectedGoalName = goal.name,
                    ),
                )
            }
        }
    }

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

    companion object {
        /** Fração anual → input com vírgula decimal (0.005 → "0,005"). */
        private fun fraction(value: Double): String =
            BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',')

        /** Troca pelo [calibrated] apenas se o campo ainda está no default estático. */
        private fun String.ifDefault(default: String, calibrated: String): String =
            if (this == default) calibrated else this

        /** Calcula meses entre dois instantes (mínimo 1). */
        private fun monthsBetween(from: Instant, to: Instant): Int {
            val days = Duration.between(from, to).toDays()
            return max(1, (days / 30).toInt())
        }
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
    /** Metas do usuário — para importar valor/prazo nos forms. */
    val goals: List<Goal> = emptyList(),
    /** Soma dos ativos cadastrados — habilita "usar patrimônio total". */
    val totalAssets: BigDecimal? = null,
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
    /** Nome da meta importada (só exibição). */
    val selectedGoalName: String? = null,
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
    /** Nome da meta importada (só exibição). */
    val selectedGoalName: String? = null,
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
    /** Nome da meta importada (só exibição). */
    val selectedGoalName: String? = null,
) {
    val canRun: Boolean
        get() = targetAmount.isNotBlank() && monthsToGoal.isNotBlank()
}

/** Helper para campos do form aplicar sanitização compartilhada. */
fun String.asMonetaryInput(): String = sanitizeCurrencyInput(this)
