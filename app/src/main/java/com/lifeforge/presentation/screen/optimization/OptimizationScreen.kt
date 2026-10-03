package com.lifeforge.presentation.screen.optimization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.IterationStep
import com.lifeforge.domain.model.OptimizationResult
import com.lifeforge.domain.model.OptimizationType
import com.lifeforge.domain.model.RebalanceResult
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.model.TerminationReason
import com.lifeforge.presentation.common.AllocationBar
import com.lifeforge.presentation.common.AllocationShare
import com.lifeforge.presentation.common.AutoSizeText
import com.lifeforge.presentation.common.BrlAxisFormatter
import com.lifeforge.presentation.common.ChartLegend
import com.lifeforge.presentation.common.ConnectedChoice
import com.lifeforge.presentation.common.ContentCard
import com.lifeforge.presentation.common.EnumDropdown
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.ExpandableSection
import com.lifeforge.presentation.common.FormSection
import com.lifeforge.presentation.common.MoneyField
import com.lifeforge.presentation.common.MonthsField
import com.lifeforge.presentation.common.PercentField
import com.lifeforge.presentation.common.ProgressActionBar
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.SectionHeader
import com.lifeforge.presentation.common.ShapeIcon
import com.lifeforge.presentation.common.TabTopAppBar
import com.lifeforge.presentation.common.UseTotalAssetsChip
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatCount
import com.lifeforge.presentation.common.formatHorizon
import com.lifeforge.presentation.common.formatProbability
import com.lifeforge.presentation.common.icon
import com.lifeforge.presentation.common.label
import com.lifeforge.presentation.common.readableWidth
import com.lifeforge.presentation.common.rememberDashedLine
import com.lifeforge.presentation.common.rememberFitToWidthZoom
import com.lifeforge.presentation.common.rememberSolidLine
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import java.math.BigDecimal

/**
 * Tela de Otimização — três modos, escolhidos em botões conectados:
 *
 * - **Aporte**: dado o prazo e a meta, qual o aporte mensal mínimo?
 * - **Prazo**: dado o aporte e a meta, em quantos meses chega?
 * - **Carteira**: dado o perfil de risco, qual mix de ativos sugerido?
 *
 * Os formulários vêm pré-preenchidos (premissas do CDI da base de referência e
 * probabilidade-alvo de 80%), então dá para calcular de imediato. O resultado
 * mostra o valor encontrado, a verificação por Monte Carlo e o passo a passo da
 * busca binária. Resultados ficam no ViewModel: trocar de modo não os apaga.
 */
@Composable
fun OptimizationScreen(viewModel: OptimizationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()
    var resultTop by remember { mutableIntStateOf(0) }
    val mode = state.selectedMode

    // Resultado novo (em qualquer modo) → rola até ele; trocar de modo não rola.
    val scrollSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(state.contributionResult, state.horizonResult, state.rebalanceResult) {
        val hasResult = when (mode) {
            OptimizationMode.CONTRIBUTION -> state.contributionResult != null
            OptimizationMode.HORIZON -> state.horizonResult != null
            OptimizationMode.REBALANCE -> state.rebalanceResult != null
        }
        if (!hasResult) return@LaunchedEffect
        withFrameNanos { }
        scrollState.animateScrollTo(resultTop, scrollSpec)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { TabTopAppBar(title = "Otimizar", subtitle = mode.subtitle, scrollBehavior = scrollBehavior) },
        bottomBar = {
            ProgressActionBar(
                isRunning = state.isRunning,
                runningText = "Calculando…",
                idleText = mode.action,
                enabled = when (mode) {
                    OptimizationMode.CONTRIBUTION -> state.contributionForm.canRun
                    OptimizationMode.HORIZON -> state.horizonForm.canRun
                    OptimizationMode.REBALANCE -> state.rebalanceForm.canRun
                },
                onClick = when (mode) {
                    OptimizationMode.CONTRIBUTION -> viewModel::runContribution
                    OptimizationMode.HORIZON -> viewModel::runHorizon
                    OptimizationMode.REBALANCE -> viewModel::runRebalance
                },
                icon = mode.icon,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .readableWidth()
                .padding(horizontal = ScreenPadding)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ConnectedChoice(
                options = OptimizationMode.entries,
                selected = mode,
                onSelect = viewModel::selectMode,
                label = { it.label },
                icon = { it.icon },
                enabled = !state.isRunning,
            )

            if (state.errorBanner != null) {
                ErrorBanner(message = state.errorBanner!!, onDismiss = viewModel::onErrorBannerDismiss)
            }

            val enabled = !state.isRunning
            when (mode) {
                OptimizationMode.CONTRIBUTION -> ContributionForm(
                    form = state.contributionForm,
                    enabled = enabled,
                    onChange = viewModel::onContributionForm,
                    goals = state.goals,
                    totalAssets = state.totalAssets,
                    onApplyGoal = { viewModel.applyGoal(OptimizationMode.CONTRIBUTION, it) },
                    onUseTotalAssets = { viewModel.useTotalAssets(OptimizationMode.CONTRIBUTION) },
                )
                OptimizationMode.HORIZON -> HorizonForm(
                    form = state.horizonForm,
                    enabled = enabled,
                    onChange = viewModel::onHorizonForm,
                    goals = state.goals,
                    totalAssets = state.totalAssets,
                    onApplyGoal = { viewModel.applyGoal(OptimizationMode.HORIZON, it) },
                    onUseTotalAssets = { viewModel.useTotalAssets(OptimizationMode.HORIZON) },
                )
                OptimizationMode.REBALANCE -> RebalanceForm(
                    form = state.rebalanceForm,
                    enabled = enabled,
                    onChange = viewModel::onRebalanceForm,
                    goals = state.goals,
                    totalAssets = state.totalAssets,
                    onApplyGoal = { viewModel.applyGoal(OptimizationMode.REBALANCE, it) },
                    onUseTotalAssets = { viewModel.useTotalAssets(OptimizationMode.REBALANCE) },
                )
            }

            Column(
                modifier = Modifier.onPlaced { resultTop = it.positionInParent().y.toInt() },
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (mode) {
                    OptimizationMode.CONTRIBUTION -> state.contributionResult?.let { result ->
                        SectionHeader(title = "Resultado")
                        OptimizationResultSection(result, label = "Aporte mensal necessário", value = formatBrl(result.optimalValue))
                    }
                    OptimizationMode.HORIZON -> state.horizonResult?.let { result ->
                        SectionHeader(title = "Resultado")
                        OptimizationResultSection(result, label = "Tempo necessário", value = formatPeriod(result.optimalValue))
                    }
                    OptimizationMode.REBALANCE -> state.rebalanceResult?.let { result ->
                        SectionHeader(title = "Resultado")
                        RebalanceResultCard(result)
                    }
                }
            }
        }
    }
}

private val OptimizationMode.label: String
    get() = when (this) {
        OptimizationMode.CONTRIBUTION -> "Aporte"
        OptimizationMode.HORIZON -> "Prazo"
        OptimizationMode.REBALANCE -> "Carteira"
    }

private val OptimizationMode.icon: ImageVector
    get() = when (this) {
        OptimizationMode.CONTRIBUTION -> Icons.Outlined.Savings
        OptimizationMode.HORIZON -> Icons.Outlined.HourglassBottom
        OptimizationMode.REBALANCE -> Icons.Outlined.PieChart
    }

private val OptimizationMode.subtitle: String
    get() = when (this) {
        OptimizationMode.CONTRIBUTION -> "Quanto aportar por mês para chegar lá"
        OptimizationMode.HORIZON -> "Em quanto tempo a meta é atingida"
        OptimizationMode.REBALANCE -> "Mix de ativos para o seu perfil"
    }

private val OptimizationMode.action: String
    get() = when (this) {
        OptimizationMode.CONTRIBUTION -> "Calcular aporte mínimo"
        OptimizationMode.HORIZON -> "Calcular prazo"
        OptimizationMode.REBALANCE -> "Sugerir carteira"
    }

/** Meses → "2 anos e 6 meses" (ou "menos de 1 mês"). */
internal fun formatPeriod(months: Double): String =
    if (months < 1.0) "Menos de 1 mês" else formatHorizon(months.toInt())

// ============================================================================
// Formulários
// ============================================================================

@Composable
private fun ContributionForm(
    form: ContributionForm,
    enabled: Boolean,
    onChange: ((ContributionForm) -> ContributionForm) -> Unit,
    goals: List<Goal>,
    totalAssets: BigDecimal?,
    onApplyGoal: (Goal) -> Unit,
    onUseTotalAssets: () -> Unit,
) {
    FormSection(title = "Meta") {
        GoalImportPicker(goals, form.selectedGoalName, enabled, onApplyGoal)
        MoneyField(
            value = form.targetAmount,
            onValueChange = { v -> onChange { it.copy(targetAmount = v.asMonetaryInput()) } },
            label = "Valor da meta",
            enabled = enabled,
        )
        MonthsField(
            value = form.horizonMonths,
            onValueChange = { v -> onChange { it.copy(horizonMonths = v) } },
            label = "Prazo",
            enabled = enabled,
        )
    }
    FormSection(title = "Seu dinheiro hoje") {
        MoneyField(
            value = form.initialCapital,
            onValueChange = { v -> onChange { it.copy(initialCapital = v.asMonetaryInput()) } },
            label = "Capital inicial",
            enabled = enabled,
        )
        UseTotalAssetsChip(totalAssets, enabled, onUseTotalAssets)
    }
    PremisesSection(
        expectedReturn = form.expectedReturnAnnual,
        volatility = form.volatilityAnnual,
        targetProbability = form.targetSuccessProbability,
        probabilityHelp = "O menor aporte que alcança essa chance de sucesso.",
        enabled = enabled,
        onExpectedReturn = { v -> onChange { it.copy(expectedReturnAnnual = v.asMonetaryInput()) } },
        onVolatility = { v -> onChange { it.copy(volatilityAnnual = v.asMonetaryInput()) } },
        onTargetProbability = { v -> onChange { it.copy(targetSuccessProbability = v.asMonetaryInput()) } },
    )
}

@Composable
private fun HorizonForm(
    form: HorizonForm,
    enabled: Boolean,
    onChange: ((HorizonForm) -> HorizonForm) -> Unit,
    goals: List<Goal>,
    totalAssets: BigDecimal?,
    onApplyGoal: (Goal) -> Unit,
    onUseTotalAssets: () -> Unit,
) {
    FormSection(title = "Meta") {
        GoalImportPicker(goals, form.selectedGoalName, enabled, onApplyGoal)
        MoneyField(
            value = form.targetAmount,
            onValueChange = { v -> onChange { it.copy(targetAmount = v.asMonetaryInput()) } },
            label = "Valor da meta",
            enabled = enabled,
        )
    }
    FormSection(title = "Seu dinheiro") {
        MoneyField(
            value = form.initialCapital,
            onValueChange = { v -> onChange { it.copy(initialCapital = v.asMonetaryInput()) } },
            label = "Capital inicial",
            enabled = enabled,
        )
        UseTotalAssetsChip(totalAssets, enabled, onUseTotalAssets)
        MoneyField(
            value = form.monthlyContribution,
            onValueChange = { v -> onChange { it.copy(monthlyContribution = v.asMonetaryInput()) } },
            label = "Aporte mensal",
            enabled = enabled,
        )
    }
    PremisesSection(
        expectedReturn = form.expectedReturnAnnual,
        volatility = form.volatilityAnnual,
        targetProbability = form.targetSuccessProbability,
        probabilityHelp = "O menor prazo que alcança essa chance de sucesso.",
        enabled = enabled,
        onExpectedReturn = { v -> onChange { it.copy(expectedReturnAnnual = v.asMonetaryInput()) } },
        onVolatility = { v -> onChange { it.copy(volatilityAnnual = v.asMonetaryInput()) } },
        onTargetProbability = { v -> onChange { it.copy(targetSuccessProbability = v.asMonetaryInput()) } },
    )
}

@Composable
private fun RebalanceForm(
    form: RebalanceForm,
    enabled: Boolean,
    onChange: ((RebalanceForm) -> RebalanceForm) -> Unit,
    goals: List<Goal>,
    totalAssets: BigDecimal?,
    onApplyGoal: (Goal) -> Unit,
    onUseTotalAssets: () -> Unit,
) {
    FormSection(title = "Perfil de risco") {
        ConnectedChoice(
            options = RiskProfile.entries,
            selected = form.riskProfile,
            onSelect = { p -> onChange { it.copy(riskProfile = p) } },
            label = RiskProfile::label,
            enabled = enabled,
        )
    }
    FormSection(title = "Meta", supporting = "A carteira fica mais conservadora conforme a meta se aproxima.") {
        GoalImportPicker(goals, form.selectedGoalName, enabled, onApplyGoal)
        MoneyField(
            value = form.targetAmount,
            onValueChange = { v -> onChange { it.copy(targetAmount = v.asMonetaryInput()) } },
            label = "Valor da meta",
            enabled = enabled,
        )
        MonthsField(
            value = form.monthsToGoal,
            onValueChange = { v -> onChange { it.copy(monthsToGoal = v) } },
            label = "Prazo",
            imeAction = ImeAction.Done,
            enabled = enabled,
        )
    }
    FormSection(title = "Seu dinheiro") {
        MoneyField(
            value = form.currentCapital,
            onValueChange = { v -> onChange { it.copy(currentCapital = v.asMonetaryInput()) } },
            label = "Capital disponível",
            enabled = enabled,
        )
        UseTotalAssetsChip(totalAssets, enabled, onUseTotalAssets)
    }
}

/** Premissas da busca, recolhidas: já vêm com o CDI da base e a chance-alvo de 80%. */
@Composable
private fun PremisesSection(
    expectedReturn: String,
    volatility: String,
    targetProbability: String,
    probabilityHelp: String,
    enabled: Boolean,
    onExpectedReturn: (String) -> Unit,
    onVolatility: (String) -> Unit,
    onTargetProbability: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    ExpandableSection(
        title = "Premissas",
        summary = "Retorno ${expectedReturn.ifBlank { "—" }}% · volatilidade ${volatility.ifBlank { "—" }}% ao ano · " +
            "chance-alvo ${targetProbability.ifBlank { "—" }}%",
        expanded = open,
        onExpandedChange = { open = it },
    ) {
        PercentField(
            value = expectedReturn,
            onValueChange = onExpectedReturn,
            label = "Retorno esperado",
            suffix = "% a.a.",
            enabled = enabled,
        )
        PercentField(
            value = volatility,
            onValueChange = onVolatility,
            label = "Volatilidade",
            suffix = "% a.a.",
            supportingText = "Quanto o retorno oscila de um ano para o outro.",
            enabled = enabled,
        )
        PercentField(
            value = targetProbability,
            onValueChange = onTargetProbability,
            label = "Chance de sucesso desejada",
            supportingText = probabilityHelp,
            imeAction = ImeAction.Done,
            enabled = enabled,
        )
    }
}

/**
 * Importa uma meta para o formulário: valor (e prazo, quando o modo usa o
 * prazo como entrada). Não aparece quando o usuário não tem metas.
 */
@Composable
private fun GoalImportPicker(
    goals: List<Goal>,
    selectedGoalName: String?,
    enabled: Boolean,
    onApply: (Goal) -> Unit,
) {
    if (goals.isEmpty()) return
    EnumDropdown<Goal?>(
        label = "Importar de uma meta",
        options = goals,
        selected = goals.firstOrNull { it.name == selectedGoalName },
        onSelect = { goal -> goal?.let(onApply) },
        labelOf = { goal -> goal?.name ?: "Escolher meta…" },
        iconOf = { goal -> goal?.category?.icon() ?: Icons.Outlined.Savings },
        enabled = enabled,
    )
}

// ============================================================================
// Resultados
// ============================================================================

@Composable
private fun OptimizationResultSection(result: OptimizationResult, label: String, value: String) {
    OptimizationHero(result = result, label = label, value = value)
    if (result.iterations.isNotEmpty()) {
        BinarySearchSteps(result)
    }
}

/**
 * Resposta em destaque: o valor encontrado, a chance verificada por Monte Carlo
 * (10.000 cenários) contra a desejada e, se for o caso, por que a meta é inviável.
 */
@Composable
private fun OptimizationHero(result: OptimizationResult, label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    val container = if (result.feasible) colors.primaryContainer else colors.errorContainer
    val content = if (result.feasible) colors.onPrimaryContainer else colors.onErrorContainer
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShapeIcon(
                    icon = if (result.feasible) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                    containerColor = if (result.feasible) colors.primary else colors.error,
                    contentColor = if (result.feasible) colors.onPrimary else colors.onError,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                )
                Text(
                    if (result.feasible) "Encontrado" else "Inviável com esses parâmetros",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(label, style = MaterialTheme.typography.labelLarge)
            AutoSizeText(
                text = value,
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = content,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Chance verificada: ${formatProbability(result.achievedProbability)} " +
                    "(desejada: ${formatProbability(result.targetProbability)})",
                style = MaterialTheme.typography.bodyMedium,
            )
            samplingNote(result.achievedProbability, result.targetProbability)?.let { note ->
                Text(note, style = MaterialTheme.typography.bodySmall)
            }
            terminationText(result.terminationReason)?.let { text ->
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                listOfNotNull(
                    result.verification?.let { "Verificado com ${formatCount(it.numSimulations)} cenários" },
                    "${result.iterations.size} passos",
                    "${formatCount(result.executionTimeMs.toInt())} ms",
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** Explicação do fim da busca quando ele não é a convergência normal. */
internal fun terminationText(reason: TerminationReason): String? = when (reason) {
    TerminationReason.CONVERGED -> null
    TerminationReason.MAX_ITERATIONS -> "A busca parou no limite de passos, antes de convergir."
    TerminationReason.INFEASIBLE_UPPER_BOUND ->
        "Nem o teto da busca alcança a chance desejada: aumente o prazo ou o aporte, reduza a meta ou revise as premissas."
    TerminationReason.LOWER_BOUND_SUFFICIENT -> "O limite inferior da busca já alcança a chance desejada."
}

/**
 * Passo a passo da busca binária (TCC, Seção 4.6): em cada passo, o candidato
 * testado (o meio do intervalo), a chance medida por Monte Carlo e o intervalo,
 * que se estreita até convergir. O gráfico mostra o candidato e os limites.
 */
@Composable
private fun BinarySearchSteps(result: OptimizationResult) {
    var open by rememberSaveable { mutableStateOf(false) }
    val isMoney = result.type == OptimizationType.OPTIMAL_CONTRIBUTION
    val format: (Double) -> String = if (isMoney) ::formatBrl else { months -> formatPeriod(months) }
    ExpandableSection(
        title = "Passo a passo da busca binária",
        summary = "${result.iterations.size} passos: o intervalo se estreita até o menor valor que atinge a chance desejada.",
        expanded = open,
        onExpandedChange = { open = it },
    ) {
        BinarySearchChart(steps = result.iterations, isMoney = isMoney)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            result.iterations.forEach { step ->
                StepRow(step = step, format = format, targetProbability = result.targetProbability)
            }
        }
    }
}

@Composable
private fun StepRow(step: IterationStep, format: (Double) -> String, targetProbability: Double) {
    val reached = step.measuredProbability >= targetProbability
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Largura fixa: os números de 1 e de 2 dígitos ficam alinhados.
        Text(
            "${step.index + 1}",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(format(step.candidate), style = MaterialTheme.typography.titleSmall)
            Text(
                "Intervalo ${format(step.lowerBound)} – ${format(step.upperBound)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Text(
            formatProbability(step.measuredProbability),
            style = MaterialTheme.typography.titleSmall,
            color = if (reached) colors.primary else colors.error,
        )
    }
}

@Composable
private fun BinarySearchChart(steps: List<IterationStep>, isMoney: Boolean) {
    if (steps.size < 2) return
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(steps) {
        modelProducer.runTransaction {
            lineSeries {
                // Passos numerados a partir de 1 (o motor conta a partir de 0).
                val x = steps.map { it.index + 1 }
                series(x = x, y = steps.map { it.upperBound })
                series(x = x, y = steps.map { it.lowerBound })
                series(x = x, y = steps.map { it.candidate })
            }
        }
    }
    val boundColor = MaterialTheme.colorScheme.outline
    val candidateColor = MaterialTheme.colorScheme.primary
    val upper = rememberDashedLine(boundColor)
    val lower = rememberDashedLine(boundColor)
    val candidate = rememberSolidLine(candidateColor)
    val stepFormatter = remember { CartesianValueFormatter { _, value, _ -> value.toInt().toString() } }
    val description = "Convergência da busca binária em ${steps.size} passos, do intervalo inicial ao valor final"
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(lineProvider = LineCartesianLayer.LineProvider.series(upper, lower, candidate)),
            startAxis = if (isMoney) {
                VerticalAxis.rememberStart(valueFormatter = BrlAxisFormatter)
            } else {
                VerticalAxis.rememberStart()
            },
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = stepFormatter, guideline = null),
        ),
        modelProducer = modelProducer,
        zoomState = rememberFitToWidthZoom(),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { contentDescription = description },
    )
    ChartLegend(
        entries = listOf(
            "Candidato testado" to candidateColor,
            "Limites do intervalo (tracejados)" to boundColor,
        ),
    )
}

@Composable
private fun RebalanceResultCard(result: RebalanceResult) {
    val shares = remember(result) {
        result.weights.toList()
            .sortedByDescending { it.second }
            .map { (type, weight) -> AllocationShare(type.label(), weight) }
    }
    ContentCard(title = "Carteira sugerida", supporting = "Distribuição do capital por tipo de ativo.") {
        AllocationBar(shares = shares)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Retorno esperado", "${formatProbability(result.expectedReturnAnnual)} a.a.", Modifier.weight(1f))
            StatTile("Volatilidade", "${formatProbability(result.volatilityAnnual)} a.a.", Modifier.weight(1f))
        }
        if (result.rationale.isNotBlank()) {
            Text(result.rationale, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMediumEmphasized)
        }
    }
}

/**
 * A busca binária mede cada candidato com 2.000 cenários (mesma semente) e a
 * verificação final usa 10.000: a probabilidade verificada pode ficar um pouco
 * abaixo do alvo só por ruído amostral. Quando a diferença cabe em dois
 * erros-padrão da verificação, a tela explica isso em vez de parecer falha.
 */
internal fun samplingNote(achieved: Double, target: Double, verificationRuns: Int = 10_000): String? {
    if (achieved >= target || verificationRuns <= 0) return null
    val margin = 2 * kotlin.math.sqrt(target * (1 - target) / verificationRuns)
    val gap = target - achieved
    if (gap > margin) return null
    return "Diferença de ${formatPp(gap)} em relação ao alvo: dentro do erro amostral do Monte Carlo " +
        "(±${formatPp(margin)} com ${formatCount(verificationRuns)} cenários)."
}

private fun formatPp(fraction: Double): String =
    String.format(java.util.Locale("pt", "BR"), "%.1f p.p.", fraction * 100)
