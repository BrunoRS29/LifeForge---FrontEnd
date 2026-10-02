package com.lifeforge.presentation.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lifeforge.domain.model.ReferenceData
import com.lifeforge.domain.usecase.FinancialSnapshot
import com.lifeforge.presentation.common.ShapeIcon
import com.lifeforge.presentation.common.formatBrlCompact
import kotlin.math.roundToInt

private const val DEFAULT_SAFE_WITHDRAWAL_RATE = 0.04   // regra dos 4% (fallback)
private const val FIRE_RETURN_FALLBACK = 0.11           // moderado (fallback)
private const val MAX_PROJECTION_MONTHS = 1_200         // 100 anos (cap do loop)

/**
 * Cartão de Independência Financeira (FI/RE) — diferencial de TCC.
 *
 * Patrimônio-alvo = gastos anuais ÷ taxa de retirada segura (regra dos 4% =
 * 25× os gastos anuais). Mostra o progresso atual e, no ritmo de aporte atual,
 * uma estimativa de quantos anos faltam.
 *
 * Some dos cálculos usa o SALÁRIO (não a renda total) como base do aporte: o
 * rendimento dos ativos já é capturado pela taxa de retorno que compõe o
 * patrimônio — evita contar o rendimento duas vezes.
 */
@Composable
fun FinancialIndependenceCard(
    snapshot: FinancialSnapshot,
    referenceData: ReferenceData?,
    modifier: Modifier = Modifier,
) {
    val annualExpenses = snapshot.monthlyExpenses.toDouble() * 12.0
    if (annualExpenses <= 0.0) return   // sem gastos não dá pra estimar FI/RE

    val swr = referenceData?.safeWithdrawalRate ?: DEFAULT_SAFE_WITHDRAWAL_RATE
    val target = annualExpenses / swr
    val current = snapshot.totalAssets.toDouble()
    val progress = (current / target).coerceIn(0.0, 1.0)
    val pct = (progress * 100).roundToInt()

    val annualReturn = referenceData?.returnForRiskProfile(null) ?: FIRE_RETURN_FALLBACK
    val monthlyContribution =
        (snapshot.monthlySalary.toDouble() - snapshot.monthlyExpenses.toDouble()).coerceAtLeast(0.0)
    val yearsToFi = yearsToTarget(current, monthlyContribution, annualReturn, target)

    val outlook = when {
        progress >= 1.0 -> "Você já atingiu a independência financeira."
        yearsToFi == null -> "No ritmo atual, aumente o aporte para chegar lá."
        else -> "No ritmo atual, faltam cerca de $yearsToFi anos."
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ShapeIcon(
                    icon = Icons.Outlined.BeachAccess,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = MaterialShapes.Sunny.toShape(),
                    size = 48.dp,
                )
                Column(Modifier.weight(1f)) {
                    Text("Independência financeira", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Meta de ${formatBrlCompact(target.toBigDecimal())} (${(1 / swr).roundToInt()}× seus gastos anuais)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("$pct%", style = MaterialTheme.typography.headlineSmallEmphasized)
            }
            // Barra ondulada do Material 3 Expressive: o progresso "vivo" até a meta.
            LinearWavyProgressIndicator(
                progress = { progress.toFloat() },
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Progresso até a independência financeira: $pct%"
                    },
            )
            Text(
                text = "Você já tem ${formatBrlCompact(current.toBigDecimal())}. $outlook",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Anos até o patrimônio atingir [target] com aporte mensal fixo e retorno
 * anual [annualReturn]. Retorna null se não atingir em [MAX_PROJECTION_MONTHS]
 * (ex.: aporte zero e patrimônio abaixo da meta).
 */
private fun yearsToTarget(
    initial: Double,
    monthlyContribution: Double,
    annualReturn: Double,
    target: Double,
): Int? {
    if (initial >= target) return 0
    if (monthlyContribution <= 0.0 && annualReturn <= 0.0) return null
    val r = (1.0 + annualReturn).pow(1.0 / 12.0) - 1.0
    var wealth = initial
    var month = 0
    while (wealth < target && month < MAX_PROJECTION_MONTHS) {
        wealth = wealth * (1.0 + r) + monthlyContribution
        month++
    }
    return if (wealth >= target) (month / 12.0).roundToInt() else null
}

private fun Double.pow(exp: Double): Double = Math.pow(this, exp)
