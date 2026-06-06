package com.lifeforge.presentation.screen.prediction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.ExpenseCategoryPrediction
import com.lifeforge.domain.model.ExpensePrediction
import com.lifeforge.domain.model.IncomePrediction
import com.lifeforge.domain.model.IncomePredictionPoint
import com.lifeforge.domain.model.PredictionMetrics
import com.lifeforge.domain.model.WealthPrediction
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatBrlCompact
import com.lifeforge.presentation.common.formatPercent
import com.lifeforge.presentation.common.label
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Tela standalone de Predicoes (Sprint 5).
 *
 * Layout: dois cards principais empilhados verticalmente:
 *  1. Card "Predicao de Renda" - input do horizonte + botao + resultados
 *  2. Card "Predicao de Despesas" - input do horizonte + botao + resultados
 *
 * Decisao de design: NAO eh aba do bottom bar. Acessivel via "Profile"
 * ou via deep link "Simular com IA" da tela de Simulation. Mantemos o
 * bottom bar com 5 abas (limite ergonomico de Material Design).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionScreen(
    onNavigateBack: () -> Unit,
    viewModel: PredictionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Predicoes (IA)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        PredictionContent(
            state = state,
            padding = padding,
            onIncomeHorizonChange = viewModel::onIncomeHorizonChange,
            onExpenseHorizonChange = viewModel::onExpenseHorizonChange,
            onRunIncome = viewModel::runPredictIncome,
            onRunExpenses = viewModel::runPredictExpenses,
            onWealthHorizonChange = viewModel::onWealthHorizonChange,
            onRunWealth = viewModel::runPredictWealth,
            onDismissError = viewModel::onErrorBannerDismiss,
        )
    }
}

@Composable
private fun PredictionContent(
    state: PredictionUiState,
    padding: PaddingValues,
    onIncomeHorizonChange: (Int) -> Unit,
    onExpenseHorizonChange: (Int) -> Unit,
    onRunIncome: () -> Unit,
    onRunExpenses: () -> Unit,
    onWealthHorizonChange: (Int) -> Unit,
    onRunWealth: () -> Unit,
    onDismissError: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.errorBanner != null) {
            item {
                ErrorBanner(
                    message = state.errorBanner,
                    onDismiss = onDismissError,
                )
            }
        }

        item { IntroCard() }

        item {
            IncomePredictionCard(
                horizonMonths = state.incomeHorizonMonths,
                isLoading = state.isPredictingIncome,
                prediction = state.incomePrediction,
                onHorizonChange = onIncomeHorizonChange,
                onRun = onRunIncome,
            )
        }

        item {
            ExpensePredictionCard(
                horizonMonths = state.expenseHorizonMonths,
                isLoading = state.isPredictingExpenses,
                prediction = state.expensePrediction,
                onHorizonChange = onExpenseHorizonChange,
                onRun = onRunExpenses,
            )
        }

        item {
            WealthPredictionCard(
                horizonMonths = state.wealthHorizonMonths,
                isLoading = state.isPredictingWealth,
                prediction = state.wealthPrediction,
                onHorizonChange = onWealthHorizonChange,
                onRun = onRunWealth,
            )
        }
    }
}

// ============================================================================
// Card introdutorio
// ============================================================================

@Composable
private fun IntroCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Modelos personalizados",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "As predicoes sao geradas a partir do SEU historico de " +
                        "renda e despesa. Quanto mais registros voce tem, mais " +
                        "precisa fica.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// ============================================================================
// Income card
// ============================================================================

@Composable
fun IncomePredictionCard(
    horizonMonths: Int,
    isLoading: Boolean,
    prediction: IncomePrediction?,
    onHorizonChange: (Int) -> Unit,
    onRun: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Predicao de renda",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Regressao linear sobre os recebimentos historicos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizonField(
                label = "Horizonte (1-60 meses)",
                value = horizonMonths,
                onChange = onHorizonChange,
                enabled = !isLoading,
            )

            Button(
                onClick = onRun,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Prever renda")
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            prediction?.let { IncomeResultBlock(it) }
        }
    }
}

@Composable
private fun IncomeResultBlock(prediction: IncomePrediction) {
    HorizontalDivider()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryRow(
            label = "Renda mensal media projetada",
            value = formatBrl(prediction.expectedMonthlyIncome),
            highlight = true,
        )
        SummaryRow(
            label = "Crescimento anual estimado",
            value = formatGrowthRate(prediction.annualGrowthRate),
        )
        SummaryRow(
            label = "Volatilidade mensal (sigma)",
            value = formatBrl(prediction.residualVolatilityMonthly),
        )

        // Pontos da projecao - primeiros 6 e ultimo, para nao poluir
        // a UI em horizontes longos
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Projecao mes a mes",
            style = MaterialTheme.typography.titleSmall,
        )
        prediction.projection.takeProjectionSnippet().forEach { point ->
            ProjectionRow(point)
        }

        MetricsRow(prediction.metrics)
    }
}

private fun List<IncomePredictionPoint>.takeProjectionSnippet(): List<IncomePredictionPoint> =
    if (size <= 7) this else take(6) + last()

@Composable
private fun ProjectionRow(point: IncomePredictionPoint) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("M+${point.monthIndex}", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = formatBrl(point.predictedAmount),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// ============================================================================
// Expense card
// ============================================================================

@Composable
fun ExpensePredictionCard(
    horizonMonths: Int,
    isLoading: Boolean,
    prediction: ExpensePrediction?,
    onHorizonChange: (Int) -> Unit,
    onRun: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Predicao de despesas",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Random Forest por categoria sobre o historico de gastos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizonField(
                label = "Horizonte (1-12 meses)",
                value = horizonMonths,
                onChange = onHorizonChange,
                enabled = !isLoading,
            )

            Button(
                onClick = onRun,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Prever despesas")
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            prediction?.let { ExpenseResultBlock(it) }
        }
    }
}

@Composable
private fun ExpenseResultBlock(prediction: ExpensePrediction) {
    HorizontalDivider()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryRow(
            label = "Despesa mensal total prevista",
            value = formatBrl(prediction.expectedMonthlyExpense),
            highlight = true,
        )

        Spacer(Modifier.height(4.dp))
        Text("Por categoria", style = MaterialTheme.typography.titleSmall)

        // Mostra apenas categorias com valor > 0 (Random Forest tende a
        // devolver 0.0 para categorias sem historico)
        prediction.byCategory
            .filter { it.predictedAmount > 0.0 }
            .sortedByDescending { it.predictedAmount }
            .forEach { CategoryRow(it) }

        MetricsRow(prediction.metrics)
    }
}

@Composable
private fun CategoryRow(item: ExpenseCategoryPrediction) {
    // O label centralizado vive em EnumLabels.kt como extension function
    // `ExpenseCategory.label()`. Categorias desconhecidas (vindas do
    // backend em formato novo) caem no rawCategory.
    val label = item.category?.label() ?: item.rawCategory
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = formatBrl(item.predictedAmount),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// ============================================================================
// Wealth card (serie temporal de patrimonio)
// ============================================================================

@Composable
fun WealthPredictionCard(
    horizonMonths: Int,
    isLoading: Boolean,
    prediction: WealthPrediction?,
    onHorizonChange: (Int) -> Unit,
    onRun: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Predicao de patrimonio",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Serie temporal (ARIMA) sobre o patrimonio acumulado " +
                    "reconstruido do seu historico de receitas e despesas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizonField(
                label = "Horizonte (1-60 meses)",
                value = horizonMonths,
                onChange = onHorizonChange,
                enabled = !isLoading,
            )

            Button(
                onClick = onRun,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Prever patrimonio")
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            prediction?.let { WealthResultBlock(it) }
        }
    }
}

@Composable
private fun WealthResultBlock(prediction: WealthPrediction) {
    HorizontalDivider()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryRow(
            label = "Patrimonio projetado (fim do horizonte)",
            value = formatBrl(prediction.expectedFinalWealth),
            highlight = true,
        )
        SummaryRow(
            label = "Crescimento mensal medio",
            value = formatGrowthRate(prediction.monthlyGrowthRate),
        )

        Spacer(Modifier.height(4.dp))
        Text(
            "Patrimonio: realizado (azul) x projetado",
            style = MaterialTheme.typography.titleSmall,
        )
        WealthChart(prediction)

        MetricsRow(prediction.metrics)
    }
}

/**
 * Gráfico realizado × projetado: a série histórica (real) e a projeção
 * futura, posicionada logo após o último ponto real via eixo x explícito.
 */
@Composable
private fun WealthChart(prediction: WealthPrediction) {
    val history = prediction.history
    val projection = prediction.projection
    if (history.size < 2) return

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(prediction.predictionId) {
        val realX = history.map { it.monthIndex }
        val realY = history.map { it.amount }
        val lastX = history.last().monthIndex
        val lastY = history.last().amount
        // A projeção continua a partir do último ponto real (continuidade visual).
        val projX = listOf(lastX) + projection.map { lastX + it.monthIndex }
        val projY = listOf(lastY) + projection.map { it.predictedAmount }

        modelProducer.runTransaction {
            lineSeries {
                series(x = realX, y = realY)
                series(x = projX, y = projY)
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
    )
}

// ============================================================================
// Componentes reutilizaveis
// ============================================================================

@Composable
private fun HorizonField(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { raw -> raw.toIntOrNull()?.let(onChange) },
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
    )
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun MetricsRow(metrics: PredictionMetrics) {
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = {},
            label = { Text("MAE ${formatMetric(metrics.mae)}") },
            shape = RoundedCornerShape(8.dp),
        )
        AssistChip(
            onClick = {},
            label = { Text("RMSE ${formatMetric(metrics.rmse)}") },
            shape = RoundedCornerShape(8.dp),
        )
        AssistChip(
            onClick = {},
            label = { Text("R² ${formatR2(metrics.r2)}") },
            shape = RoundedCornerShape(8.dp),
        )
    }
}

// ============================================================================
// Formatters locais (especificos desta tela)
// ============================================================================

/**
 * Formata growth rate. Convertemos a fracao em percentual e usamos o
 * [formatPercent] global. Ex.: 0.10 -> "10,0%".
 */
private fun formatGrowthRate(rate: Double): String {
    val asPercent = BigDecimal(rate * 100.0).setScale(1, RoundingMode.HALF_UP)
    val sign = if (rate >= 0) "+" else ""
    return sign + formatPercent(asPercent)
}

/** Numerico curto, 1 decimal. Ex.: "1234,5". */
private fun formatMetric(value: Double): String =
    String.format(java.util.Locale("pt", "BR"), "%.1f", value)

/** R² com 2 decimais, pode ser negativo. */
private fun formatR2(value: Double): String =
    String.format(java.util.Locale("pt", "BR"), "%.2f", value)
