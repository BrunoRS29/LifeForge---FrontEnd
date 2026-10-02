package com.lifeforge.presentation.screen.usability

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.SusScale
import com.lifeforge.domain.model.TaskOutcome
import com.lifeforge.domain.model.UsabilitySession
import com.lifeforge.domain.model.UsabilitySummary
import com.lifeforge.domain.model.UsabilityTask
import com.lifeforge.domain.usability.ActiveUsabilitySession
import com.lifeforge.presentation.common.formatDateTime
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.util.Locale

/**
 * Condução da avaliação de usabilidade (TCC, objetivo (g) e Seção 3.6.3):
 * o avaliador inicia a sessão de um participante, cronometra cada tarefa do
 * roteiro (o participante usa o app livremente; a barra no rodapé segue em
 * qualquer tela) e, ao fim, aplica o questionário SUS. Os resultados ficam no
 * aparelho, com resumo estatístico e exportação em CSV.
 */
@Composable
fun UsabilityScreen(
    onNavigateBack: () -> Unit,
    onOpenQuestionnaire: () -> Unit,
    viewModel: UsabilityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<UsabilitySession?>(null) }

    // Salvar o CSV onde o avaliador escolher (Downloads, Drive…), sem FileProvider.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    // BOM para o Excel reconhecer UTF-8 (acentos dos comentários).
                    out.write("﻿".toByteArray(Charsets.UTF_8))
                    out.write(viewModel.exportCsv().toByteArray(Charsets.UTF_8))
                }
            }.isSuccess
            Toast.makeText(context, if (ok) "CSV exportado." else "Não foi possível salvar o arquivo.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avaliação de usabilidade") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProtocolCard()
            val active = state.active
            when {
                active == null -> StartSessionCard(
                    participantCode = state.participantCode,
                    onCodeChange = viewModel::onParticipantCodeChange,
                    onStart = viewModel::startSession,
                )
                active.tasksDone -> TasksDoneCard(active, onOpenQuestionnaire, viewModel::cancelSession)
                else -> CurrentTaskCard(
                    active = active,
                    onStartTask = viewModel::startTask,
                    onFinish = viewModel::finishTask,
                    onCancel = viewModel::cancelSession,
                )
            }
            ResultsCard(
                summary = state.summary,
                onExport = { exportLauncher.launch("lifeforge-avaliacao-sus.csv") },
            )
            if (state.sessions.isNotEmpty()) {
                SessionsCard(state.sessions, onDelete = { pendingDelete = it })
            }
        }
    }

    pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir a sessão de ${session.participantCode}?") },
            text = { Text("As respostas e os tempos desta sessão serão apagados deste aparelho.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(session.id)
                    pendingDelete = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ProtocolCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Como funciona", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(
                "Cada participante executa as tarefas abaixo, com o tempo e a conclusão registrados, e " +
                    "depois responde às ${SusScale.items.size} afirmações do System Usability Scale (SUS).",
                style = MaterialTheme.typography.bodySmall,
            )
            UsabilityTask.entries.forEachIndexed { index, task ->
                Text(
                    "${index + 1}. ${task.title} — ${task.instruction}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StartSessionCard(participantCode: String, onCodeChange: (String) -> Unit, onStart: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nova sessão", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            OutlinedTextField(
                value = participantCode,
                onValueChange = onCodeChange,
                label = { Text("Código do participante") },
                supportingText = { Text("Use um código, não o nome: os dados ficam anônimos.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onStart, enabled = participantCode.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Iniciar sessão")
            }
        }
    }
}

@Composable
private fun CurrentTaskCard(
    active: ActiveUsabilitySession,
    onStartTask: () -> Unit,
    onFinish: (TaskOutcome) -> Unit,
    onCancel: () -> Unit,
) {
    val task = active.currentTask ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Participante ${active.participantCode} · tarefa ${active.currentTaskIndex + 1} de ${active.totalTasks}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(task.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(task.instruction, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            if (active.isTaskRunning) {
                ElapsedTimer(active.taskStartedAt, style = MaterialTheme.typography.displaySmall)
                Text(
                    "O participante pode navegar à vontade: a barra no rodapé mostra o tempo e encerra a tarefa de qualquer tela.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onFinish(TaskOutcome.COMPLETED) }, modifier = Modifier.weight(1f)) { Text("Concluída") }
                    OutlinedButton(
                        onClick = { onFinish(TaskOutcome.NOT_COMPLETED) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Não concluída") }
                }
            } else {
                Button(onClick = onStartTask, modifier = Modifier.fillMaxWidth()) { Text("Iniciar tarefa e cronômetro") }
            }
            TextButton(onClick = onCancel) { Text("Cancelar sessão") }
        }
    }
}

@Composable
private fun TasksDoneCard(active: ActiveUsabilitySession, onOpenQuestionnaire: () -> Unit, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Tarefas encerradas — ${active.participantCode}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            active.results.forEachIndexed { index, result ->
                Text(
                    "${index + 1}. ${result.task.title}: " +
                        (if (result.outcome == TaskOutcome.COMPLETED) "concluída" else "não concluída") +
                        " em ${formatDuration(result.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = onOpenQuestionnaire, modifier = Modifier.fillMaxWidth()) { Text("Responder o questionário SUS") }
            TextButton(onClick = onCancel) { Text("Descartar sessão") }
        }
    }
}

@Composable
private fun ResultsCard(summary: UsabilitySummary, onExport: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Resultados (${summary.participants} participante${if (summary.participants == 1) "" else "s"})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            val mean = summary.meanSus
            if (mean == null) {
                Text("Nenhuma sessão concluída ainda.", style = MaterialTheme.typography.bodySmall)
                return@Column
            }
            Text(
                "SUS médio: ${decimal(mean)}" + (summary.sdSus?.let { " (desvio-padrão ${decimal(it)})" } ?: ""),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Referência da literatura: ${decimal(SusScale.REFERENCE_MEAN)} — " +
                    if (mean >= SusScale.REFERENCE_MEAN) "resultado acima da média." else "resultado abaixo da média.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            summary.tasks.forEach { task ->
                Text(
                    "${task.task.title}: ${percent(task.completionRate)} concluíram" +
                        (task.meanDurationMs?.let { " · tempo médio ${formatDuration(it.toLong())}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null)
                Text("  Exportar CSV")
            }
        }
    }
}

@Composable
private fun SessionsCard(sessions: List<UsabilitySession>, onDelete: (UsabilitySession) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sessões", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            sessions.forEach { session ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${session.participantCode} · SUS ${decimal(session.susScore)}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatDateTime(session.startedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDelete(session) }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Excluir sessão de ${session.participantCode}",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

/** Cronômetro que se atualiza a cada segundo a partir do início da tarefa. */
@Composable
fun ElapsedTimer(startedAt: Instant?, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(startedAt) {
        while (startedAt != null) {
            elapsed = Duration.between(startedAt, Instant.now()).toMillis().coerceAtLeast(0)
            delay(1_000)
        }
    }
    Text(formatDuration(elapsed), style = style, modifier = modifier)
}

/** 83_000 ms → "1:23". */
fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun decimal(value: Double): String = String.format(Locale("pt", "BR"), "%.1f", value)

private fun percent(fraction: Double): String = String.format(Locale("pt", "BR"), "%.0f%%", fraction * 100)
