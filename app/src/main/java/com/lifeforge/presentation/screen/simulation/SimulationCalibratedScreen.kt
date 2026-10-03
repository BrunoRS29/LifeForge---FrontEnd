package com.lifeforge.presentation.screen.simulation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.CalibrationSource
import com.lifeforge.domain.model.CalibrationSummary
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.presentation.common.ContentCard
import com.lifeforge.presentation.common.DetailTopAppBar
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.FormSection
import com.lifeforge.presentation.common.MoneyField
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.SectionHeader
import com.lifeforge.presentation.common.ShapeIcon
import com.lifeforge.presentation.common.formatAnnualRate
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatProbability
import com.lifeforge.presentation.common.readableWidth
import com.lifeforge.presentation.common.sanitizeCurrencyInput

/**
 * Simulação calibrada pela IA: um toque.
 *
 * Diferenças para a [SimulationScreen]:
 *  - o formulário não pede o aporte mensal (derivado da renda e da despesa
 *    previstas) nem as premissas de mercado (vêm da base de referência,
 *    calibradas pelo perfil de risco e pelo vínculo);
 *  - depois do resultado, primeiro o [CalibrationSummaryCard] explica de onde
 *    veio cada premissa (modelo, perfil ou base de referência), depois o mesmo
 *    [ResultSection] da simulação manual (medidor, cenários e gráficos).
 */
@Composable
fun SimulationCalibratedScreen(
    onNavigateBack: () -> Unit,
    viewModel: SimulationCalibratedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()
    var resultTop by remember { mutableIntStateOf(0) }

    ScrollToResult(resultId = state.result?.simulation?.id, scrollState = scrollState, resultTop = { resultTop })

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopAppBar(
                title = "Simular com IA",
                subtitle = state.goalName,
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
                navigationEnabled = !state.isRunning,
            )
        },
        bottomBar = {
            RunBar(
                isRunning = state.isRunning,
                runningText = state.progressMessage ?: "Calibrando…",
                idleText = "Simular com IA",
                enabled = state.form.canRun,
                onRun = viewModel::runCalibrated,
                icon = Icons.Outlined.AutoAwesome,
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

                AiCalloutCard(premises = state.premises)

                CalibratedParameterForm(
                    form = state.form,
                    isRunning = state.isRunning,
                    onChange = viewModel::onFormChange,
                    totalAssets = state.totalAssets,
                    onUseTotalAssets = viewModel::useTotalAssetsAsInitialCapital,
                )

                AnimatedVisibility(
                    visible = state.result != null,
                    enter = fadeIn() + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    modifier = Modifier.onPlaced { resultTop = it.positionInParent().y.toInt() },
                ) {
                    state.result?.let { result ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SectionHeader(title = "Resultado")
                            CalibrationSummaryCard(summary = result.calibration)
                            ResultSection(result = result.simulation)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Explicação da IA
// ============================================================================

@Composable
private fun AiCalloutCard(premises: SimulationPremises?) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShapeIcon(
                icon = Icons.Outlined.AutoAwesome,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                shape = MaterialShapes.Sunny.toShape(),
                size = 48.dp,
            )
            Text("Tudo calibrado pelo seu perfil", style = MaterialTheme.typography.titleLarge)
            Text(
                "Você não estima nada de mercado: a IA prevê sua renda e despesas pelo seu histórico " +
                    "(e calcula o aporte mensal), e o retorno, a volatilidade, a inflação e o risco de " +
                    "desemprego vêm da base de referência calibrada ao seu perfil de risco e vínculo.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (premises != null) {
                Text(premisesText(premises), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** "Seu perfil (moderado): retorno ~10,6% a.a. e inflação ~4,2% a.a." */
private fun premisesText(premises: SimulationPremises): String {
    val profile = when (premises.riskProfile) {
        RiskProfile.CONSERVATIVE -> "conservador"
        RiskProfile.AGGRESSIVE -> "arrojado"
        else -> "moderado"
    }
    return "Seu perfil ($profile): retorno ~${formatProbability(premises.expectedReturnAnnual)} a.a. e " +
        "inflação ~${formatProbability(premises.inflationAnnual)} a.a."
}

// ============================================================================
// Formulário
// ============================================================================

@Composable
private fun CalibratedParameterForm(
    form: CalibratedSimulationForm,
    isRunning: Boolean,
    onChange: ((CalibratedSimulationForm) -> CalibratedSimulationForm) -> Unit,
    totalAssets: java.math.BigDecimal? = null,
    onUseTotalAssets: () -> Unit = {},
) {
    val enabled = !isRunning
    // Sem aporte mensal (derivado pela IA) e sem premissas de mercado (o backend
    // as calibra pelo perfil): só o que é do usuário.
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        FormSection(title = "Meta e prazo") {
            MoneyField(
                value = form.targetAmountInput,
                onValueChange = { v -> onChange { it.copy(targetAmountInput = sanitizeCurrencyInput(v)) } },
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
        FormSection(title = "Seu dinheiro hoje") {
            MoneyField(
                value = form.initialCapitalInput,
                onValueChange = { v -> onChange { it.copy(initialCapitalInput = sanitizeCurrencyInput(v)) } },
                label = "Capital inicial",
                imeAction = ImeAction.Done,
                enabled = enabled,
            )
            UseTotalAssetsChip(totalAssets = totalAssets, enabled = enabled, onClick = onUseTotalAssets)
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

// ============================================================================
// CalibrationSummaryCard
// ============================================================================

/**
 * Cartão que explica COMO os parâmetros foram calibrados — transparência em
 * decisões assistidas por modelos (requisito do TCC): renda projetada, menos a
 * despesa projetada, igual ao aporte aplicado, com a origem de cada número e o
 * aviso quando o aporte foi zerado.
 */
@Composable
fun CalibrationSummaryCard(summary: CalibrationSummary) {
    ContentCard(
        title = if (summary.usedFallback) "Como os parâmetros foram calibrados" else "Como a IA calibrou os parâmetros",
        action = {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        },
    ) {
        // Partida a frio: sem histórico para os modelos, o servidor recua para o
        // perfil, médias simples e a base de referência — e explica o porquê.
        if (summary.usedFallback) {
            FallbackNotice(summary)
        }

        if (summary.contributionFromProfile) {
            CalibrationLine(
                label = "Aporte mensal (do seu perfil)",
                value = formatBrl(summary.appliedMonthlyContribution),
                highlight = true,
            )
        } else {
            CalibrationLine(
                label = "Renda mensal" + summary.incomeSource.sourceSuffix(),
                value = formatBrl(summary.predictedMonthlyIncome),
            )
            CalibrationLine(
                label = "(−) Despesa mensal" + summary.expenseSource.sourceSuffix(),
                value = formatBrl(summary.predictedMonthlyExpense),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            CalibrationLine(
                label = "(=) Aporte derivado",
                value = formatBrl(summary.appliedMonthlyContribution),
                highlight = true,
            )
        }

        if (summary.cappedToZero) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = "Despesa maior que renda: aporte zerado " +
                        "(${formatBrl(summary.rawMonthlyContribution)} antes do ajuste).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Text(
            text = calibrationRiskText(summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val predictionIds = listOfNotNull(
            summary.incomePredictionId?.let { "Predição de renda #$it" },
            summary.expensePredictionId?.let { "Predição de despesa #$it" },
        )
        if (predictionIds.isNotEmpty()) {
            Text(
                text = predictionIds.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Aviso de recuo: o que não veio dos modelos de IA e por quê. */
@Composable
private fun FallbackNotice(summary: CalibrationSummary) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Nem tudo veio dos modelos de IA: sem histórico suficiente (6 receitas e 12 " +
                        "despesas) ou com o serviço indisponível, usamos os dados do seu perfil e as " +
                        "médias dos seus lançamentos; a variação de renda vem da base de referência " +
                        "para o seu vínculo de trabalho.",
                    style = MaterialTheme.typography.bodySmall,
                )
                summary.fallbackNotes.forEach { note ->
                    Text("• $note", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun CalibrationSource?.sourceSuffix(): String = when (this) {
    null -> ""
    CalibrationSource.ML_MODEL -> " projetada (IA)"
    else -> " (${label})"
}

@Composable
private fun CalibrationLine(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlight) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (highlight) {
                MaterialTheme.typography.titleLargeEmphasized
            } else {
                MaterialTheme.typography.titleSmall
            },
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Como a incerteza entra na simulação calibrada: a carteira oscila com a
 * volatilidade de mercado do perfil; a incerteza da renda prevista faz o
 * aporte variar mês a mês (não o retorno de todo o patrimônio).
 */
internal fun calibrationRiskText(summary: CalibrationSummary): String = buildString {
    append("Volatilidade da carteira: ${formatAnnualRate(summary.appliedVolatilityAnnual)} a.a. (mercado).")
    if (summary.contributionVariationMonthly > 0.0) {
        append(" Incerteza da renda: o aporte varia ±")
        append(formatAnnualRate(summary.contributionVariationMonthly))
        append(" ao mês.")
    }
}
