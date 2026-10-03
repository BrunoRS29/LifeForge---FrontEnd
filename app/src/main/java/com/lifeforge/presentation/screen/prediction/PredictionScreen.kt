package com.lifeforge.presentation.screen.prediction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.LinearWavyProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.ExpenseCategoryPrediction
import com.lifeforge.domain.model.ExpensePrediction
import com.lifeforge.domain.model.IncomePrediction
import com.lifeforge.domain.model.PredictionMetrics
import com.lifeforge.domain.model.WealthPrediction
import com.lifeforge.presentation.common.ActionButton
import com.lifeforge.presentation.common.ActionEmphasis
import com.lifeforge.presentation.common.AutoSizeText
import com.lifeforge.presentation.common.BrlAxisFormatter
import com.lifeforge.presentation.common.ChartLegend
import com.lifeforge.presentation.common.ConnectedChoice
import com.lifeforge.presentation.common.DetailTopAppBar
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.ShapeIcon
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatHorizon
import com.lifeforge.presentation.common.formatPercent
import com.lifeforge.presentation.common.icon
import com.lifeforge.presentation.common.label
import com.lifeforge.presentation.common.monthAxisSpacing
import com.lifeforge.presentation.common.monthItemPlacer
import com.lifeforge.presentation.common.readableWidth
import com.lifeforge.presentation.common.rememberAreaLine
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
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Predições do microsserviço de IA, ajustadas ao histórico do próprio usuário:
 * renda (regressão linear), despesas por categoria (Random Forest) e patrimônio
 * (ARIMA, realizado × projetado). Cada bloco escolhe o horizonte, roda o modelo
 * e mostra o resultado com as métricas de erro (MAE, RMSE e R²).
 *
 * Não é aba da navegação principal: abre pelo painel e pela simulação com IA.
 */
@Composable
fun PredictionScreen(
    onNavigateBack: () -> Unit,
    viewModel: PredictionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopAppBar(
                title = "Predições",
                subtitle = "Modelos ajustados ao seu histórico",
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .readableWidth()
                .padding(horizontal = ScreenPadding)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.errorBanner != null) {
                ErrorBanner(message = state.errorBanner!!, onDismiss = viewModel::onErrorBannerDismiss)
            }
            IntroCard()
            PredictionBlock(
                title = "Renda",
                model = "Regressão linear",
                description = "Tendência dos seus recebimentos (mínimo de 6 meses de histórico).",
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                horizonOptions = listOf(6, 12, 24, 60),
                horizon = state.incomeHorizonMonths,
                onHorizonChange = viewModel::onIncomeHorizonChange,
                isLoading = state.isPredictingIncome,
                actionLabel = "Prever renda",
                onRun = viewModel::runPredictIncome,
            ) {
                state.incomePrediction?.let { IncomeResult(it) }
            }
            PredictionBlock(
                title = "Despesas",
                model = "Random Forest",
                description = "Gasto por categoria, com a sazonalidade do ano (mínimo de 12 meses de histórico).",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                horizonOptions = listOf(1, 3, 6, 12),
                horizon = state.expenseHorizonMonths,
                onHorizonChange = viewModel::onExpenseHorizonChange,
                isLoading = state.isPredictingExpenses,
                actionLabel = "Prever despesas",
                onRun = viewModel::runPredictExpenses,
            ) {
                state.expensePrediction?.let { ExpenseResult(it) }
            }
            PredictionBlock(
                title = "Patrimônio",
                model = "ARIMA",
                description = "Série do patrimônio reconstruída das suas receitas e despesas.",
                icon = Icons.AutoMirrored.Outlined.ShowChart,
                horizonOptions = listOf(6, 12, 24, 60),
                horizon = state.wealthHorizonMonths,
                onHorizonChange = viewModel::onWealthHorizonChange,
                isLoading = state.isPredictingWealth,
                actionLabel = "Prever patrimônio",
                onRun = viewModel::runPredictWealth,
            ) {
                state.wealthPrediction?.let { WealthResult(it) }
            }
        }
    }
}

@Composable
private fun IntroCard() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShapeIcon(
                icon = Icons.Outlined.AutoAwesome,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                shape = MaterialShapes.Sunny.toShape(),
                size = 48.dp,
            )
            Text(
                "Os modelos aprendem com o SEU histórico de receitas e despesas: quanto mais registros, " +
                    "mais precisa a predição.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Bloco de uma predição: cabeçalho com o modelo, escolha do horizonte em botões
 * conectados, ação de prever (com indicador ondulado durante o ajuste do
 * modelo) e o resultado.
 */
@Composable
private fun PredictionBlock(
    title: String,
    model: String,
    description: String,
    icon: ImageVector,
    horizonOptions: List<Int>,
    horizon: Int,
    onHorizonChange: (Int) -> Unit,
    isLoading: Boolean,
    actionLabel: String,
    onRun: () -> Unit,
    result: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ShapeIcon(icon = icon)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Text(model, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Horizonte", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ConnectedChoice(
                    options = horizonOptions,
                    selected = horizon,
                    onSelect = onHorizonChange,
                    label = ::formatHorizon,
                    enabled = !isLoading,
                )
            }
            ActionButton(
                text = if (isLoading) "Ajustando o modelo…" else actionLabel,
                icon = Icons.Outlined.AutoAwesome,
                onClick = onRun,
                enabled = !isLoading,
                emphasis = ActionEmphasis.Tonal,
                modifier = Modifier.fillMaxWidth(),
            )
            if (isLoading) LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            result()
        }
    }
}

// ============================================================================
// Resultados
// ============================================================================

@Composable
private fun IncomeResult(prediction: IncomePrediction) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeadlineValue(label = "Renda mensal média projetada", value = formatBrl(prediction.expectedMonthlyIncome))
        SummaryRow("Crescimento anual estimado", formatGrowthRate(prediction.annualGrowthRate))
        SummaryRow("Variação mensal típica (desvio dos resíduos)", formatBrl(prediction.residualVolatilityMonthly))
        IncomeChart(prediction)
        MetricsRow(prediction.metrics)
    }
}

@Composable
private fun IncomeChart(prediction: IncomePrediction) {
    val points = prediction.projection
    if (points.size < 2) return
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(prediction.predictionId) {
        modelProducer.runTransaction {
            lineSeries { series(x = points.map { it.monthIndex }, y = points.map { it.predictedAmount }) }
        }
    }
    val line = rememberAreaLine(MaterialTheme.colorScheme.primary)
    val itemPlacer = remember(points.size) { monthItemPlacer(monthAxisSpacing(points.size)) }
    val description = "Renda projetada mês a mês, de ${formatBrl(points.first().predictedAmount)} " +
        "a ${formatBrl(points.last().predictedAmount)} em ${points.last().monthIndex} meses"
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(lineProvider = LineCartesianLayer.LineProvider.series(line)),
            startAxis = VerticalAxis.rememberStart(valueFormatter = BrlAxisFormatter),
            bottomAxis = HorizontalAxis.rememberBottom(itemPlacer = itemPlacer, guideline = null),
        ),
        modelProducer = modelProducer,
        zoomState = rememberFitToWidthZoom(),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { contentDescription = description },
    )
}

@Composable
private fun ExpenseResult(prediction: ExpensePrediction) {
    // Só categorias com valor > 0: o modelo devolve 0 para categorias sem histórico.
    val categories = remember(prediction) {
        prediction.byCategory.filter { it.predictedAmount > 0.0 }.sortedByDescending { it.predictedAmount }
    }
    val max = categories.maxOfOrNull { it.predictedAmount } ?: 0.0
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeadlineValue(label = "Despesa mensal total prevista", value = formatBrl(prediction.expectedMonthlyExpense))
        categories.forEach { CategoryBar(it, max) }
        MetricsRow(prediction.metrics)
    }
}

/** Categoria com uma barra proporcional ao maior gasto previsto. */
@Composable
private fun CategoryBar(item: ExpenseCategoryPrediction, max: Double) {
    // Categorias desconhecidas (formato novo do backend) caem no nome cru.
    val label = item.category?.label() ?: item.rawCategory
    val fraction = if (max > 0) (item.predictedAmount / max).toFloat() else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapeIcon(
            icon = item.category?.icon() ?: Icons.Outlined.Category,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = CircleShape,
            size = 36.dp,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(formatBrl(item.predictedAmount), style = MaterialTheme.typography.titleSmall)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                )
            }
        }
    }
}

@Composable
private fun WealthResult(prediction: WealthPrediction) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeadlineValue(label = "Patrimônio projetado ao fim do horizonte", value = formatBrl(prediction.expectedFinalWealth))
        averageMonthlyIncrease(prediction)?.let { increase ->
            SummaryRow(
                "Aumento médio previsto",
                "${if (increase >= 0) "+" else "−"}${formatBrl(kotlin.math.abs(increase))}/mês",
            )
        }
        WealthChart(prediction)
        MetricsRow(prediction.metrics)
    }
}

/**
 * Gráfico realizado × projetado: a série histórica (real) e a projeção futura,
 * que começa no último ponto real (continuidade visual) via eixo x explícito.
 */
@Composable
private fun WealthChart(prediction: WealthPrediction) {
    val history = prediction.history
    val projection = prediction.projection
    if (history.size < 2) return

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(prediction.predictionId) {
        val lastX = history.last().monthIndex
        val lastY = history.last().amount
        modelProducer.runTransaction {
            lineSeries {
                series(x = history.map { it.monthIndex }, y = history.map { it.amount })
                series(
                    x = listOf(lastX) + projection.map { lastX + it.monthIndex },
                    y = listOf(lastY) + projection.map { it.predictedAmount },
                )
            }
        }
    }

    val realizedColor = MaterialTheme.colorScheme.primary
    val projectedColor = MaterialTheme.colorScheme.secondary
    val realized = rememberSolidLine(realizedColor)
    val projected = rememberAreaLine(projectedColor)
    val months = history.last().monthIndex - history.first().monthIndex + (projection.lastOrNull()?.monthIndex ?: 0)
    val itemPlacer = remember(months) { monthItemPlacer(monthAxisSpacing(months)) }
    val description = "Patrimônio realizado até ${formatBrl(history.last().amount)} e projetado até " +
        formatBrl(prediction.expectedFinalWealth)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(lineProvider = LineCartesianLayer.LineProvider.series(realized, projected)),
                startAxis = VerticalAxis.rememberStart(valueFormatter = BrlAxisFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(itemPlacer = itemPlacer, guideline = null),
            ),
            modelProducer = modelProducer,
            zoomState = rememberFitToWidthZoom(),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics { contentDescription = description },
        )
        ChartLegend(
            listOf(
                "Realizado (reconstruído do histórico)" to realizedColor,
                "Projetado pelo modelo" to projectedColor,
            ),
        )
    }
}

// ============================================================================
// Componentes
// ============================================================================

@Composable
private fun HeadlineValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AutoSizeText(
            text = value,
            style = MaterialTheme.typography.headlineMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // O rótulo quebra linha se precisar; o valor fica inteiro numa linha.
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.titleSmall, maxLines = 1, softWrap = false)
    }
}

/** Métricas de erro do modelo (MAE, RMSE e R²) — informação, não botão. */
@Composable
private fun MetricsRow(metrics: PredictionMetrics) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricPill("MAE", formatMetric(metrics.mae))
        MetricPill("RMSE", formatMetric(metrics.rmse))
        MetricPill("R²", formatR2(metrics.r2))
    }
}

@Composable
private fun MetricPill(name: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
        modifier = Modifier.semantics { contentDescription = "$name $value" },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(name, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ============================================================================
// Formatadores locais (específicos desta tela)
// ============================================================================

/** Crescimento anual com sinal: 0.10 → "+10,0%". */
private fun formatGrowthRate(rate: Double): String {
    val asPercent = BigDecimal(rate * 100.0).setScale(1, RoundingMode.HALF_UP)
    val sign = if (rate >= 0) "+" else ""
    return sign + formatPercent(asPercent)
}

/** Numérico curto, 1 decimal. Ex.: "1234,5". */
private fun formatMetric(value: Double): String =
    String.format(java.util.Locale("pt", "BR"), "%.1f", value)

/** R² com 2 decimais, pode ser negativo. */
private fun formatR2(value: Double): String =
    String.format(java.util.Locale("pt", "BR"), "%.2f", value)

/**
 * Quanto o patrimônio sobe por mês, em média, do último mês realizado ao fim
 * do horizonte — mais legível que a taxa relativa do modelo (inclinação ÷ nível
 * médio), que a partir de um patrimônio pequeno parece crescimento composto.
 */
internal fun averageMonthlyIncrease(prediction: WealthPrediction): Double? {
    val last = prediction.history.lastOrNull()?.amount ?: return null
    if (prediction.horizonMonths <= 0) return null
    return (prediction.expectedFinalWealth - last) / prediction.horizonMonths
}
