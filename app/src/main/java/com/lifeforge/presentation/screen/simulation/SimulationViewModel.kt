package com.lifeforge.presentation.screen.simulation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.SimulationParameters
import com.lifeforge.domain.model.SimulationResult
import com.lifeforge.domain.model.SimulationSummary
import com.lifeforge.domain.usecase.GetReferenceDataUseCase
import com.lifeforge.domain.usecase.GetSimulationUseCase
import com.lifeforge.domain.usecase.GetUserProfileUseCase
import com.lifeforge.domain.usecase.ObserveAssetsUseCase
import com.lifeforge.domain.usecase.ObserveGoalUseCase
import com.lifeforge.domain.usecase.ObserveSimulationsByGoalUseCase
import com.lifeforge.domain.usecase.RefreshSimulationsByGoalUseCase
import com.lifeforge.domain.usecase.RunSimulationUseCase
import com.lifeforge.presentation.common.parseCurrencyInputAsDouble
import com.lifeforge.presentation.common.sanitizeCurrencyInput
import com.lifeforge.presentation.common.toUserMessage
import com.lifeforge.presentation.navigation.Simulation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.math.max

/**
 * ViewModel da tela de simulação Monte Carlo.
 *
 * Carrega a meta uma única vez no `init` para pré-popular os parâmetros
 * derivados (`targetAmount` da meta, `horizonMonths` calculado entre
 * agora e `targetDate`). Demais parâmetros (capital inicial, aporte
 * mensal, retorno, volatilidade, etc.) começam com defaults razoáveis
 * para um cenário brasileiro — usuário ajusta nos sliders.
 *
 * O Flow de simulações anteriores da meta é observado via
 * `ObserveSimulationsByGoalUseCase` e renderizado embaixo do form,
 * permitindo comparar execuções recentes.
 *
 * Cada `run()` valida o conjunto (mín. 10k iterações pela especificação
 * do TCC), executa via `RunSimulationUseCase` (~1-3s no backend) e
 * grava o resultado no `result` para a UI renderizar os gráficos Vico.
 */
@HiltViewModel
class SimulationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeGoal: ObserveGoalUseCase,
    observeSimulations: ObserveSimulationsByGoalUseCase,
    private val refreshSimulations: RefreshSimulationsByGoalUseCase,
    private val runSimulation: RunSimulationUseCase,
    private val getSimulation: GetSimulationUseCase,
    private val observeAssets: ObserveAssetsUseCase,
    private val getReferenceData: GetReferenceDataUseCase,
    private val getUserProfile: GetUserProfileUseCase,
) : ViewModel() {

    private val goalId: Long = savedStateHandle.toRoute<Simulation>().goalId

    private val localState = MutableStateFlow(LocalUiState())

    val state: StateFlow<SimulationUiState> = combine(
        observeSimulations(goalId),
        localState,
    ) { history, local ->
        SimulationUiState(
            goalName = local.goalName,
            form = local.form,
            isRunning = local.isRunning,
            result = local.result,
            history = history,
            errorBanner = local.errorBanner,
            totalAssets = local.totalAssets,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SimulationUiState(form = defaultForm()),
    )

    init {
        loadGoalAndPrepopulate()
        loadSafestScenarioDefaults()
        observeTotalAssets()
        viewModelScope.launch { refreshSimulations(goalId) }
    }

    /**
     * Premissas padrão = "cenário mais seguro": retorno de 100% do CDI com a
     * volatilidade do CDI (base de referência) e probabilidade de desemprego
     * do vínculo do usuário (PNAD/base). Só sobrescreve campos que ainda
     * estão nos defaults estáticos — nunca o que o usuário já digitou.
     */
    private fun loadSafestScenarioDefaults() {
        viewModelScope.launch {
            val ref = (getReferenceData() as? DataResult.Success)?.data ?: return@launch
            val employmentType =
                (getUserProfile() as? DataResult.Success)?.data?.employmentType?.name
            val defaults = defaultForm()
            localState.update { current ->
                val f = current.form
                current.copy(
                    form = f.copy(
                        expectedReturnInput = f.expectedReturnInput
                            .ifDefault(defaults.expectedReturnInput, fraction(ref.cdiAnnual)),
                        volatilityInput = f.volatilityInput
                            .ifDefault(defaults.volatilityInput, fraction(ref.cdiVolatilityAnnual)),
                        inflationInput = f.inflationInput
                            .ifDefault(defaults.inflationInput, fraction(ref.inflationAnnualMean)),
                        unemploymentProbInput = f.unemploymentProbInput.ifDefault(
                            defaults.unemploymentProbInput,
                            fraction(ref.unemploymentProbFor(employmentType)),
                        ),
                        unemploymentDurationMonths = ref.unemploymentDurationMonths,
                    ),
                )
            }
        }
    }

    /** Observa os ativos para oferecer "usar patrimônio total" no capital inicial. */
    private fun observeTotalAssets() {
        viewModelScope.launch {
            observeAssets().collect { assets ->
                val total = assets.fold(BigDecimal.ZERO) { acc, a -> acc + a.currentValue }
                localState.update { it.copy(totalAssets = total) }
            }
        }
    }

    /** Preenche o capital inicial com o patrimônio total (soma dos ativos). */
    fun useTotalAssetsAsInitialCapital() {
        val total = localState.value.totalAssets ?: return
        localState.update { current ->
            current.copy(
                form = current.form.copy(initialCapitalInput = total.toInputString()),
            )
        }
    }

    /**
     * Coleta a meta uma única vez para pré-popular targetAmount e
     * horizonMonths. Não mantém coleta contínua — se a meta for editada
     * em outra tela enquanto o usuário está aqui, o form não sobrescreve
     * a digitação atual.
     */
    private fun loadGoalAndPrepopulate() {
        viewModelScope.launch {
            val goal = observeGoal(goalId).first() ?: run {
                localState.update {
                    it.copy(errorBanner = "Meta não encontrada")
                }
                return@launch
            }

            val months = monthsBetween(Instant.now(), goal.targetDate)
            localState.update { current ->
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

    fun onFormChange(mutate: (SimulationForm) -> SimulationForm) {
        localState.update { it.copy(form = mutate(it.form), errorBanner = null) }
    }

    fun onErrorBannerDismiss() {
        localState.update { it.copy(errorBanner = null) }
    }

    /**
     * Reabre uma rodada do histórico: carrega o resultado completo (cache
     * local ou backend) e o exibe na mesma seção de resultado da tela.
     * Obs.: rodadas vindas do cache não guardam a trajetória do fan chart —
     * os demais gráficos e estatísticas aparecem normalmente.
     */
    fun openHistoryEntry(simulationId: Long) {
        if (localState.value.isRunning) return
        viewModelScope.launch {
            localState.update { it.copy(isRunning = true, errorBanner = null) }
            when (val result = getSimulation(simulationId)) {
                is DataResult.Success -> localState.update {
                    it.copy(isRunning = false, result = result.data)
                }
                is DataResult.Failure -> localState.update {
                    it.copy(isRunning = false, errorBanner = mapError(result.error))
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Execução da simulação
    // ------------------------------------------------------------------------

    fun runSimulation() {
        val form = localState.value.form
        if (localState.value.isRunning) return

        val params = parseForm(form) ?: return

        viewModelScope.launch {
            localState.update { it.copy(isRunning = true, errorBanner = null) }

            when (val result = runSimulation.invoke(params)) {
                is DataResult.Success -> localState.update {
                    it.copy(isRunning = false, result = result.data)
                }
                is DataResult.Failure -> localState.update {
                    it.copy(isRunning = false, errorBanner = mapError(result.error))
                }
            }
        }
    }

    /**
     * Parse e validação dos campos do form. Retorna `null` e atualiza
     * `errorBanner` se algum campo for inválido.
     */
    private fun parseForm(form: SimulationForm): SimulationParameters? {
        val initialCapital = parseCurrencyInputAsDouble(form.initialCapitalInput)
        val monthly = parseCurrencyInputAsDouble(form.monthlyContributionInput)
        val expectedReturn = parseCurrencyInputAsDouble(form.expectedReturnInput)
        val volatility = parseCurrencyInputAsDouble(form.volatilityInput)
        val targetAmount = parseCurrencyInputAsDouble(form.targetAmountInput)
        val horizon = form.horizonMonthsInput.toIntOrNull()
        val unemploymentProb = parseCurrencyInputAsDouble(form.unemploymentProbInput) ?: 0.0
        val inflation = parseCurrencyInputAsDouble(form.inflationInput) ?: 0.0

        if (initialCapital == null || monthly == null || expectedReturn == null
            || volatility == null || targetAmount == null || horizon == null) {
            localState.update {
                it.copy(errorBanner = "Verifique os valores numéricos do formulário")
            }
            return null
        }

        return SimulationParameters(
            goalId = goalId,
            initialCapital = initialCapital,
            monthlyContribution = monthly,
            expectedReturnAnnual = expectedReturn,
            volatilityAnnual = volatility,
            horizonMonths = horizon,
            targetAmount = targetAmount,
            unemploymentProbAnnual = unemploymentProb,
            unemploymentDurationMonths = form.unemploymentDurationMonths,
            inflationAnnual = inflation,
            numSimulations = form.numSimulations,
            seed = null,  // backend gera seed aleatório se null
        )
    }

    private fun mapError(error: AppError): String = when (error) {
        is AppError.Validation -> error.message ?: "Parâmetros inválidos"
        else -> error.toUserMessage()
    }

    private data class LocalUiState(
        val goalName: String? = null,
        val form: SimulationForm = defaultForm(),
        val isRunning: Boolean = false,
        val result: SimulationResult? = null,
        val errorBanner: String? = null,
        val totalAssets: BigDecimal? = null,
    )

    companion object {
        /**
         * Defaults para o formulário. Refletem um cenário brasileiro
         * razoável: poupador com R$ 10k, aportando R$ 1k/mês, esperando
         * 8% a.a. (próximo da Selic) com 15% de volatilidade. Inflação
         * 4% e probabilidade de desemprego 5% a.a. cobrem eventos
         * adversos realistas.
         */
        private fun defaultForm() = SimulationForm(
            initialCapitalInput = "10000",
            monthlyContributionInput = "1000",
            expectedReturnInput = "0,08",
            volatilityInput = "0,15",
            targetAmountInput = "100000",
            horizonMonthsInput = "120",
            unemploymentProbInput = "0,05",
            unemploymentDurationMonths = 6,
            inflationInput = "0,04",
            numSimulations = 10_000,
        )

        /** Calcula meses entre dois instantes (mínimo 1). */
        private fun monthsBetween(from: Instant, to: Instant): Int {
            val days = Duration.between(from, to).toDays()
            return max(1, (days / 30).toInt())
        }

        /** Fração anual → input com vírgula decimal (0.005 → "0,005"). */
        private fun fraction(value: Double): String =
            BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',')

        /** BigDecimal → input monetário com vírgula decimal. */
        private fun BigDecimal.toInputString(): String = toPlainString().replace('.', ',')

        /** Troca pelo [calibrated] apenas se o campo ainda está no default estático. */
        private fun String.ifDefault(default: String, calibrated: String): String =
            if (this == default) calibrated else this
    }
}

data class SimulationForm(
    val initialCapitalInput: String,
    val monthlyContributionInput: String,
    val expectedReturnInput: String,
    val volatilityInput: String,
    val targetAmountInput: String,
    val horizonMonthsInput: String,
    val unemploymentProbInput: String,
    val unemploymentDurationMonths: Int,
    val inflationInput: String,
    /**
     * Mínimo 10.000 conforme especificação técnica do TCC. UI expõe
     * via Slider entre 10k e 50k — mais que isso o ganho marginal
     * é pequeno e o tempo de execução cresce linearmente.
     */
    val numSimulations: Int,
) {
    val canRun: Boolean
        get() = initialCapitalInput.isNotBlank() &&
            monthlyContributionInput.isNotBlank() &&
            expectedReturnInput.isNotBlank() &&
            volatilityInput.isNotBlank() &&
            targetAmountInput.isNotBlank() &&
            horizonMonthsInput.isNotBlank()
}

data class SimulationUiState(
    val goalName: String? = null,
    val form: SimulationForm,
    val isRunning: Boolean = false,
    val result: SimulationResult? = null,
    val history: List<SimulationSummary> = emptyList(),
    val errorBanner: String? = null,
    /** Soma dos ativos cadastrados — habilita "usar patrimônio total". */
    val totalAssets: java.math.BigDecimal? = null,
)

/** Helper de sanitização — usado nos callbacks dos campos. */
fun String.asMonetaryInput(): String = sanitizeCurrencyInput(this)
