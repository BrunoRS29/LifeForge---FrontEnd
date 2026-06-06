package com.lifeforge.presentation.screen.dashboard

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeforge.domain.model.WealthProjection
import com.lifeforge.domain.usecase.FinancialSnapshot
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatBrlCompact
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

/**
 * Card de evolução patrimonial: real (hoje) × projetado (Seção 8.4 do TCC).
 *
 * Como ainda não armazenamos um histórico mensal de patrimônio, o ponto
 * "real" é o patrimônio atual (mês 0) e a curva é a PROJEÇÃO determinística
 * para os próximos 5 anos (juros compostos com aportes, fórmula 6.1):
 *  - linha de cima: patrimônio projetado COM rendimento (~8% a.a.)
 *  - linha de baixo: somente aportes acumulados (sem rendimento)
 *
 * A distância entre as linhas é o efeito dos juros compostos — a mensagem
 * central do app: planejar investindo supera guardar parado.
 */
@Composable
fun WealthProjectionCard(
    snapshot: FinancialSnapshot,
    modifier: Modifier = Modifier,
) {
    val initial = snapshot.totalAssets.toDouble()
    val contribution = (snapshot.monthlyIncome - snapshot.monthlyExpenses).toDouble()
    val months = 60
    val annualReturn = 0.08

    val projected = remember(initial, contribution) {
        WealthProjection.project(initial, contribution, annualReturn, months)
    }
    val contributionsOnly = remember(initial, contribution) {
        WealthProjection.project(initial, contribution, 0.0, months)
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(projected, contributionsOnly) {
        modelProducer.runTransaction {
            lineSeries {
                series(y = projected)
                series(y = contributionsOnly)
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
                "Evolução do patrimônio (5 anos)",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "A partir de ${formatBrl(snapshot.totalAssets)} hoje, aportando " +
                    "${formatBrl(contribution)}/mês a ~8% a.a. Linha de baixo: " +
                    "sem rendimento (só aportes).",
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
                    .height(200.dp),
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Em 5 anos: projetado ${formatBrlCompact(projected.last().toBigDecimal())} " +
                    "vs. ${formatBrlCompact(contributionsOnly.last().toBigDecimal())} sem investir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
