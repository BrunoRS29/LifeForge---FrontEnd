package com.lifeforge.presentation.screen.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LowPriority
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalHealth
import com.lifeforge.domain.model.isLocalOnly
import com.lifeforge.presentation.common.ActionButton
import com.lifeforge.presentation.common.ActionEmphasis
import com.lifeforge.presentation.common.BottomActionBar
import com.lifeforge.presentation.common.DetailTopAppBar
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.GoalHealthChip
import com.lifeforge.presentation.common.GroupItem
import com.lifeforge.presentation.common.ListGroup
import com.lifeforge.presentation.common.LoadingOverlay
import com.lifeforge.presentation.common.ScreenLoading
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.ShapeIcon
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatDate
import com.lifeforge.presentation.common.formatMonthsLeft
import com.lifeforge.presentation.common.formatProbability
import com.lifeforge.presentation.common.goalHealthColor
import com.lifeforge.presentation.common.icon
import com.lifeforge.presentation.common.label
import com.lifeforge.presentation.common.readableWidth
import kotlin.math.roundToInt

/**
 * Detalhe da meta: valor-alvo em destaque, a saúde pela última simulação (anel
 * de probabilidade), os dados da meta em lista segmentada e as duas ações
 * principais — "Simular com IA" e "Simular" — fixas na base da tela, ao alcance
 * do polegar. Editar fica na barra superior; apagar, no menu "mais opções",
 * com confirmação (ação destrutiva e rara).
 */
@Composable
fun GoalDetailScreen(
    onNavigateBack: () -> Unit,
    onEdit: (goalId: Long) -> Unit,
    onSimulate: (goalId: Long) -> Unit,
    onSimulateWithAi: (goalId: Long) -> Unit = {},
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.eventsFlow.collect { event ->
            when (event) {
                GoalDetailEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    val goal = state.goal
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopAppBar(
                title = goal?.name ?: "Meta",
                subtitle = goal?.category?.label(),
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (goal != null) {
                        IconButton(onClick = { onEdit(goal.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar meta")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "Mais opções")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Apagar meta", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        menuOpen = false
                                        showDeleteDialog = true
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (goal != null) {
                SimulateActions(
                    canSimulate = !goal.isLocalOnly(),
                    onSimulate = { onSimulate(goal.id) },
                    onSimulateWithAi = { onSimulateWithAi(goal.id) },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                goal != null -> GoalDetailContent(
                    goal = goal,
                    health = state.health,
                    errorBanner = state.errorBanner,
                    onErrorDismiss = viewModel::onErrorBannerDismiss,
                )
                else -> ScreenLoading()
            }
            LoadingOverlay(visible = state.isDeleting)
        }

        if (showDeleteDialog) {
            DeleteConfirmDialog(
                goalName = goal?.name,
                onConfirm = {
                    showDeleteDialog = false
                    viewModel.delete()
                },
                onDismiss = { showDeleteDialog = false },
            )
        }
    }
}

/** Ações de simulação fixas na base: a primária (IA, um toque) e a com parâmetros. */
@Composable
private fun SimulateActions(canSimulate: Boolean, onSimulate: () -> Unit, onSimulateWithAi: () -> Unit) {
    BottomActionBar {
        if (!canSimulate) {
            Text(
                "Meta salva neste aparelho: a simulação fica disponível depois que ela for enviada ao servidor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(
                text = "Simular",
                icon = Icons.Outlined.AutoGraph,
                onClick = onSimulate,
                enabled = canSimulate,
                emphasis = ActionEmphasis.Tonal,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                text = "Simular com IA",
                icon = Icons.Outlined.AutoAwesome,
                onClick = onSimulateWithAi,
                enabled = canSimulate,
                modifier = Modifier.weight(1.3f),
            )
        }
    }
}

@Composable
private fun GoalDetailContent(
    goal: Goal,
    health: GoalHealth?,
    errorBanner: String?,
    onErrorDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .readableWidth()
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (errorBanner != null) {
            ErrorBanner(message = errorBanner, onDismiss = onErrorDismiss)
        }
        TargetHero(goal = goal, health = health)
        if (health != null) {
            HealthCard(health = health, canSimulate = !goal.isLocalOnly())
        }
        ListGroup(
            title = "Detalhes",
            items = listOf(
                GroupItem("Categoria", supporting = goal.category.label(), icon = Icons.Outlined.Category),
                GroupItem("Data alvo", supporting = formatDate(goal.targetDate), icon = Icons.Outlined.Event),
                GroupItem("Prioridade", supporting = "${goal.priority} de 10", icon = Icons.Outlined.LowPriority),
                GroupItem("Criada em", supporting = formatDate(goal.createdAt), icon = Icons.Outlined.History),
            ),
        )
    }
}

/** O valor-alvo em destaque, com o ícone da categoria e o prazo. */
@Composable
private fun TargetHero(goal: Goal, health: GoalHealth?) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ShapeIcon(
                icon = goal.category.icon(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                size = 48.dp,
            )
            Text(
                "Valor alvo",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(formatBrl(goal.targetAmount), style = MaterialTheme.typography.displaySmallEmphasized)
            Text(
                buildString {
                    append("Até ${formatDate(goal.targetDate)}")
                    if (health != null) append(" · ${formatMonthsLeft(health.monthsLeft)}")
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Probabilidade de sucesso da última simulação, em anel, com o selo de saúde. */
@Composable
private fun HealthCard(health: GoalHealth, canSimulate: Boolean) {
    val latest = health.latest
    val accent = goalHealthColor(health.status)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { (latest?.successProbability ?: 0.0).toFloat() },
                    color = accent,
                    trackColor = accent.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(88.dp)
                        .semantics {
                            contentDescription = latest?.let {
                                "Probabilidade de sucesso: ${formatProbability(it.successProbability)}"
                            } ?: "Meta ainda não simulada"
                        },
                )
                Text(
                    latest?.let { "${(it.successProbability * 100).roundToInt()}%" } ?: "—",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalHealthChip(health)
                Text(
                    latest?.let {
                        "Última simulação em ${formatDate(it.createdAt)}: " +
                            "${formatProbability(it.successProbability)} de chance de atingir a meta no prazo."
                    } ?: if (canSimulate) {
                        "Simule para descobrir a probabilidade de atingir esta meta no prazo."
                    } else {
                        "Meta salva neste aparelho — ela será enviada ao servidor assim que houver conexão."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    goalName: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
        title = { Text("Apagar meta?") },
        text = {
            Text(
                (goalName?.let { "\"$it\" e o histórico de simulações dela serão apagados. " } ?: "") +
                    "Esta ação não pode ser desfeita.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Apagar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
