package com.lifeforge.presentation.screen.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeforge.domain.model.GoalHealth
import com.lifeforge.domain.model.GoalHealthStatus
import com.lifeforge.presentation.common.GoalHealthChip
import com.lifeforge.presentation.common.formatBrlCompact
import com.lifeforge.presentation.common.formatMonthsLeft
import com.lifeforge.presentation.common.goalHealthLabel

private const val MAX_GOALS_ON_DASHBOARD = 4

/**
 * Resumo das metas ativas com o indicador de saúde de cada uma (proposta,
 * Seção 8.4): a leitura de uma linha da última simulação. As metas que pedem
 * ação (em risco, atenção) aparecem primeiro.
 */
@Composable
fun GoalsHealthCard(
    goals: List<GoalHealth>,
    onOpenGoal: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Suas metas",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (goals.isEmpty()) {
                Text(
                    "Crie uma meta na aba Metas para acompanhar aqui a probabilidade de alcançá-la.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Text(
                summaryLine(goals),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            goals.take(MAX_GOALS_ON_DASHBOARD).forEachIndexed { index, health ->
                if (index > 0) HorizontalDivider()
                GoalHealthRow(health = health, onClick = { onOpenGoal(health.goal.id) })
            }
            if (goals.size > MAX_GOALS_ON_DASHBOARD) {
                Text(
                    "+ ${goals.size - MAX_GOALS_ON_DASHBOARD} na aba Metas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun GoalHealthRow(health: GoalHealth, onClick: () -> Unit) {
    val goal = health.goal
    val description = "${goal.name}, ${formatBrlCompact(goal.targetAmount)}, " +
        "${formatMonthsLeft(health.monthsLeft)}, ${goalHealthLabel(health)}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = "Abrir meta", role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                goal.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${formatBrlCompact(goal.targetAmount)} · ${formatMonthsLeft(health.monthsLeft)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GoalHealthChip(health)
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    return parts.joinToString(" · ") + " — com base na última simulação de cada meta."
}

/**
 * Atalho para a tela de inteligência preditiva (renda, despesas e
 * patrimônio projetados pelos modelos do microsserviço de IA).
 */
@Composable
fun PredictionsEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.Insights,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Inteligência preditiva",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "Projeções de renda, despesas e patrimônio aprendidas com o seu histórico, com as métricas de erro de cada modelo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
