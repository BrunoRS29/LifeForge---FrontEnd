package com.lifeforge.presentation.screen.goal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.Goal
import com.lifeforge.presentation.common.EmptyState
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatDate
import com.lifeforge.presentation.common.label

/**
 * Lista de metas. Layout: TopAppBar com refresh, LazyColumn de cards,
 * EmptyState quando vazia e FAB extendido para criar nova.
 *
 * Cada card mostra nome (titleLarge), categoria como chip, valor alvo
 * em destaque (headlineSmall) e a data alvo formatada. Tocar abre o
 * detalhe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsListScreen(
    onGoalClick: (Long) -> Unit,
    onCreateGoal: () -> Unit,
    viewModel: GoalsListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Metas") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateGoal,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Nova meta") },
            )
        },
    ) { padding ->
        // Atualização por gesto (puxar para baixo), como nos apps modernos.
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.errorBanner != null) {
                    ErrorBanner(
                        message = state.errorBanner!!,
                        onDismiss = viewModel::onErrorBannerDismiss,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                when {
                    state.goals.isEmpty() && !state.isRefreshing -> {
                        // LazyColumn + fillParentMaxSize mantém o gesto de
                        // puxar funcionando mesmo sem metas cadastradas.
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                EmptyState(
                                    title = "Nenhuma meta ainda",
                                    description = "Crie sua primeira meta e simule seu plano financeiro.",
                                    icon = Icons.Outlined.Flag,
                                    modifier = Modifier.fillParentMaxSize(),
                                )
                            }
                        }
                    }
                    else -> GoalsList(state.goals, onGoalClick)
                }
            }
        }
    }
}

@Composable
private fun GoalsList(
    goals: List<Goal>,
    onGoalClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,  // espaço pro FAB
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = goals, key = { it.id }) { goal ->
            GoalCard(goal = goal, onClick = { onGoalClick(goal.id) })
        }
    }
}

@Composable
private fun GoalCard(goal: Goal, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                AssistChip(
                    onClick = onClick,
                    label = { Text(goal.category.label()) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = formatBrl(goal.targetAmount),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Até ${formatDate(goal.targetDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
