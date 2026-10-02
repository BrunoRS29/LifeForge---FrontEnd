package com.lifeforge.presentation.screen.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalHealth
import com.lifeforge.domain.model.isLocalOnly
import com.lifeforge.presentation.common.GoalHealthChip
import com.lifeforge.presentation.common.formatMonthsLeft
import com.lifeforge.presentation.common.formatProbability
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.LoadingIndicator
import com.lifeforge.presentation.common.LoadingOverlay
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatDate
import com.lifeforge.presentation.common.label

/**
 * Tela de detalhe da meta. Mostra todos os campos em cards, e oferece
 * 4 ações: Simular clássico, Simular com IA, Editar e Apagar.
 *
 * Delete usa dialog de confirmação — operação destrutiva merece o
 * extra tap. Após sucesso, navega de volta automaticamente via
 * `eventsFlow` do ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(Unit) {
        viewModel.eventsFlow.collect { event ->
            when (event) {
                GoalDetailEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.goal?.name ?: "Meta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val goal = state.goal
            when {
                goal != null -> GoalDetailContent(
                    goal = goal,
                    health = state.health,
                    errorBanner = state.errorBanner,
                    onErrorDismiss = viewModel::onErrorBannerDismiss,
                    onEdit = { onEdit(goal.id) },
                    onSimulate = { onSimulate(goal.id) },
                    onSimulateWithAi = { onSimulateWithAi(goal.id) },
                    onDeleteClick = { showDeleteDialog = true },
                )
                else -> LoadingIndicator()
            }

            LoadingOverlay(visible = state.isDeleting)
        }

        if (showDeleteDialog) {
            DeleteConfirmDialog(
                onConfirm = {
                    showDeleteDialog = false
                    viewModel.delete()
                },
                onDismiss = { showDeleteDialog = false },
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
    onEdit: () -> Unit,
    onSimulate: () -> Unit,
    onSimulateWithAi: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    // Meta criada sem conexão: o motor roda no servidor, que ainda não a conhece.
    val canSimulate = !goal.isLocalOnly()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (errorBanner != null) {
            ErrorBanner(message = errorBanner, onDismiss = onErrorDismiss)
        }

        // Card destaque com o valor alvo.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    "Valor alvo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    formatBrl(goal.targetAmount),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // Saúde da meta: leitura da última simulação (ou o convite para simular).
        if (health != null) {
            GoalHealthChip(health)
            Text(
                health.latest?.let { latest ->
                    "Última simulação em ${formatDate(latest.createdAt)}: " +
                        "${formatProbability(latest.successProbability)} de chance de atingir a meta " +
                        "(${formatMonthsLeft(health.monthsLeft)})."
                } ?: if (canSimulate) {
                    "Simule para descobrir a probabilidade de atingir esta meta no prazo."
                } else {
                    "Meta salva neste aparelho — ela será enviada ao servidor assim que houver " +
                        "conexão, e a simulação fica disponível em seguida."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        InfoRow(label = "Categoria", value = goal.category.label())
        InfoRow(label = "Data alvo", value = formatDate(goal.targetDate))
        InfoRow(label = "Prioridade", value = "${goal.priority}/10")
        InfoRow(label = "Criada em", value = formatDate(goal.createdAt))

        Spacer(Modifier.height(8.dp))

        // Simulação clássica (parâmetros manuais) e calibrada pela IA (1 toque).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSimulate,
                enabled = canSimulate,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.AutoGraph, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Simular")
            }

            Button(
                onClick = onSimulateWithAi,
                enabled = canSimulate,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Simular com IA")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Editar")
            }
            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Apagar")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apagar meta?") },
        text = { Text("Esta ação não pode ser desfeita.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Apagar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}