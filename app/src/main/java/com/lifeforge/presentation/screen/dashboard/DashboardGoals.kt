package com.lifeforge.presentation.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeforge.domain.model.GoalHealth
import com.lifeforge.domain.model.GoalHealthStatus
import com.lifeforge.domain.model.RecurringPattern
import com.lifeforge.domain.usecase.FinancialSnapshot
import com.lifeforge.presentation.common.GoalHealthChip
import com.lifeforge.presentation.common.SectionHeader
import com.lifeforge.presentation.common.ShapeIcon
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatBrlCompact
import com.lifeforge.presentation.common.formatMonthsLeft
import com.lifeforge.presentation.common.goalHealthLabel
import com.lifeforge.presentation.common.icon

private const val MAX_GOALS_ON_DASHBOARD = 4
private const val MAX_RECURRING_PER_GROUP = 5

/**
 * Metas ativas com o indicador de saúde de cada uma (proposta, Seção 8.4): a
 * leitura de uma linha da última simulação. As metas que pedem ação (em risco,
 * atenção) aparecem primeiro. Lista segmentada do Material 3 Expressive.
 */
@Composable
fun GoalsHealthSection(
    goals: List<GoalHealth>,
    onOpenGoal: (Long) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Suas metas",
            supporting = if (goals.isEmpty()) null else summaryLine(goals),
            action = if (goals.isEmpty()) null else {
                { TextButton(onClick = onSeeAll) { Text("Ver todas") } }
            },
        )
        if (goals.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.extraLarge,
                onClick = onSeeAll,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShapeIcon(icon = Icons.Outlined.Flag)
                    Text(
                        "Crie uma meta na aba Metas para acompanhar aqui a probabilidade de alcançá-la.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            return@Column
        }
        val shown = goals.take(MAX_GOALS_ON_DASHBOARD)
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            shown.forEachIndexed { index, health ->
                GoalHealthItem(
                    health = health,
                    index = index,
                    count = shown.size,
                    onClick = { onOpenGoal(health.goal.id) },
                )
            }
        }
        if (goals.size > MAX_GOALS_ON_DASHBOARD) {
            Text(
                "+ ${goals.size - MAX_GOALS_ON_DASHBOARD} na aba Metas",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun GoalHealthItem(health: GoalHealth, index: Int, count: Int, onClick: () -> Unit) {
    val goal = health.goal
    val details = "${formatBrlCompact(goal.targetAmount)} · ${formatMonthsLeft(health.monthsLeft)}"
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "${goal.name}, $details, ${goalHealthLabel(health)}"
        },
        leadingContent = { ShapeIcon(icon = goal.category.icon()) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(details, color = MaterialTheme.colorScheme.onSurfaceVariant)
                GoalHealthChip(health)
            }
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        Text(goal.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** "2 no caminho · 1 em risco · 1 sem simulação". */
private fun summaryLine(goals: List<GoalHealth>): String {
    val counts = goals.groupingBy { it.status }.eachCount()
    val parts = listOfNotNull(
        counts[GoalHealthStatus.ON_TRACK]?.let { "$it no caminho" },
        counts[GoalHealthStatus.ATTENTION]?.let { "$it em atenção" },
        counts[GoalHealthStatus.AT_RISK]?.let { "$it em risco" },
        counts[GoalHealthStatus.NOT_SIMULATED]?.let { "$it sem simulação" },
        counts[GoalHealthStatus.PENDING_SYNC]?.let { "$it aguardando sincronização" },
    )
    return parts.joinToString(" · ") + ", pela última simulação de cada meta"
}

/**
 * Atalho para a tela de inteligência preditiva (renda, despesas e patrimônio
 * projetados pelos modelos do microsserviço de IA).
 */
@Composable
fun PredictionsEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShapeIcon(
                icon = Icons.Outlined.Insights,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                shape = MaterialShapes.Sunny.toShape(),
                size = 48.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Inteligência preditiva", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Renda, despesas e patrimônio projetados a partir do seu histórico, com as métricas de erro de cada modelo.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** Receitas e despesas que se repetem no histórico (3+ meses), em listas segmentadas. */
@Composable
fun RecurringPatternsSection(snapshot: FinancialSnapshot, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Recorrências detectadas",
            supporting = "Itens que aparecem em 3 ou mais meses do seu histórico",
        )
        if (snapshot.recurringIncomes.isNotEmpty()) {
            RecurringGroup("Receitas", snapshot.recurringIncomes, isIncome = true)
        }
        if (snapshot.recurringExpenses.isNotEmpty()) {
            RecurringGroup("Despesas", snapshot.recurringExpenses, isIncome = false)
        }
    }
}

@Composable
private fun RecurringGroup(title: String, patterns: List<RecurringPattern>, isIncome: Boolean) {
    val shown = patterns.take(MAX_RECURRING_PER_GROUP)
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            shown.forEachIndexed { index, pattern ->
                val amount = formatBrl(pattern.monthlyAmount) + "/mês"
                SegmentedListItem(
                    onClick = {},
                    enabled = false,
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = shown.size),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    supportingContent = {
                        Text("${pattern.months} meses", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingContent = {
                        Text(
                            text = if (isIncome) amount else "−$amount",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isIncome) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                ) {
                    Text(pattern.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
