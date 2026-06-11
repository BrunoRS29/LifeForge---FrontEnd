package com.lifeforge.presentation.screen.simulation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeforge.domain.model.TrajectoryBand
import com.lifeforge.presentation.common.formatBrlCompact
import com.lifeforge.presentation.common.formatProbability
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

/**
 * Medidor (gauge) semicircular de probabilidade de sucesso.
 *
 * Requisito explicito do TCC (Secao 8.3 / 12.2): "gauge de probabilidade
 * (0-100%)". Desenhado com Canvas - um arco de fundo (track) e um arco de
 * valor proporcional a probabilidade, com a porcentagem ao centro.
 *
 * Cores seguem o mesmo semaforo do card de resultado:
 *  - verde   (>= 70%)
 *  - ambar   (>= 50%)
 *  - vermelho (< 50%)
 */
@Composable
fun ProbabilityGauge(
    probability: Double,
    modifier: Modifier = Modifier,
) {
    val p = probability.coerceIn(0.0, 1.0).toFloat()
    // O arco "preenche" suavemente até o valor final — dá vida ao resultado
    // sem atrapalhar a leitura (o número aparece imediatamente).
    val animatedP by animateFloatAsState(
        targetValue = p,
        animationSpec = tween(durationMillis = 900),
        label = "gauge-progress",
    )

    val arcColor = when {
        p >= 0.70f -> MaterialTheme.colorScheme.primary
        p >= 0.50f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val strokePx = 26f
            // Diametro limitado pela largura e por 2x a altura disponivel.
            val diameter = minOf(size.width - strokePx, (size.height - strokePx) * 2f)
            val radius = diameter / 2f
            // Posiciona a "linha do diametro" do semicirculo perto da base.
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = size.height - strokePx / 2f - radius,
            )
            val arcSize = Size(diameter, diameter)

            // Arco de fundo (180 graus = semicirculo superior).
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // Arco de valor proporcional a probabilidade (animado).
            drawArc(
                color = arcColor,
                startAngle = 180f,
                sweepAngle = 180f * animatedP,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            Text(
                text = formatProbability(probability),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
            Text(
                text = "probabilidade de sucesso",
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
            )
            Spacer(Modifier.height(2.dp))
        }
    }
}

/**
 * Fan chart: faixa de percentis (P10–P90) do patrimônio ao longo do tempo.
 *
 * Requisito explicito do TCC (Secao 8.3 / 12.2): "gráfico de faixa (fan
 * chart) com intervalos P10-P90". Renderiza cinco séries de linha (P10, P25,
 * P50, P75, P90) — a abertura entre elas mês a mês é o "leque" que mostra
 * como a incerteza cresce com o horizonte.
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
                series(y = trajectory.map { it.p10 })
                series(y = trajectory.map { it.p25 })
                series(y = trajectory.map { it.p50 })
                series(y = trajectory.map { it.p75 })
                series(y = trajectory.map { it.p90 })
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Projeção do patrimônio ao longo do tempo",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Cada linha é um percentil dos cenários simulados — de baixo " +
                    "para cima: P10, P25, mediana (P50), P75 e P90. Eixo X em " +
                    "meses, eixo Y em R$. O leque abre conforme a incerteza " +
                    "cresce no horizonte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            val last = trajectory.last()
            Text(
                "No mês ${last.monthIndex}: pessimista ${formatBrlCompact(last.p10.toBigDecimal())} • " +
                    "mediana ${formatBrlCompact(last.p50.toBigDecimal())} • " +
                    "otimista ${formatBrlCompact(last.p90.toBigDecimal())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            // Descrição para leitores de tela (TalkBack): o gráfico em si é
            // só desenho — o resumo textual carrega a informação.
            val chartDescription = "Gráfico de leque da projeção em ${last.monthIndex} meses: " +
                "cenário pessimista ${formatBrlCompact(last.p10.toBigDecimal())}, " +
                "mediana ${formatBrlCompact(last.p50.toBigDecimal())}, " +
                "otimista ${formatBrlCompact(last.p90.toBigDecimal())}"
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(),
                ),
                modelProducer = modelProducer,
                scrollState = rememberVicoScrollState(scrollEnabled = true),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .semantics { contentDescription = chartDescription },
            )
        }
    }
}
