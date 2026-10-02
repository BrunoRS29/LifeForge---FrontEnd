package com.lifeforge.presentation.screen.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.MetricComparison
import com.lifeforge.domain.model.SimulationResult
import com.lifeforge.domain.model.StrategyComparison
import com.lifeforge.domain.model.StrategyMetric
import com.lifeforge.domain.model.StrategySide
import com.lifeforge.presentation.common.ScreenLoading
import com.lifeforge.presentation.common.AutoSizeText
import com.lifeforge.presentation.common.BrlAxisFormatter
import com.lifeforge.presentation.common.formatAnnualRate
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatBrlCompact
import com.lifeforge.presentation.common.formatDateTime
import com.lifeforge.presentation.common.formatProbability
import com.lifeforge.presentation.common.monthAxisSpacing
import com.lifeforge.presentation.common.monthItemPlacer
import com.lifeforge.presentation.common.rememberFitToWidthZoom
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.util.Locale
import kotlin.math.abs

/**
 * Comparação lado a lado de duas estratégias para a mesma meta (proposta,
 * Seção 8.3): veredito, resultados (com o melhor de cada linha marcado),
 * premissas que mudaram e a mediana de cada estratégia ao longo do tempo.
 */
@Composable
fun SimulationCompareScreen(
    onNavigateBack: () -> Unit,
    viewModel: SimulationCompareViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comparar estratégias") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val comparison = state.comparison
            when {
                state.isLoading -> ScreenLoading()
                comparison != null -> CompareContent(comparison)
                else -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        state.errorMessage ?: "Não foi possível carregar as simulações.",
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::load) { Text("Tentar de novo") }
                }
            }
        }
    }
}

@Composable
private fun CompareContent(comparison: StrategyComparison) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VerdictCard(comparison)
        StrategyHeaders(comparison.a, comparison.b)
        ComparisonCard(
            title = "Resultados",
            subtitle = "Marcado o melhor de cada linha. Valores ao fim do horizonte, em R$ correntes.",
            rows = comparison.outcomes,
        )
        if (comparison.premises.isNotEmpty()) {
            ComparisonCard(
                title = "Premissas",
                subtitle = "Em destaque, o que muda de uma estratégia para a outra.",
                rows = comparison.premises,
            )
        }
        MedianTrajectoriesCard(comparison.a, comparison.b)
    }
}

// ============================================================================
// Veredito
// ============================================================================

@Composable
private fun VerdictCard(comparison: StrategyComparison) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.CompareArrows,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                verdictText(comparison),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Leitura em linguagem simples da comparação (separada para teste). */
internal fun verdictText(comparison: StrategyComparison): String {
    val a = comparison.a.successProbability
    val b = comparison.b.successProbability
    val delta = abs(comparison.probabilityDeltaPp)
    val deltaText = String.format(Locale("pt", "BR"), "%.1f", delta)
    val contributionNote = comparison.premises
        .firstOrNull { it.metric == StrategyMetric.MONTHLY_CONTRIBUTION && it.differs }
        ?.let { row ->
            val cheaper = if (row.a < row.b) "A" else "B"
            " A estratégia $cheaper exige ${formatBrl(abs(row.a - row.b))}/mês a menos de aporte."
        }
        .orEmpty()
    return if (comparison.technicalTie) {
        val tiebreak = when (comparison.recommended) {
            StrategySide.A -> " Pelo critério de desempate, a estratégia A é mais eficiente."
            StrategySide.B -> " Pelo critério de desempate, a estratégia B é mais eficiente."
            null -> ""
        }
        "Empate técnico: as chances de atingir a meta diferem só $deltaText p.p. " +
            "(${formatProbability(a)} × ${formatProbability(b)}), dentro da margem de erro das " +
            "simulações.$contributionNote$tiebreak"
    } else {
        val (best, bestP, worstP) = if (comparison.recommended == StrategySide.B) Triple("B", b, a) else Triple("A", a, b)
        "A estratégia $best tem $deltaText p.p. a mais de chance de atingir a meta " +
            "(${formatProbability(bestP)} contra ${formatProbability(worstP)}).$contributionNote"
    }
}

// ============================================================================
// Cabeçalhos das estratégias
// ============================================================================

@Composable
private fun StrategyHeaders(a: SimulationResult, b: SimulationResult) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StrategyHeader("A", a, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        StrategyHeader("B", b, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
    }
}

@Composable
private fun StrategyHeader(label: String, result: SimulationResult, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Text("Estratégia $label", style = MaterialTheme.typography.titleSmall)
            }
            Text(
                formatDateTime(result.createdAt) + if (result.inputs?.calibrated == true) " · com IA" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatProbability(result.successProbability),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ============================================================================
// Tabela de comparação
// ============================================================================

@Composable
private fun ComparisonCard(title: String, subtitle: String, rows: List<MetricComparison>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Mesmo espaçamento das linhas: os rótulos A/B ficam sobre os valores.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.weight(1.2f))
                Text("A", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("B", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider()
                ComparisonRow(row)
            }
        }
    }
}

@Composable
private fun ComparisonRow(row: MetricComparison) {
    val label = metricLabel(row.metric)
    val a = formatMetric(row.metric, row.a)
    val b = formatMetric(row.metric, row.b)
    val better = when (row.better) {
        StrategySide.A -> ", melhor: A"
        StrategySide.B -> ", melhor: B"
        null -> ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: A $a, B $b$better" },
        // Folga entre as colunas: sem ela, o valor de A encostava no ícone de B.
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.2f))
        MetricValue(a, highlight = row.better == StrategySide.A, emphasize = row.differs && row.metric.higherIsBetter == null, modifier = Modifier.weight(1f))
        MetricValue(b, highlight = row.better == StrategySide.B, emphasize = row.differs && row.metric.higherIsBetter == null, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MetricValue(text: String, highlight: Boolean, emphasize: Boolean, modifier: Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (highlight) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
        // Uma linha só: em telas estreitas o valor em destaque (negrito + ícone)
        // reduz um pouco a fonte em vez de quebrar "R$ 189,1 / mil".
        AutoSizeText(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (highlight || emphasize) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.End,
            ),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

internal fun metricLabel(metric: StrategyMetric): String = when (metric) {
    StrategyMetric.SUCCESS_PROBABILITY -> "Chance de sucesso"
    StrategyMetric.MEDIAN -> "Mediana (P50)"
    StrategyMetric.PESSIMISTIC_P10 -> "Pessimista (P10)"
    StrategyMetric.OPTIMISTIC_P90 -> "Otimista (P90)"
    StrategyMetric.MEAN_REAL -> "Média real (sem inflação)"
    StrategyMetric.MONTHLY_CONTRIBUTION -> "Aporte mensal"
    StrategyMetric.EXPECTED_RETURN -> "Retorno esperado"
    StrategyMetric.VOLATILITY -> "Volatilidade"
    StrategyMetric.HORIZON_MONTHS -> "Horizonte"
    StrategyMetric.INFLATION -> "Inflação"
    StrategyMetric.UNEMPLOYMENT -> "Risco de desemprego"
}

internal fun formatMetric(metric: StrategyMetric, value: Double): String = when (metric) {
    StrategyMetric.SUCCESS_PROBABILITY -> formatProbability(value)
    StrategyMetric.MEDIAN, StrategyMetric.PESSIMISTIC_P10, StrategyMetric.OPTIMISTIC_P90,
    StrategyMetric.MEAN_REAL -> formatBrlCompact(value.toBigDecimal())
    StrategyMetric.MONTHLY_CONTRIBUTION -> formatBrl(value)
    StrategyMetric.EXPECTED_RETURN, StrategyMetric.VOLATILITY, StrategyMetric.INFLATION,
    StrategyMetric.UNEMPLOYMENT -> formatAnnualRate(value) + " a.a."
    StrategyMetric.HORIZON_MONTHS -> formatHorizon(value.toInt())
}

// ============================================================================
// Medianas ao longo do tempo
// ============================================================================

@Composable
private fun MedianTrajectoriesCard(a: SimulationResult, b: SimulationResult) {
    if (a.trajectory.size < 2 || b.trajectory.size < 2) return
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(a.id, b.id) {
        modelProducer.runTransaction {
            lineSeries {
                series(x = a.trajectory.map { it.monthIndex }, y = a.trajectory.map { it.p50 })
                series(x = b.trajectory.map { it.monthIndex }, y = b.trajectory.map { it.p50 })
            }
        }
    }
    val description = "Mediana do patrimônio ao longo do tempo: estratégia A termina em " +
        "${formatBrlCompact(a.trajectory.last().p50.toBigDecimal())}, estratégia B em " +
        formatBrlCompact(b.trajectory.last().p50.toBigDecimal())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Mediana do patrimônio ao longo do tempo",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Eixo X em meses, eixo Y em R$. Linha na cor de cada estratégia (A e B).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            val months = maxOf(a.trajectory.last().monthIndex, b.trajectory.last().monthIndex)
            val itemPlacer = remember(months) { monthItemPlacer(monthAxisSpacing(months)) }
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = BrlAxisFormatter),
                    bottomAxis = HorizontalAxis.rememberBottom(itemPlacer = itemPlacer),
                ),
                modelProducer = modelProducer,
                zoomState = rememberFitToWidthZoom(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .semantics { contentDescription = description },
            )
        }
    }
}
