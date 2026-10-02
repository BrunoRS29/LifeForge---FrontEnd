package com.lifeforge.presentation.screen.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.GoalHealth
import com.lifeforge.domain.model.GoalHealthStatus
import com.lifeforge.presentation.common.EmptyState
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.GoalHealthChip
import com.lifeforge.presentation.common.RefreshableBox
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.ShapeIcon
import com.lifeforge.presentation.common.TabTopAppBar
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatDate
import com.lifeforge.presentation.common.formatMonthsLeft
import com.lifeforge.presentation.common.goalHealthColor
import com.lifeforge.presentation.common.goalHealthLabel
import com.lifeforge.presentation.common.icon
import com.lifeforge.presentation.common.label
import com.lifeforge.presentation.common.readableWidth
import kotlin.math.roundToInt

/**
 * Lista de metas: barra grande que recolhe ao rolar, cards com o anel de
 * probabilidade da última simulação (Material 3 Expressive) e o botão "Nova meta",
 * que encolhe para só o ícone enquanto a lista rola. Em janelas largas as metas
 * ficam em duas colunas (layout de feed). Puxar para baixo atualiza.
 */
@Composable
fun GoalsListScreen(
    onGoalClick: (Long) -> Unit,
    onCreateGoal: () -> Unit,
    viewModel: GoalsListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val gridState = rememberLazyGridState()
    val fabExpanded by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TabTopAppBar(
                title = "Metas",
                subtitle = goalsSubtitle(state.goals),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateGoal,
                expanded = fabExpanded,
                // Recolhido, o botão fica só com o ícone: aí o ícone carrega o rótulo.
                icon = { Icon(Icons.Rounded.Add, contentDescription = if (fabExpanded) null else "Nova meta") },
                text = { Text("Nova meta") },
            )
        },
    ) { padding ->
        RefreshableBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 340.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .readableWidth(),
                contentPadding = PaddingValues(
                    start = ScreenPadding,
                    end = ScreenPadding,
                    top = 8.dp,
                    bottom = 104.dp, // espaço para o botão flutuante
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.errorBanner != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ErrorBanner(message = state.errorBanner!!, onDismiss = viewModel::onErrorBannerDismiss)
                    }
                }
                if (state.goals.isEmpty() && !state.isRefreshing) {
                    // Dentro da grade, o gesto de puxar para atualizar continua valendo.
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            title = "Nenhuma meta ainda",
                            description = "Crie sua primeira meta e descubra, com milhares de cenários simulados, a chance de alcançá-la.",
                            icon = Icons.Outlined.Flag,
                            modifier = Modifier.padding(top = 48.dp),
                        )
                    }
                }
                items(items = state.goals, key = { it.goal.id }) { health ->
                    GoalCard(
                        health = health,
                        onClick = { onGoalClick(health.goal.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/** "3 metas · 2 no caminho" — ou um convite, sem metas. */
internal fun goalsSubtitle(goals: List<GoalHealth>): String {
    if (goals.isEmpty()) return "Planeje e simule seus objetivos"
    val total = if (goals.size == 1) "1 meta" else "${goals.size} metas"
    val onTrack = goals.count { it.status == GoalHealthStatus.ON_TRACK }
    return if (onTrack > 0) "$total · $onTrack no caminho" else total
}

@Composable
private fun GoalCard(health: GoalHealth, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val goal = health.goal
    val accent = goalHealthColor(health.status)
    val probability = health.latest?.successProbability
    val description = "${goal.name}, ${goal.category.label()}, meta de ${formatBrl(goal.targetAmount)} " +
        "até ${formatDate(goal.targetDate)}, ${formatMonthsLeft(health.monthsLeft)}, ${goalHealthLabel(health)}"
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ShapeIcon(icon = goal.category.icon(), size = 48.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        goal.category.label(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Anel ondulado com a probabilidade de sucesso da última simulação.
                Box(contentAlignment = Alignment.Center) {
                    CircularWavyProgressIndicator(
                        progress = { (probability ?: 0.0).toFloat() },
                        color = accent,
                        trackColor = accent.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp),
                    )
                    Text(
                        text = probability?.let { "${(it * 100).roundToInt()}%" } ?: "—",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    formatBrl(goal.targetAmount),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Até ${formatDate(goal.targetDate)} · ${formatMonthsLeft(health.monthsLeft)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GoalHealthChip(health)
        }
    }
}
