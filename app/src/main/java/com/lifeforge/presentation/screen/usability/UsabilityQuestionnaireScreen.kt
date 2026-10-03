package com.lifeforge.presentation.screen.usability

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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.SusScale
import com.lifeforge.presentation.common.ActionButton
import com.lifeforge.presentation.common.BottomActionBar
import com.lifeforge.presentation.common.ConnectedChoice
import com.lifeforge.presentation.common.DetailTopAppBar
import com.lifeforge.presentation.common.FormMaxWidth
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.ShapeIcon
import com.lifeforge.presentation.common.readableWidth
import java.util.Locale

private val likertLabels = listOf(
    "Discordo totalmente",
    "Discordo",
    "Neutro",
    "Concordo",
    "Concordo totalmente",
)

/**
 * Questionário System Usability Scale (BROOKE, 1996), respondido pelo
 * participante ao fim das tarefas. Escala de 1 (discordo totalmente) a 5
 * (concordo totalmente) em botões conectados; a pontuação de 0 a 100 é
 * calculada ao concluir. "Concluir" fica fixo na base, com o progresso.
 */
@Composable
fun UsabilityQuestionnaireScreen(
    onNavigateBack: () -> Unit,
    viewModel: UsabilityQuestionnaireViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val score = state.savedScore
    val answered = state.answers.count { it != null }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopAppBar(
                title = "Questionário SUS",
                subtitle = if (score == null) "$answered de ${SusScale.items.size} respondidas" else null,
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (score == null) {
                BottomActionBar {
                    state.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    ActionButton(
                        text = if (answered < SusScale.items.size) {
                            "Responda todas ($answered/${SusScale.items.size})"
                        } else {
                            "Concluir"
                        },
                        icon = Icons.Rounded.Check,
                        onClick = viewModel::submit,
                        enabled = state.canSubmit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .readableWidth(FormMaxWidth)
                .padding(horizontal = ScreenPadding)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (score != null) {
                ScoreCard(score = score, participant = state.participantCode, onDone = onNavigateBack)
                return@Column
            }
            Text(
                "Para cada afirmação, marque de 1 (discordo totalmente) a 5 (concordo totalmente). " +
                    "Não há respostas certas: vale a primeira impressão.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SusScale.items.forEachIndexed { index, item ->
                LikertItem(
                    number = index + 1,
                    statement = item,
                    selected = state.answers[index],
                    onSelect = { viewModel.answer(index, it) },
                )
            }
            OutlinedTextField(
                value = state.comment,
                onValueChange = viewModel::onCommentChange,
                label = { Text("Comentário (opcional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LikertItem(number: Int, statement: String, selected: Int?, onSelect: (Int) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Número da afirmação num círculo: marcado quando já respondida.
                Surface(
                    shape = CircleShape,
                    color = if (selected != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (selected != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$number", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Text(statement, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
            ConnectedChoice<Int?>(
                options = (1..5).toList(),
                selected = selected,
                onSelect = { value -> value?.let(onSelect) },
                label = { it.toString() },
                description = { value -> "$value, ${likertLabels[(value ?: 1) - 1]}" },
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    likertLabels.first(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(likertLabels.last(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ScoreCard(score: Double, participant: String?, onDone: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShapeIcon(
                icon = Icons.Outlined.CheckCircle,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialShapes.Cookie9Sided.toShape(),
                size = 48.dp,
            )
            Text("Sessão de ${participant ?: "participante"} registrada", style = MaterialTheme.typography.titleMedium)
            Text("Pontuação SUS", style = MaterialTheme.typography.labelLarge)
            Text(
                String.format(Locale("pt", "BR"), "%.1f", score),
                style = MaterialTheme.typography.displayMediumEmphasized,
            )
            Text(
                if (score >= SusScale.REFERENCE_MEAN) {
                    "Acima da média de referência da escala (68)."
                } else {
                    "Abaixo da média de referência da escala (68)."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            ActionButton(text = "Voltar à avaliação", onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}
