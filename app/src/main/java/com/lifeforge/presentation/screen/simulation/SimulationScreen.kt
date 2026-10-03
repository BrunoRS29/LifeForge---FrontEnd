package com.lifeforge.presentation.screen.simulation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.SimulationInputs
import com.lifeforge.domain.model.SimulationSummary
import com.lifeforge.presentation.common.ActionButton
import com.lifeforge.presentation.common.ConnectedChoice
import com.lifeforge.presentation.common.DetailTopAppBar
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.ExpandableSection
import com.lifeforge.presentation.common.FormSection
import com.lifeforge.presentation.common.MoneyField
import com.lifeforge.presentation.common.MonthsField
import com.lifeforge.presentation.common.PercentField
import com.lifeforge.presentation.common.ProgressActionBar
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.SectionHeader
import com.lifeforge.presentation.common.UseTotalAssetsChip
import com.lifeforge.presentation.common.formatAnnualRate
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatCount
import com.lifeforge.presentation.common.formatDateTime
import com.lifeforge.presentation.common.formatHorizon
import com.lifeforge.presentation.common.formatProbability
import com.lifeforge.presentation.common.readableWidth

/**
 * Tela de Simulação de Monte Carlo.
 *
 * De cima para baixo: os parâmetros em seções (meta e prazo, seu dinheiro,
 * premissas de mercado recolhidas — já vêm da base de referência —, quantidade de
 * cenários), o resultado e o histórico de rodadas da meta. "Simular" fica fixo na
 * base; quando o resultado chega, a tela rola até ele.
 *
 * Os gráficos (Vico) e o medidor ficam em [ResultSection], compartilhado com a
 * simulação calibrada pela IA.
 */
@Composable
fun SimulationScreen(
    onNavigateBack: () -> Unit,
    onCompare: (goalId: Long, firstId: Long, secondId: Long) -> Unit = { _, _, _ -> },
    viewModel: SimulationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()
    var resultTop by remember { mutableIntStateOf(0) }

    ScrollToResult(resultId = state.result?.id, scrollState = scrollState, resultTop = { resultTop })

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopAppBar(
                title = "Simulação",
                subtitle = state.goalName,
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
                navigationEnabled = !state.isRunning,
            )
        },
        bottomBar = {
            ProgressActionBar(
                isRunning = state.isRunning,
                runningText = "Simulando ${formatCount(state.form.numSimulations)} cenários…",
                idleText = "Simular",
                enabled = state.form.canRun,
                onClick = viewModel::runSimulation,
                icon = Icons.Outlined.AutoGraph,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .readableWidth()
                    .padding(horizontal = ScreenPadding)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (state.errorBanner != null) {
                    ErrorBanner(message = state.errorBanner!!, onDismiss = viewModel::onErrorBannerDismiss)
                }

                ParameterForm(
                    form = state.form,
                    isRunning = state.isRunning,
                    onChange = viewModel::onFormChange,
                    totalAssets = state.totalAssets,
                    onUseTotalAssets = viewModel::useTotalAssetsAsInitialCapital,
                )

                // Entrada animada do resultado — o bloco "desliza" para a tela.
                AnimatedVisibility(
                    visible = state.result != null,
                    enter = fadeIn() + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    modifier = Modifier.onPlaced { resultTop = it.positionInParent().y.toInt() },
                ) {
                    state.result?.let { result ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(title = "Resultado")
                            ResultSection(result = result)
                        }
                    }
                }

                if (state.history.isNotEmpty()) {
                    HistorySection(
                        history = state.history,
                        onOpen = viewModel::openHistoryEntry,
                        isCompareMode = state.isCompareMode,
                        selection = state.compareSelection,
                        onToggleCompareMode = viewModel::toggleCompareMode,
                        onToggleSelection = viewModel::toggleCompareSelection,
                        onCompare = {
                            val (first, second) = state.compareSelection
                            viewModel.toggleCompareMode()
                            onCompare(state.goalId, first, second)
                        },
                    )
                }
            }
        }
    }
}

/** Quando um resultado novo aparece, rola a tela até ele (depois de medido). */
@Composable
internal fun ScrollToResult(resultId: Long?, scrollState: ScrollState, resultTop: () -> Int) {
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(resultId) {
        if (resultId == null) return@LaunchedEffect
        withFrameNanos { } // espera o resultado entrar na composição e ser posicionado
        scrollState.animateScrollTo(resultTop(), spec)
    }
}

// ============================================================================
// Formulário
// ============================================================================

@Composable
private fun ParameterForm(
    form: SimulationForm,
    isRunning: Boolean,
    onChange: ((SimulationForm) -> SimulationForm) -> Unit,
    totalAssets: java.math.BigDecimal? = null,
    onUseTotalAssets: () -> Unit = {},
) {
    val enabled = !isRunning
    var premisesOpen by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        FormSection(title = "Meta e prazo") {
            MoneyField(
                value = form.targetAmountInput,
                onValueChange = { v -> onChange { it.copy(targetAmountInput = v.asMonetaryInput()) } },
                label = "Valor da meta",
                enabled = enabled,
            )
            MonthsField(
                value = form.horizonMonthsInput,
                onValueChange = { v -> onChange { it.copy(horizonMonthsInput = v) } },
                label = "Horizonte",
                enabled = enabled,
            )
        }

        FormSection(title = "Seu dinheiro") {
            MoneyField(
                value = form.initialCapitalInput,
                onValueChange = { v -> onChange { it.copy(initialCapitalInput = v.asMonetaryInput()) } },
                label = "Capital inicial",
                enabled = enabled,
            )
            UseTotalAssetsChip(totalAssets = totalAssets, enabled = enabled, onClick = onUseTotalAssets)
            MoneyField(
                value = form.monthlyContributionInput,
                onValueChange = { v -> onChange { it.copy(monthlyContributionInput = v.asMonetaryInput()) } },
                label = "Aporte mensal",
                imeAction = ImeAction.Done,
                enabled = enabled,
            )
        }

        // Premissas de mercado e eventos adversos: pré-preenchidas com o cenário
        // mais seguro (100% do CDI e o risco de desemprego do vínculo), por isso
        // ficam recolhidas — o resumo mostra os valores em uso.
        ExpandableSection(
            title = "Premissas de mercado e riscos",
            summary = premisesSummary(form),
            expanded = premisesOpen,
            onExpandedChange = { premisesOpen = it },
        ) {
            Text(
                "Pré-preenchidas com o cenário mais seguro hoje: 100% do CDI (retorno e volatilidade " +
                    "da base de referência) e o risco de desemprego típico do seu vínculo de trabalho. " +
                    "Ajuste para simular outra carteira.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PercentField(
                value = form.expectedReturnInput,
                onValueChange = { v -> onChange { it.copy(expectedReturnInput = v.asMonetaryInput()) } },
                label = "Retorno esperado",
                suffix = "% a.a.",
                enabled = enabled,
            )
            PercentField(
                value = form.volatilityInput,
                onValueChange = { v -> onChange { it.copy(volatilityInput = v.asMonetaryInput()) } },
                label = "Volatilidade",
                suffix = "% a.a.",
                supportingText = "Quanto o retorno oscila de um ano para o outro.",
                enabled = enabled,
            )
            PercentField(
                value = form.inflationInput,
                onValueChange = { v -> onChange { it.copy(inflationInput = v.asMonetaryInput()) } },
                label = "Inflação",
                suffix = "% a.a.",
                enabled = enabled,
            )
            PercentField(
                value = form.unemploymentProbInput,
                onValueChange = { v -> onChange { it.copy(unemploymentProbInput = v.asMonetaryInput()) } },
                label = "Chance de desemprego",
                suffix = "% a.a.",
                supportingText = "Desempregado, o aporte para por ${form.unemploymentDurationMonths} meses.",
                imeAction = ImeAction.Done,
                enabled = enabled,
            )
        }

        FormSection(
            title = "Cenários simulados",
            supporting = "Mínimo de 10 mil. Mais cenários deixam o resultado mais estável e levam mais tempo.",
        ) {
            IterationsChoice(
                selected = form.numSimulations,
                onSelect = { n -> onChange { it.copy(numSimulations = n) } },
                enabled = enabled,
            )
        }
    }
}

/** "Retorno 10,65% · volatilidade 0,5% · inflação 4% · desemprego 3,1% ao ano". */
internal fun premisesSummary(form: SimulationForm): String =
    "Retorno ${form.expectedReturnInput.ifBlank { "—" }}% · volatilidade ${form.volatilityInput.ifBlank { "—" }}% · " +
        "inflação ${form.inflationInput.ifBlank { "—" }}% · desemprego ${form.unemploymentProbInput.ifBlank { "—" }}% ao ano"

/** Quantidades de cenários oferecidas (o TCC exige no mínimo 10 mil). */
internal val IterationOptions = listOf(10_000, 20_000, 30_000, 50_000)

/** Escolha da quantidade de cenários em botões conectados. */
@Composable
internal fun IterationsChoice(selected: Int, onSelect: (Int) -> Unit, enabled: Boolean) {
    ConnectedChoice(
        options = IterationOptions,
        selected = selected,
        onSelect = onSelect,
        label = { "${it / 1000} mil" },
        enabled = enabled,
    )
}

// ============================================================================
// Histórico
// ============================================================================

@Composable
private fun HistorySection(
    history: List<SimulationSummary>,
    onOpen: (Long) -> Unit,
    isCompareMode: Boolean,
    selection: List<Long>,
    onToggleCompareMode: () -> Unit,
    onToggleSelection: (Long) -> Unit,
    onCompare: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Simulações anteriores",
            supporting = if (isCompareMode) {
                "Escolha duas rodadas para comparar as estratégias lado a lado."
            } else {
                "Toque numa rodada para reabrir o resultado e os gráficos dela."
            },
            // Comparar estratégias lado a lado exige ao menos duas rodadas.
            action = if (history.size >= 2) {
                {
                    TextButton(onClick = onToggleCompareMode) {
                        Text(if (isCompareMode) "Cancelar" else "Comparar")
                    }
                }
            } else {
                null
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            history.forEachIndexed { index, sim ->
                HistoryItem(
                    sim = sim,
                    index = index,
                    count = history.size,
                    isCompareMode = isCompareMode,
                    selected = sim.id in selection,
                    onOpen = { onOpen(sim.id) },
                    onToggle = { onToggleSelection(sim.id) },
                )
            }
        }
        if (isCompareMode) {
            ActionButton(
                text = "Comparar selecionadas (${selection.size}/2)",
                icon = Icons.AutoMirrored.Rounded.CompareArrows,
                onClick = onCompare,
                enabled = selection.size == 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HistoryItem(
    sim: SimulationSummary,
    index: Int,
    count: Int,
    isCompareMode: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val colors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    )
    val headline: @Composable () -> Unit = {
        Text(formatDateTime(sim.createdAt) + if (sim.inputs?.calibrated == true) " · com IA" else "")
    }
    val supporting: @Composable () -> Unit = {
        Text(
            listOfNotNull(sim.inputs?.let(::strategyLine), "Mediana: ${formatBrl(sim.median)}").joinToString("\n"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val probability: @Composable () -> Unit = {
        Text(
            formatProbability(sim.successProbability),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (isCompareMode) {
        // A linha inteira é o alvo de toque; a caixa de seleção só sinaliza.
        SegmentedListItem(
            checked = selected,
            onCheckedChange = { onToggle() },
            shapes = shapes,
            colors = colors,
            leadingContent = { Checkbox(checked = selected, onCheckedChange = null) },
            supportingContent = supporting,
            trailingContent = probability,
            content = headline,
        )
    } else {
        SegmentedListItem(
            onClick = onOpen,
            shapes = shapes,
            colors = colors,
            supportingContent = supporting,
            trailingContent = probability,
            content = headline,
        )
    }
}

/** "Aporte R$ 2.000 · 8% a.a. · vol. 15% · 20 anos": a estratégia em uma linha. */
internal fun strategyLine(inputs: SimulationInputs): String =
    "Aporte ${formatBrl(inputs.monthlyContribution)} · ${formatAnnualRate(inputs.expectedReturnAnnual)} a.a. · " +
        "vol. ${formatAnnualRate(inputs.volatilityAnnual)} · ${formatHorizon(inputs.horizonMonths)}"
