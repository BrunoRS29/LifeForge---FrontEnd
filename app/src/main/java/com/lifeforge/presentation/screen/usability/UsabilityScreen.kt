package com.lifeforge.presentation.screen.usability

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.lifeforge.presentation.common.ActionButton
import com.lifeforge.presentation.common.ActionEmphasis
import com.lifeforge.presentation.common.ContentCard
import com.lifeforge.presentation.common.DetailTopAppBar
import com.lifeforge.presentation.common.FormSection
import com.lifeforge.presentation.common.GroupItem
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.ListGroup
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.formatDateTime
import com.lifeforge.presentation.common.readableWidth
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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopAppBar(
                title = "Avaliação de usabilidade",
                subtitle = "Tarefas cronometradas e questionário SUS",
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .readableWidth()
                .padding(horizontal = ScreenPadding)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val active = state.active
            when {
                active == null -> StartSessionSection(
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
            ProtocolCard()
            ResultsCard(
                summary = state.summary,
                onExport = { exportLauncher.launch("lifeforge-avaliacao-sus.csv") },
            )
            if (state.sessions.isNotEmpty()) {
                ListGroup(
                    title = "Sessões",
                    items = state.sessions.map { session ->
                        GroupItem(
                            headline = "${session.participantCode} · SUS ${decimal(session.susScore)}",
                            supporting = formatDateTime(session.startedAt),
                            icon = Icons.Outlined.Person,
                            trailing = {
                                IconButton(onClick = { pendingDelete = session }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Excluir sessão de ${session.participantCode}",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            },
                        )
                    },
                )
            }
        }
    }

    pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Excluir a sessão de ${session.participantCode}?") },
            text = { Text("As respostas e os tempos desta sessão serão apagados deste aparelho.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(session.id)
                    pendingDelete = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } },
        )
    }
}

/** Roteiro da avaliação: as tarefas, na ordem em que o participante as executa. */
@Composable
private fun ProtocolCard() {
    ContentCard(
        title = "Como funciona",
        supporting = "Cada participante executa as tarefas abaixo, com o tempo e a conclusão registrados, e depois " +
            "responde às ${SusScale.items.size} afirmações do System Usability Scale (SUS).",
    ) {
        UsabilityTask.entries.forEachIndexed { index, task ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        task.instruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StartSessionSection(participantCode: String, onCodeChange: (String) -> Unit, onStart: () -> Unit) {
    FormSection(title = "Nova sessão", supporting = "Use um código, não o nome: os dados ficam anônimos.") {
        LifeForgeTextField(
            value = participantCode,
            onValueChange = onCodeChange,
            label = "Código do participante",
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done,
        )
        ActionButton(
            text = "Iniciar sessão",
            icon = Icons.Outlined.PlayArrow,
            onClick = onStart,
            enabled = participantCode.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
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
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Participante ${active.participantCode} · tarefa ${active.currentTaskIndex + 1} de ${active.totalTasks}",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(task.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(task.instruction, style = MaterialTheme.typography.bodyMedium)
            if (active.isTaskRunning) {
                ElapsedTimer(active.taskStartedAt, style = MaterialTheme.typography.displayMediumEmphasized)
                Text(
                    "O participante pode navegar à vontade: a barra no rodapé mostra o tempo e encerra a tarefa de qualquer tela.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = "Concluída",
                        icon = Icons.Rounded.Check,
                        onClick = { onFinish(TaskOutcome.COMPLETED) },
                        modifier = Modifier.weight(1f),
                    )
                    ActionButton(
                        text = "Não concluída",
                        onClick = { onFinish(TaskOutcome.NOT_COMPLETED) },
                        emphasis = ActionEmphasis.Outlined,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                ActionButton(
                    text = "Iniciar tarefa e cronômetro",
                    icon = Icons.Outlined.Timer,
                    onClick = onStartTask,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TextButton(onClick = onCancel) { Text("Cancelar sessão") }
        }
    }
}

@Composable
private fun TasksDoneCard(active: ActiveUsabilitySession, onOpenQuestionnaire: () -> Unit, onCancel: () -> Unit) {
    ContentCard(title = "Tarefas encerradas — ${active.participantCode}") {
        active.results.forEachIndexed { index, result ->
            Text(
                "${index + 1}. ${result.task.title}: " +
                    (if (result.outcome == TaskOutcome.COMPLETED) "concluída" else "não concluída") +
                    " em ${formatDuration(result.durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        ActionButton(
            text = "Responder o questionário SUS",
            icon = Icons.AutoMirrored.Outlined.FactCheck,
            onClick = onOpenQuestionnaire,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onCancel) { Text("Descartar sessão") }
    }
}

@Composable
private fun ResultsCard(summary: UsabilitySummary, onExport: () -> Unit) {
    ContentCard(
        title = "Resultados",
        supporting = "${summary.participants} participante${if (summary.participants == 1) "" else "s"}",
    ) {
        val mean = summary.meanSus
        if (mean == null) {
            Text("Nenhuma sessão concluída ainda.", style = MaterialTheme.typography.bodyMedium)
            return@ContentCard
        }
        Column {
            Text("SUS médio", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(decimal(mean), style = MaterialTheme.typography.displaySmallEmphasized, color = MaterialTheme.colorScheme.primary)
            summary.sdSus?.let {
                Text("desvio-padrão ${decimal(it)}", style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(
            "Referência da literatura: ${decimal(SusScale.REFERENCE_MEAN)} — " +
                if (mean >= SusScale.REFERENCE_MEAN) "resultado acima da média." else "resultado abaixo da média.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        summary.tasks.forEach { task ->
            Text(
                "${task.task.title}: ${percent(task.completionRate)} concluíram" +
                    (task.meanDurationMs?.let { " · tempo médio ${formatDuration(it.toLong())}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        ActionButton(
            text = "Exportar CSV",
            icon = Icons.Outlined.FileDownload,
            onClick = onExport,
            emphasis = ActionEmphasis.Outlined,
            modifier = Modifier.fillMaxWidth(),
        )
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
