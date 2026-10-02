package com.lifeforge.presentation.screen.usability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.SusScale
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
 * (concordo totalmente); a pontuação de 0 a 100 é calculada ao concluir.
 */
@Composable
fun UsabilityQuestionnaireScreen(
    onNavigateBack: () -> Unit,
    viewModel: UsabilityQuestionnaireViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Questionário SUS") },
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
            val score = state.savedScore
            if (score != null) {
                ScoreCard(score = score, participant = state.participantCode, onDone = onNavigateBack)
                return@Column
            }
            Text(
                "Para cada afirmação, marque de 1 (discordo totalmente) a 5 (concordo totalmente). " +
                    "Não há respostas certas: vale a primeira impressão.",
                style = MaterialTheme.typography.bodyMedium,
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
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            val answered = state.answers.count { it != null }
            Button(onClick = viewModel::submit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth()) {
                Text(if (answered < SusScale.items.size) "Responda todas ($answered/${SusScale.items.size})" else "Concluir")
            }
        }
    }
}

@Composable
private fun LikertItem(number: Int, statement: String, selected: Int?, onSelect: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$number. $statement", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (1..5).forEach { value ->
                    FilterChip(
                        selected = selected == value,
                        onClick = { onSelect(value) },
                        label = { Text(value.toString()) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "$value, ${likertLabels[value - 1]}" },
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    likertLabels.first(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    likertLabels.last(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ScoreCard(score: Double, participant: String?, onDone: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Sessão de ${participant ?: "participante"} registrada",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Pontuação SUS: " + String.format(Locale("pt", "BR"), "%.1f", score),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                if (score >= SusScale.REFERENCE_MEAN) {
                    "Acima da média de referência da escala (68)."
                } else {
                    "Abaixo da média de referência da escala (68)."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Voltar à avaliação") }
        }
    }
}
