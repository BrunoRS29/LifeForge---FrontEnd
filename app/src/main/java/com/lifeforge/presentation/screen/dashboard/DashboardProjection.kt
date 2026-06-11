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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lifeforge.domain.model.ProjectionInputs
import com.lifeforge.domain.model.ReferenceData
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.model.UserProfile
import com.lifeforge.domain.model.WealthProjection
import com.lifeforge.domain.usecase.FinancialSnapshot
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatBrlCompact
import com.lifeforge.presentation.common.parseCurrencyInput
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlin.math.roundToInt

private const val DEFAULT_INFLATION = 0.045   // premissa-base (4,5% a.a.)
private const val DEFAULT_MONTHS = 60         // 5 anos quando não há perfil

/**
 * Card de evolução patrimonial: real (hoje) × projetado (Seção 8.4 do TCC).
 *
 * Fase 2: a projeção agora é PERSONALIZADA pelo perfil:
 *  - horizonte = até a idade desejada de aposentadoria (senão 5 anos);
 *  - renda cresce ao ritmo do crescimento salarial informado;
 *  - despesas corrigidas pela inflação;
 *  - retorno anual conforme o perfil de risco.
 *
 * A linha de cima é o patrimônio COM rendimento; a de baixo, só os aportes
 * acumulados. A distância é o efeito dos juros compostos.
 */
@Composable
fun WealthProjectionCard(
    snapshot: FinancialSnapshot,
    profile: UserProfile?,
    riskProfile: RiskProfile?,
    referenceData: ReferenceData?,
    modifier: Modifier = Modifier,
) {
    // Premissas da base de referencia do backend; fallback nas constantes
    // locais quando offline ou ainda nao carregadas.
    val annualInflation = referenceData?.inflationAnnualMean ?: DEFAULT_INFLATION
    val annualReturn = referenceData?.returnForRiskProfile(riskProfile)
        ?: WealthProjection.returnForRiskProfile(riskProfile)
    val horizonMonths = monthsToRetirement(profile)
    val months = horizonMonths ?: DEFAULT_MONTHS
    val annualSalaryGrowth = profile?.expectedSalaryGrowth?.let(::parsePercent)
        ?: referenceData?.salaryGrowthAnnualMean
        ?: annualInflation
    val monthlySalary = snapshot.monthlySalary.toDouble().takeIf { it > 0.0 }
        ?: profile?.monthlySalary?.let { parseCurrencyInput(it)?.toDouble() }
        ?: 0.0
    val monthlyExpenses = snapshot.monthlyExpenses.toDouble()

    // Ativos reais do perfil (imóvel valoriza, veículos depreciam) e filhos
    // (custo por idade) — só entram quando o usuário informa; taxas vêm da base.
    val propertyValue = profile?.propertyValue?.let { parseCurrencyInput(it)?.toDouble() } ?: 0.0
    val vehiclesValue = profile?.vehiclesValue?.let { parseCurrencyInput(it)?.toDouble() } ?: 0.0
    val totalDebt = profile?.totalDebt?.let { parseCurrencyInput(it)?.toDouble() } ?: 0.0
    val childrenAges = parseAges(profile?.childrenAges)

    val inputs = ProjectionInputs(
        initialWealth = snapshot.totalAssets.toDouble(),
        monthlyIncome = monthlySalary,
        monthlyExpenses = monthlyExpenses,
        annualReturn = annualReturn,
        annualSalaryGrowth = annualSalaryGrowth,
        annualInflation = annualInflation,
        months = months,
        initialPropertyValue = propertyValue,
        annualPropertyAppreciation = referenceData?.realEstateAppreciationAnnual ?: 0.0,
        initialVehiclesValue = vehiclesValue,
        annualVehicleDepreciation = referenceData?.vehicleDepreciationAnnual ?: 0.0,
        childrenAges = childrenAges,
        childCostByAge = referenceData?.childCostByAge ?: emptyList(),
        initialDebt = totalDebt,
    )
    val proj = remember(inputs) { WealthProjection.projectDynamic(inputs) }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(proj) {
        modelProducer.runTransaction {
            lineSeries {
                series(y = proj.projected)
                series(y = proj.contributionsOnly)
            }
        }
    }

    val years = (months / 12.0).roundToInt().coerceAtLeast(1)
    val childCost0 = childrenAges.sumOf { referenceData?.childMonthlyCost(it) ?: 0.0 }
    val contribution0 = (monthlySalary - monthlyExpenses - childCost0).coerceAtLeast(0.0)
    val initialNetWorth = snapshot.totalAssets.toDouble() + propertyValue + vehiclesValue - totalDebt
    val personalized = horizonMonths != null ||
        profile?.monthlySalary != null || profile?.expectedSalaryGrowth != null

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (horizonMonths != null) "Evolução do patrimônio até a aposentadoria"
                else "Evolução do patrimônio ($years anos)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                buildString {
                    append("De ${formatBrl(initialNetWorth.toBigDecimal())} hoje, aportando ~")
                    append("${formatBrl(contribution0)}/mês")
                    append(" por $years anos · retorno ~${pct(annualReturn)} a.a.")
                    append(" · salário +${pct(annualSalaryGrowth)}/ano · inflação ${pct(annualInflation)}/ano.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!personalized) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Preencha seu perfil (idade, aposentadoria, salário) para personalizar esta projeção.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(12.dp))

            // Resumo textual para leitores de tela (TalkBack).
            val chartDescription = "Gráfico de evolução em $years anos: " +
                "investindo, ${formatBrlCompact(proj.finalProjected.toBigDecimal())}; " +
                "apenas guardando, ${formatBrlCompact(proj.finalContributionsOnly.toBigDecimal())}"
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .semantics { contentDescription = chartDescription },
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Linha de cima: patrimônio investindo (juros compostos). Linha de " +
                    "baixo: apenas acumulando os aportes, sem rendimento. Eixo X em " +
                    "meses, eixo Y em R$ — a distância entre as linhas é o que os " +
                    "juros fazem por você.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Em $years anos: projetado ${formatBrlCompact(proj.finalProjected.toBigDecimal())} " +
                    "vs. ${formatBrlCompact(proj.finalContributionsOnly.toBigDecimal())} sem investir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Meses até a aposentadoria a partir do perfil; null se idade/aposentadoria ausentes. */
private fun monthsToRetirement(profile: UserProfile?): Int? {
    val age = profile?.age ?: return null
    val retire = profile.retirementAge ?: return null
    if (retire <= age) return null
    return ((retire - age) * 12).coerceAtMost(600)
}

/** "5" -> 0.05 ; "5,5" -> 0.055. */
private fun parsePercent(raw: String): Double? =
    raw.replace(',', '.').toDoubleOrNull()?.let { it / 100.0 }

/** "3, 7" -> [3, 7]; ignora valores inválidos/fora de 0..120. */
private fun parseAges(raw: String?): List<Int> =
    raw?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.filter { it in 0..120 } ?: emptyList()

private fun pct(fraction: Double): String = "${(fraction * 100).roundToInt()}%"
