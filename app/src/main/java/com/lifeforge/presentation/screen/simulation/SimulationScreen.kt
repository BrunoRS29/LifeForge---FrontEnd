package com.lifeforge.presentation.screen.simulation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.HistogramBucket
import com.lifeforge.domain.model.SimulationResult
import com.lifeforge.domain.model.SimulationSummary
import com.lifeforge.domain.model.SimulationInputs
import com.lifeforge.presentation.common.CurrencyField
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.BrlAxisFormatter
import com.lifeforge.presentation.common.formatAnnualRate
import com.lifeforge.presentation.common.formatCount
import com.lifeforge.presentation.common.formatAxisBrl
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.rememberFitToWidthZoom
import com.lifeforge.presentation.common.formatBrlCompact
import com.lifeforge.presentation.common.formatDateTime
import com.lifeforge.presentation.common.formatProbability
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
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
    onCompare: (goalId: Long, firstId: Long, secondId: Long) -> Unit = { _, _, _ -> },
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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
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
                    totalAssets = state.totalAssets,
                    onUseTotalAssets = viewModel::useTotalAssetsAsInitialCapital,
                )

                Button(
                    onClick = viewModel::runSimulation,
                    enabled = !state.isRunning && state.form.canRun,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.isRunning) "Simulando ${formatCount(state.form.numSimulations)} cenários…"
                        else "Executar simulação",
                    )
                }

                // Entrada animada do resultado — o bloco "desliza" para a tela
                // quando a simulação termina (motion sutil do Material 3).
                AnimatedVisibility(
                    visible = state.result != null,
                    enter = fadeIn() + expandVertically(),
                ) {
                    state.result?.let { result ->
                        Column {
                            Spacer(Modifier.height(8.dp))
                            ResultSection(result = result)
                        }
                    }
                }

                if (state.history.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
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

// ============================================================================
// Form
// ============================================================================

@Composable
private fun ParameterForm(
    form: SimulationForm,
    isRunning: Boolean,
    onChange: ((SimulationForm) -> SimulationForm) -> Unit,
    totalAssets: java.math.BigDecimal? = null,
    onUseTotalAssets: () -> Unit = {},
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
            if (totalAssets != null && totalAssets.signum() > 0) {
                TextButton(
                    onClick = onUseTotalAssets,
                    enabled = !isRunning,
                ) {
                    Text("Usar patrimônio total (${formatBrl(totalAssets.toDouble())})")
                }
            }
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
            Text(
                "Pré-preenchidas com o cenário mais seguro hoje: 100% do CDI " +
                    "(retorno e volatilidade da base de referência) e risco de " +
                    "desemprego típico do seu vínculo de trabalho. Ajuste se " +
                    "quiser simular outra carteira.",
                style = MaterialTheme.typography.bodySmall,
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
                "Iterações: ${formatCount(form.numSimulations)}",
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
internal fun ResultSection(result: SimulationResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Card destaque: gauge de probabilidade de sucesso (TCC 8.3 / 12.2).
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProbabilityGauge(probability = result.successProbability)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${formatCount(result.numSimulations)} cenários · " +
                        "executado em ${result.executionTimeMs} ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Cenários nomeados (diferencial): pessimista/realista/otimista.
        ScenariosCard(result)

        // Estatísticas-chave: mediana e média real (deflacionada pela inflação).
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

        FanChart(trajectory = result.trajectory)
        HistogramChart(
            buckets = result.histogram,
            targetAmount = result.targetAmount,
            successProbability = result.successProbability,
        )
        PercentilesChart(percentiles = result.percentiles)
    }
}

/**
 * Três cenários nomeados a partir dos percentis do Monte Carlo (diferencial de
 * TCC): pessimista (P10), realista (P50) e otimista (P90). Dá ao usuário uma
 * leitura imediata da faixa de resultados sem precisar interpretar gráficos.
 */
@Composable
private fun ScenariosCard(result: SimulationResult) {
    val pessimista = result.percentiles["P10"] ?: result.worstCase
    val realista = result.percentiles["P50"] ?: result.median
    val otimista = result.percentiles["P90"] ?: result.bestCase
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Cenários", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                "Patrimônio final em 3 cenários (percentis do Monte Carlo).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScenarioBox(Modifier.weight(1f), "Pessimista", "P10", pessimista)
                ScenarioBox(Modifier.weight(1f), "Realista", "P50", realista)
                ScenarioBox(Modifier.weight(1f), "Otimista", "P90", otimista)
            }
        }
    }
}

@Composable
private fun ScenarioBox(
    modifier: Modifier,
    title: String,
    percentile: String,
    value: Double,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(formatBrlCompact(value.toBigDecimal()), style = MaterialTheme.typography.titleSmall)
            Text(
                percentile,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
internal fun HistogramChart(
    buckets: List<HistogramBucket>,
    targetAmount: Double,
    successProbability: Double? = null,
) {
    if (buckets.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    androidx.compose.runtime.LaunchedEffect(buckets) {
        modelProducer.runTransaction {
            columnSeries {
                series(y = buckets.map { it.count })
            }
        }
    }

    // Eixo X em R$: cada coluna é uma faixa de patrimônio; sem isto o eixo
    // mostrava apenas o índice do bucket (0..49), ilegível para o usuário.
    // Três rótulos de faixa (início, meio e fim da distribuição) cabem sem sobrepor.
    val bottomItemPlacer = remember(buckets.size) {
        HorizontalAxis.ItemPlacer.aligned(spacing = { (buckets.size + 2) / 3 })
    }
    val bottomFormatter = remember(buckets) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt().coerceIn(0, buckets.lastIndex)
            formatAxisBrl(buckets[index].rangeStart)
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
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Cada coluna conta quantos cenários simulados terminaram com o " +
                    "patrimônio daquela faixa (eixo X, em R$). Colunas mais altas = " +
                    "desfechos mais prováveis; quanto mais espalhadas, maior a " +
                    "incerteza. Use para ver onde os resultados se concentram em " +
                    "relação à meta de ${formatBrl(targetAmount)}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (successProbability != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "A parte da distribuição à direita da meta corresponde à " +
                        "probabilidade de sucesso (${formatProbability(successProbability)}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            // Resumo textual do histograma para leitores de tela (TalkBack).
            val chartDescription =
                "Histograma da distribuição dos patrimônios finais, de " +
                    "${formatBrlCompact(buckets.first().rangeStart.toBigDecimal())} a " +
                    "${formatBrlCompact(buckets.last().rangeEnd.toBigDecimal())}, " +
                    "meta de ${formatBrl(targetAmount)}"
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = bottomFormatter,
                        itemPlacer = bottomItemPlacer,
                    ),
                ),
                modelProducer = modelProducer,
                // Distribuição inteira na tela: rolando, abria na cauda esquerda
                // e o grosso dos cenários ficava fora da vista.
                zoomState = rememberFitToWidthZoom(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .semantics { contentDescription = chartDescription },
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
    val presentKeys = orderedKeys.filter { it in percentiles }
    val values = presentKeys.map { percentiles.getValue(it) }

    if (values.size < 2) return

    val modelProducer = remember { CartesianChartModelProducer() }

    androidx.compose.runtime.LaunchedEffect(values) {
        modelProducer.runTransaction {
            lineSeries {
                series(y = values)
            }
        }
    }

    // Eixo X com os nomes dos percentis (P5..P95) em vez do índice 0..6.
    val bottomFormatter = remember(presentKeys) {
        CartesianValueFormatter { _, value, _ ->
            presentKeys.getOrElse(value.toInt()) { "" }
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
                "Percentis dos resultados",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "A linha mostra o patrimônio final do cenário pessimista (P5) ao " +
                    "otimista (P95). Leia \"P25 = R$ X\" como: 25% dos cenários " +
                    "terminaram abaixo de X. Curva mais íngreme = resultados mais " +
                    "espalhados (mais incerteza).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            // Resumo textual dos percentis para leitores de tela (TalkBack).
            val chartDescription = "Percentis do patrimônio final: " +
                presentKeys.zip(values).joinToString { (key, value) ->
                    "$key ${formatBrlCompact(value.toBigDecimal())}"
                }
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = BrlAxisFormatter),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .semantics { contentDescription = chartDescription },
            )
        }
    }
}

/** Helper de sanitização — usado nos callbacks dos campos. */

// ============================================================================
// Histórico
// ============================================================================

@Composable
private fun HistorySection(
    history: List<SimulationSummary>,
    onOpen: (Long) -> Unit = {},
    isCompareMode: Boolean = false,
    selection: List<Long> = emptyList(),
    onToggleCompareMode: () -> Unit = {},
    onToggleSelection: (Long) -> Unit = {},
    onCompare: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Simulações anteriores",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                // Comparar estratégias lado a lado exige ao menos duas rodadas.
                if (history.size >= 2) {
                    TextButton(onClick = onToggleCompareMode) {
                        Text(if (isCompareMode) "Cancelar" else "Comparar")
                    }
                }
            }
            Text(
                if (isCompareMode) {
                    "Escolha duas rodadas para comparar as estratégias lado a lado."
                } else {
                    "Toque numa rodada para reabrir o resultado e os gráficos dela."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            history.forEach { sim ->
                val selected = sim.id in selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .then(
                            if (isCompareMode) {
                                Modifier.toggleable(
                                    value = selected,
                                    role = Role.Checkbox,
                                    onValueChange = { onToggleSelection(sim.id) },
                                )
                            } else {
                                Modifier.clickable(onClickLabel = "Reabrir resultado desta rodada") {
                                    onOpen(sim.id)
                                }
                            }
                        )
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isCompareMode) {
                        // A linha inteira é o alvo de toque (toggleable); o checkbox só sinaliza.
                        Checkbox(checked = selected, onCheckedChange = null)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            formatDateTime(sim.createdAt) + if (sim.inputs?.calibrated == true) " · com IA" else "",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        sim.inputs?.let { inputs ->
                            Text(
                                strategyLine(inputs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
            if (isCompareMode) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onCompare,
                    enabled = selection.size == 2,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Comparar selecionadas (${selection.size}/2)")
                }
            }
        }
    }
}

/** "Aporte R$ 2.000 · 8% a.a. · vol. 15% · 20 anos": a estratégia em uma linha. */
internal fun strategyLine(inputs: SimulationInputs): String =
    "Aporte ${formatBrl(inputs.monthlyContribution)} · ${formatAnnualRate(inputs.expectedReturnAnnual)} a.a. · " +
        "vol. ${formatAnnualRate(inputs.volatilityAnnual)} · ${formatHorizon(inputs.horizonMonths)}"


/** Horizonte legível: 240 → "20 anos", 30 → "2 anos e 6 meses", 8 → "8 meses". */
internal fun formatHorizon(months: Int): String {
    val years = months / 12
    val rest = months % 12
    val y = when (years) { 0 -> null; 1 -> "1 ano"; else -> "$years anos" }
    val m = when (rest) { 0 -> null; 1 -> "1 mês"; else -> "$rest meses" }
    return listOfNotNull(y, m).joinToString(" e ").ifEmpty { "0 meses" }
}
