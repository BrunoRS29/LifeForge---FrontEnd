package com.lifeforge.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeforge.domain.model.TaskOutcome
import com.lifeforge.domain.usability.ActiveUsabilitySession
import com.lifeforge.presentation.screen.usability.ElapsedTimer

/**
 * Barra da avaliação de usabilidade, visível em qualquer tela enquanto uma
 * tarefa está sendo cronometrada: o participante usa o app livremente e o
 * avaliador encerra a tarefa daqui. Com as tarefas encerradas, leva ao
 * questionário SUS.
 */
@Composable
fun UsabilityTaskBar(
    active: ActiveUsabilitySession?,
    onFinish: (TaskOutcome) -> Unit,
    onOpenQuestionnaire: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = active != null && (active.isTaskRunning || active.tasksDone)
    AnimatedVisibility(visible = visible, enter = expandVertically(), exit = shrinkVertically(), modifier = modifier) {
        val session = active ?: return@AnimatedVisibility
        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                if (session.tasksDone) {
                    Text(
                        "Tarefas encerradas — falta o questionário",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalButton(onClick = onOpenQuestionnaire) { Text("Abrir") }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Tarefa ${session.currentTaskIndex + 1}/${session.totalTasks}: ${session.currentTask?.title.orEmpty()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        ElapsedTimer(
                            startedAt = session.taskStartedAt,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    TextButton(onClick = { onFinish(TaskOutcome.NOT_COMPLETED) }) { Text("Não concluiu") }
                    FilledTonalButton(onClick = { onFinish(TaskOutcome.COMPLETED) }) { Text("Concluiu") }
                }
            }
        }
    }
}
