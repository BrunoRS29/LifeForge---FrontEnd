package com.lifeforge.presentation.screen.simulation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifeforge.domain.model.GoalHealthEvaluator
import com.lifeforge.domain.model.HistogramBucket
import com.lifeforge.domain.model.SimulationResult
import com.lifeforge.domain.model.TrajectoryBand
import com.lifeforge.presentation.common.AutoSizeText
import com.lifeforge.presentation.common.BrlAxisFormatter
import com.lifeforge.presentation.common.ChartLegend
import com.lifeforge.presentation.common.ContentCard
import com.lifeforge.presentation.common.HealthStatusPill
import com.lifeforge.presentation.common.ShapeIcon
import com.lifeforge.presentation.common.formatAxisBrl
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatBrlCompact
import com.lifeforge.presentation.common.formatCount
import com.lifeforge.presentation.common.formatProbability
import com.lifeforge.presentation.common.goalHealthColor
import com.lifeforge.presentation.common.healthStatusLabel
import com.lifeforge.presentation.common.monthAxisSpacing
import com.lifeforge.presentation.common.monthItemPlacer
import com.lifeforge.presentation.common.rememberAreaLine
import com.lifeforge.presentation.common.rememberBandEdgeLine
import com.lifeforge.presentation.common.rememberFitToWidthZoom
import com.lifeforge.presentation.common.rememberSolidLine
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape

/*
 * Resultado da Simulação de Monte Carlo, compartilhado pela simulação manual e
 * pela calibrada pela IA (TCC, Seções 8.3 / 12.2): medidor da probabilidade de
 * sucesso, cenários nomeados (P10/P50/P90), fan chart P10–P90, histograma dos
 * patrimônios finais, estatísticas e curva de percentis — com tempo de execução.
 */

/** Bloco completo do resultado, na ordem de leitura: resposta, cenários, gráficos, números. */
@Composable
internal fun ResultSection(result: SimulationResult, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProbabilityHero(result)
        ScenarioTiles(result)
        FanChart(trajectory = result.trajectory)
        HistogramChart(
            buckets = result.histogram,
            targetAmount = result.targetAmount,
            successProbability = result.successProbability,
        )
        StatisticsCard(result)
        PercentilesChart(percentiles = result.percentiles)
    }
}

// ============================================================================
// Probabilidade de sucesso
// ============================================================================

@Composable
private fun ProbabilityHero(result: SimulationResult) {
    val status = GoalHealthEvaluator.statusFor(result.successProbability)
    ContentCard {
        ProbabilityGauge(probability = result.successProbability, modifier = Modifier.fillMaxWidth())
        HealthStatusPill(
            status = status,
            text = healthStatusLabel(status),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            "${formatCount(result.numSimulations)} cenários · meta de ${formatBrl(result.targetAmount)} · " +
                "calculado em ${formatCount(result.executionTimeMs.toInt())} ms",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Medidor (gauge) semicircular de probabilidade de sucesso — requisito do TCC
 * (Seção 8.3 / 12.2): "gauge de probabilidade (0–100%)". Um arco de fundo e um
 * arco proporcional à probabilidade, que se preenche com a mola expressiva do
 * tema; o número aparece de imediato, sem esperar a animação.
 *
 * As cores seguem o semáforo da saúde das metas
 * ([com.lifeforge.domain.model.GoalHealthEvaluator]): primária (≥ 80%, o limiar
 * da otimização), secundária (≥ 50%) e erro (< 50%).
 */
@Composable
fun ProbabilityGauge(
    probability: Double,
    modifier: Modifier = Modifier,
) {
    val p = probability.coerceIn(0.0, 1.0).toFloat()
    val progress = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(p) { progress.animateTo(p, spec) }

    val arcColor = goalHealthColor(GoalHealthEvaluator.statusFor(probability))
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val description = "Probabilidade de sucesso: ${formatProbability(probability)}"

    Box(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            val strokePx = 22.dp.toPx()
            // Diâmetro limitado pela largura e por 2× a altura disponível.
            val diameter = minOf(size.width - strokePx, (size.height - strokePx / 2f) * 2f)
            val radius = diameter / 2f
            val topLeft = Offset(x = (size.width - diameter) / 2f, y = size.height - strokePx / 2f - radius)
            val arcSize = Size(diameter, diameter)
            val style = Stroke(width = strokePx, cap = StrokeCap.Round)

            // Semicírculo superior (180°) de fundo e o arco do valor (a mola pode
            // passar um pouco do alvo: o arco fica limitado ao semicírculo).
            drawArc(trackColor, 180f, 180f, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
            drawArc(
                color = arcColor,
                startAngle = 180f,
                sweepAngle = 180f * progress.value.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = style,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatProbability(probability),
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "de chance de atingir a meta",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ============================================================================
// Cenários nomeados
// ============================================================================

/**
 * Três cenários nomeados a partir dos percentis do Monte Carlo: pessimista
 * (P10), realista (P50) e otimista (P90). Cada um diz se alcança a meta — em
 * texto e na cor do ícone, nunca só na cor.
 */
@Composable
private fun ScenarioTiles(result: SimulationResult) {
    val pessimistic = result.percentiles["P10"] ?: result.worstCase
    val realistic = result.percentiles["P50"] ?: result.median
    val optimistic = result.percentiles["P90"] ?: result.bestCase
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Patrimônio final em três cenários",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val tile = Modifier
                .weight(1f)
                .fillMaxHeight()
            ScenarioTile("Pessimista", "P10", pessimistic, result.targetAmount, Icons.AutoMirrored.Outlined.TrendingDown, tile)
            ScenarioTile("Realista", "P50", realistic, result.targetAmount, Icons.AutoMirrored.Outlined.TrendingFlat, tile)
            ScenarioTile("Otimista", "P90", optimistic, result.targetAmount, Icons.AutoMirrored.Outlined.TrendingUp, tile)
        }
    }
}

@Composable
private fun ScenarioTile(
    title: String,
    percentile: String,
    value: Double,
    target: Double,
    icon: ImageVector,
    modifier: Modifier,
) {
    val reaches = value >= target
    val reachText = if (reaches) "atinge a meta" else "abaixo da meta"
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$title ($percentile): ${formatBrl(value)}, $reachText"
        },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapeIcon(
                    icon = icon,
                    containerColor = if (reaches) colors.primaryContainer else colors.errorContainer,
                    contentColor = if (reaches) colors.onPrimaryContainer else colors.onErrorContainer,
                    shape = MaterialShapes.Cookie4Sided.toShape(),
                    size = 36.dp,
                )
                Spacer(Modifier.weight(1f))
                Text(percentile, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
            Text(title, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            AutoSizeText(
                text = formatBrlCompact(value.toBigDecimal()),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = colors.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                reachText,
                style = MaterialTheme.typography.labelMedium,
                color = if (reaches) colors.primary else colors.error,
            )
        }
    }
}

// ============================================================================
// Estatísticas
// ============================================================================

@Composable
private fun StatisticsCard(result: SimulationResult) {
    ContentCard(
        title = "Estatísticas",
        supporting = "Patrimônio ao fim do horizonte, em R$ correntes; a média real desconta a inflação.",
    ) {
        StatRow("Média", formatBrl(result.mean))
        StatRow("Média real (sem inflação)", formatBrl(result.meanReal))
        StatRow("Mediana (P50)", formatBrl(result.median))
        StatRow("Desvio-padrão", formatBrl(result.standardDeviation))
        StatRow("Pior cenário", formatBrl(result.worstCase))
        StatRow("Melhor cenário", formatBrl(result.bestCase))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ============================================================================
// Fan chart
// ============================================================================

/**
 * Fan chart: faixas de percentis do patrimônio ao longo do tempo — requisito do
 * TCC (Seção 8.3 / 12.2): "gráfico de faixa (fan chart) com intervalos P10–P90".
 * A faixa clara vai do P10 ao P90, a mais forte do P25 ao P75, e a linha é a
 * mediana; a abertura do leque mostra a incerteza crescendo com o horizonte.
 */
@Composable
fun FanChart(
    trajectory: List<TrajectoryBand>,
    modifier: Modifier = Modifier,
) {
    if (trajectory.size < 2) return

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(trajectory) {
        modelProducer.runTransaction {
            lineSeries {
                // Ordem de desenho de cima para baixo: cada área cobre a anterior
                // (ver rememberBandEdgeLine); a mediana vem por último, por cima.
                series(y = trajectory.map { it.p90 })
                series(y = trajectory.map { it.p75 })
                series(y = trajectory.map { it.p25 })
                series(y = trajectory.map { it.p10 })
                series(y = trajectory.map { it.p50 })
            }
        }
    }

    val last = trajectory.last()
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    val primary = MaterialTheme.colorScheme.primary
    val outerBand = lerp(container, primary, 0.16f)
    val innerBand = lerp(container, primary, 0.34f)
    val edge = primary.copy(alpha = 0.5f)
    val p90 = rememberBandEdgeLine(lineColor = edge, areaColor = outerBand)
    val p75 = rememberBandEdgeLine(lineColor = edge, areaColor = innerBand)
    val p25 = rememberBandEdgeLine(lineColor = edge, areaColor = outerBand)
    val p10 = rememberBandEdgeLine(lineColor = edge, areaColor = container)
    val median = rememberSolidLine(primary)
    val itemPlacer = remember(last.monthIndex) { monthItemPlacer(monthAxisSpacing(last.monthIndex)) }
    val chartDescription = "Gráfico de leque da projeção em ${last.monthIndex} meses: " +
        "cenário pessimista ${formatBrlCompact(last.p10.toBigDecimal())}, " +
        "mediana ${formatBrlCompact(last.p50.toBigDecimal())}, " +
        "otimista ${formatBrlCompact(last.p90.toBigDecimal())}"

    ContentCard(
        modifier = modifier,
        containerColor = container,
        title = "Patrimônio ao longo do tempo",
        supporting = "Em ${last.monthIndex} meses: pessimista ${formatBrlCompact(last.p10.toBigDecimal())} · " +
            "mediana ${formatBrlCompact(last.p50.toBigDecimal())} · otimista ${formatBrlCompact(last.p90.toBigDecimal())}",
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(p90, p75, p25, p10, median),
                ),
                // Sem linhas de grade: as faixas opacas as cobririam só em parte.
                startAxis = VerticalAxis.rememberStart(valueFormatter = BrlAxisFormatter, guideline = null),
                bottomAxis = HorizontalAxis.rememberBottom(itemPlacer = itemPlacer, guideline = null),
            ),
            modelProducer = modelProducer,
            zoomState = rememberFitToWidthZoom(),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .semantics { contentDescription = chartDescription },
        )
        ChartLegend(
            entries = listOf(
                "Mediana (P50)" to primary,
                "Faixa P25–P75" to innerBand,
                "Faixa P10–P90" to outerBand,
            ),
        )
        Text(
            "Eixo horizontal em meses. Metade dos cenários termina dentro da faixa forte; oito em cada dez, dentro da clara.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ============================================================================
// Histograma
// ============================================================================

/**
 * Histograma dos patrimônios finais: cada coluna conta os cenários que
 * terminaram naquela faixa de R$. As colunas que alcançam a meta ficam na cor
 * primária — a área delas é a probabilidade de sucesso.
 */
@Composable
internal fun HistogramChart(
    buckets: List<HistogramBucket>,
    targetAmount: Double,
    successProbability: Double? = null,
) {
    if (buckets.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(buckets) {
        modelProducer.runTransaction {
            columnSeries { series(y = buckets.map { it.count }) }
        }
    }

    // Eixo X em R$: três rótulos de faixa (início, meio e fim) cabem sem sobrepor.
    val bottomItemPlacer = remember(buckets.size) {
        HorizontalAxis.ItemPlacer.aligned(spacing = { (buckets.size + 2) / 3 })
    }
    val bottomFormatter = remember(buckets) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt().coerceIn(0, buckets.lastIndex)
            formatAxisBrl(buckets[index].rangeStart)
        }
    }

    val reachedColor = MaterialTheme.colorScheme.primary
    val belowColor = MaterialTheme.colorScheme.outlineVariant
    val columnShape = remember { CorneredShape.rounded(topLeftPercent = 40, topRightPercent = 40) }
    val reached = rememberLineComponent(fill(reachedColor), thickness = 12.dp, shape = columnShape)
    val below = rememberLineComponent(fill(belowColor), thickness = 12.dp, shape = columnShape)
    // Primeira faixa cujo ponto médio já alcança a meta.
    val firstReached = remember(buckets, targetAmount) {
        buckets.indexOfFirst { (it.rangeStart + it.rangeEnd) / 2 >= targetAmount }.let { if (it < 0) buckets.size else it }
    }
    val columnProvider = remember(reached, below, firstReached) { TargetColumnProvider(below, reached, firstReached) }

    val chartDescription =
        "Histograma da distribuição dos patrimônios finais, de " +
            "${formatBrlCompact(buckets.first().rangeStart.toBigDecimal())} a " +
            "${formatBrlCompact(buckets.last().rangeEnd.toBigDecimal())}, " +
            "meta de ${formatBrl(targetAmount)}"

    ContentCard(
        title = "Distribuição dos patrimônios finais",
        supporting = "Quantos cenários terminaram em cada faixa de patrimônio (eixo em R$). " +
            "Colunas altas são os desfechos mais prováveis; quanto mais espalhadas, maior a incerteza.",
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(columnProvider = columnProvider, columnCollectionSpacing = 3.dp),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomFormatter,
                    itemPlacer = bottomItemPlacer,
                    guideline = null,
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
        ChartLegend(
            entries = listOf(
                "Atinge a meta (${formatBrlCompact(targetAmount.toBigDecimal())})" to reachedColor,
                "Fica abaixo" to belowColor,
            ),
        )
        if (successProbability != null) {
            Text(
                "As colunas que atingem a meta somam ${formatProbability(successProbability)} dos cenários — " +
                    "a probabilidade de sucesso.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Colunas abaixo da meta numa cor neutra e as que a atingem na cor primária. */
private class TargetColumnProvider(
    private val below: LineComponent,
    private val reached: LineComponent,
    private val firstReachedIndex: Int,
) : ColumnCartesianLayer.ColumnProvider {
    override fun getColumn(
        entry: ColumnCartesianLayerModel.Entry,
        seriesIndex: Int,
        extraStore: ExtraStore,
    ): LineComponent = if (entry.x >= firstReachedIndex) reached else below

    override fun getWidestSeriesColumn(seriesIndex: Int, extraStore: ExtraStore): LineComponent = reached
}

// ============================================================================
// Percentis
// ============================================================================

/**
 * Curva de percentis (P5…P95) do patrimônio final: o "leque" de resultados.
 * Curva íngreme = resultados espalhados (alta sensibilidade às incertezas).
 */
@Composable
internal fun PercentilesChart(percentiles: Map<String, Double>) {
    val presentKeys = listOf("P5", "P10", "P25", "P50", "P75", "P90", "P95").filter { it in percentiles }
    val values = presentKeys.map { percentiles.getValue(it) }
    if (values.size < 2) return

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        modelProducer.runTransaction { lineSeries { series(y = values) } }
    }
    // Eixo X com os nomes dos percentis (P5..P95) em vez do índice 0..6.
    val bottomFormatter = remember(presentKeys) {
        CartesianValueFormatter { _, value, _ -> presentKeys.getOrElse(value.toInt()) { "" } }
    }
    val line = rememberAreaLine(MaterialTheme.colorScheme.tertiary)
    val chartDescription = "Percentis do patrimônio final: " +
        presentKeys.zip(values).joinToString { (key, value) -> "$key ${formatBrlCompact(value.toBigDecimal())}" }

    ContentCard(
        title = "Percentis dos resultados",
        supporting = "Leia \"P25 = R$ X\" como: 25% dos cenários terminaram abaixo de X.",
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(lineProvider = LineCartesianLayer.LineProvider.series(line)),
                startAxis = VerticalAxis.rememberStart(valueFormatter = BrlAxisFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter, guideline = null),
            ),
            modelProducer = modelProducer,
            zoomState = rememberFitToWidthZoom(),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics { contentDescription = chartDescription },
        )
    }
}
