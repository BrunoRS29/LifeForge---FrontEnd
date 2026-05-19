package com.lifeforge.presentation.screen.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.HistogramBucket
import com.lifeforge.domain.model.SimulationResult
import com.lifeforge.domain.model.SimulationSummary
import com.lifeforge.presentation.common.CurrencyField
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatBrlCompact
import com.lifeforge.presentation.common.formatDateTime
import com.lifeforge.presentation.common.formatProbability
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

/**
 * Tela de Simulação Monte Carlo (Fase 4.4).
 *
 * Layout (de cima para baixo):
 * 1. TopAppBar com voltar
 * 2. Cabeçalho com nome da meta
 * 3. Form de parâmetros (collapsible em sprint futura — por ora sempre visível)
 * 4. Botão "Executar simulação" + LinearProgress durante execução
 * 5. **Resultado** (visível após primeira execução):
 *    - Card destaque com probabilidade de sucesso
 *    - Estatísticas-chave (mean, median, P5, P95)
 *    - Histograma da distribuição final (Vico column chart)
 *    - Curva de percentiles (Vico line chart)
 * 6. Histórico de simulações anteriores da meta (cards compactos)
 *
 * Os gráficos Vico usam `CartesianChartModelProducer` — uma fonte
 * reativa de dados que o chart consome. Quando o `SimulationResult`
 * muda, o modelo é regenerado e o gráfico anima a transição.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(
    onNavigateBack: () -> Unit,
    viewModel: SimulationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.goalName?.let { "Simular — $it" } ?: "Simulação",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isRunning) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.errorBanner != null) {
                    ErrorBanner(
                        message = state.errorBanner!!,
                        onDismiss = viewModel::onErrorBannerDismiss,
                    )
                }

                ParameterForm(
                    form = state.form,
                    isRunning = state.isRunning,
                    onChange = viewModel::onFormChange,
                )

                Button(
                    onClick = viewModel::runSimulation,
                    enabled = !state.isRunning && state.form.canRun,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.isRunning) "Simulando ${state.form.numSimulations} cenários…"
                        else "Executar simulação",
                    )
                }

                state.result?.let { result ->
                    Spacer(Modifier.height(8.dp))
                    ResultSection(result = result)
                }

                if (state.history.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    HistorySection(history = state.history)
                }
            }
        }
    }
}

// ============================================================================
// Form
// ============================================================================

@Composable
private fun ParameterForm(
    form: SimulationForm,
    isRunning: Boolean,
    onChange: ((SimulationForm) -> SimulationForm) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Parâmetros", style = MaterialTheme.typography.titleMedium)

            CurrencyField(
                value = form.initialCapitalInput,
                onValueChange = { v ->
                    onChange { it.copy(initialCapitalInput = v.asMonetaryInput()) }
                },
                label = "Capital inicial (R$)",
                enabled = !isRunning,
            )
            CurrencyField(
                value = form.monthlyContributionInput,
                onValueChange = { v ->
                    onChange { it.copy(monthlyContributionInput = v.asMonetaryInput()) }
                },
                label = "Aporte mensal (R$)",
                enabled = !isRunning,
            )
            CurrencyField(
                value = form.targetAmountInput,
                onValueChange = { v ->
                    onChange { it.copy(targetAmountInput = v.asMonetaryInput()) }
                },
                label = "Meta (R$)",
                enabled = !isRunning,
            )
            IntField(
                value = form.horizonMonthsInput,
                onValueChange = { v -> onChange { it.copy(horizonMonthsInput = v) } },
                label = "Horizonte (meses)",
                enabled = !isRunning,
            )

            Spacer(Modifier.height(4.dp))
            Text(
                "Premissas de mercado",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CurrencyField(
                value = form.expectedReturnInput,
                onValueChange = { v ->
                    onChange { it.copy(expectedReturnInput = v.asMonetaryInput()) }
                },
                label = "Retorno esperado anual (ex.: 0,08 = 8%)",
                enabled = !isRunning,
            )
            CurrencyField(
                value = form.volatilityInput,
                onValueChange = { v ->
                    onChange { it.copy(volatilityInput = v.asMonetaryInput()) }
                },
                label = "Volatilidade anual (ex.: 0,15 = 15%)",
                enabled = !isRunning,
            )
            CurrencyField(
                value = form.inflationInput,
                onValueChange = { v ->
                    onChange { it.copy(inflationInput = v.asMonetaryInput()) }
                },
                label = "Inflação anual (ex.: 0,04 = 4%)",
                enabled = !isRunning,
            )

            Spacer(Modifier.height(4.dp))
            Text(
                "Eventos adversos",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CurrencyField(
                value = form.unemploymentProbInput,
                onValueChange = { v ->
                    onChange { it.copy(unemploymentProbInput = v.asMonetaryInput()) }
                },
                label = "Prob. de desemprego anual (ex.: 0,05 = 5%)",
                imeAction = ImeAction.Done,
                enabled = !isRunning,
            )

            Spacer(Modifier.height(4.dp))
            Text(
                "Iterações: ${form.numSimulations}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "Mínimo 10.000 (especificação do TCC). " +
                    "Mais iterações = resultado mais estável, mais tempo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = form.numSimulations.toFloat(),
                onValueChange = { v ->
                    // Ajusta para múltiplos de 1000 para o slider ficar discreto.
                    val rounded = (v.toInt() / 1000) * 1000
                    onChange { it.copy(numSimulations = rounded.coerceAtLeast(10_000)) }
                },
                valueRange = 10_000f..50_000f,
                steps = 39,  // 40 valores: 10k, 11k, ..., 50k
                enabled = !isRunning,
            )
        }
    }
}

@Composable
private fun IntField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ============================================================================
// Resultado
// ============================================================================

@Composable
private fun ResultSection(result: SimulationResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Card destaque: probabilidade de sucesso.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (result.successProbability >= 0.70)
                    MaterialTheme.colorScheme.primaryContainer
                else if (result.successProbability >= 0.50)
                    MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Probabilidade de sucesso",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    formatProbability(result.successProbability),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${result.numSimulations} cenários · " +
                        "executado em ${result.executionTimeMs} ms",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Estatísticas-chave em grid 2x2.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox(
                modifier = Modifier.weight(1f),
                label = "Mediana",
                value = formatBrlCompact(result.median.toBigDecimal()),
            )
            StatBox(
                modifier = Modifier.weight(1f),
                label = "Média (real)",
                value = formatBrlCompact(result.meanReal.toBigDecimal()),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox(
                modifier = Modifier.weight(1f),
                label = "P5 (pessimista)",
                value = result.percentiles["P5"]
                    ?.let { formatBrlCompact(it.toBigDecimal()) } ?: "—",
            )
            StatBox(
                modifier = Modifier.weight(1f),
                label = "P95 (otimista)",
                value = result.percentiles["P95"]
                    ?.let { formatBrlCompact(it.toBigDecimal()) } ?: "—",
            )
        }

        HistogramChart(buckets = result.histogram, targetAmount = result.targetAmount)
        PercentilesChart(percentiles = result.percentiles)
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ============================================================================
// Gráficos Vico
// ============================================================================

/**
 * Histograma da distribuição de patrimônios finais. Cada bucket vira
 * uma coluna; eixo X mostra o valor inicial do bucket em formato compacto
 * (R$ 100k, R$ 1mi), eixo Y mostra a contagem de cenários.
 *
 * O `CartesianChartModelProducer` recebe os dados via `runTransaction`
 * — Vico recompõe o gráfico automaticamente.
 */
@Composable
private fun HistogramChart(buckets: List<HistogramBucket>, targetAmount: Double) {
    if (buckets.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    androidx.compose.runtime.LaunchedEffect(buckets) {
        modelProducer.runTransaction {
            columnSeries {
                series(y = buckets.map { it.count })
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Distribuição dos patrimônios finais",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Linha pontilhada indica a meta (${formatBrl(targetAmount)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(),
                ),
                modelProducer = modelProducer,
                scrollState = rememberVicoScrollState(scrollEnabled = true),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        }
    }
}

/**
 * Curva de percentiles (P5, P10, P25, P50, P75, P90, P95) em linha.
 *
 * Útil para visualizar o "leque" de resultados — quão larga é a
 * distribuição. Distribuição estreita = previsibilidade alta;
 * distribuição larga = alta sensibilidade aos parâmetros estocásticos.
 */
@Composable
private fun PercentilesChart(percentiles: Map<String, Double>) {
    val orderedKeys = listOf("P5", "P10", "P25", "P50", "P75", "P90", "P95")
    val values = orderedKeys.mapNotNull { key -> percentiles[key] }

    if (values.size < 2) return

    val modelProducer = remember { CartesianChartModelProducer() }

    androidx.compose.runtime.LaunchedEffect(values) {
        modelProducer.runTransaction {
            lineSeries {
                series(y = values)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Percentiles dos resultados",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Patrimônio final em diferentes cenários (do pessimista ao otimista)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        }
    }
}

/** Helper de sanitização — usado nos callbacks dos campos. */

// ============================================================================
// Histórico
// ============================================================================

@Composable
private fun HistorySection(history: List<SimulationSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Simulações anteriores",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            history.forEach { sim ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            formatDateTime(sim.createdAt),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Mediana: ${formatBrl(sim.median)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        formatProbability(sim.successProbability),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
