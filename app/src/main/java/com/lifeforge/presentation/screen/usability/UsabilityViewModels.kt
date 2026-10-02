package com.lifeforge.presentation.screen.usability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.SusScale
import com.lifeforge.domain.model.TaskOutcome
import com.lifeforge.domain.model.UsabilityCsv
import com.lifeforge.domain.model.UsabilitySession
import com.lifeforge.domain.model.UsabilityStats
import com.lifeforge.domain.model.UsabilitySummary
import com.lifeforge.domain.usability.ActiveUsabilitySession
import com.lifeforge.domain.usability.UsabilitySessionTracker
import com.lifeforge.domain.usecase.DeleteUsabilitySessionUseCase
import com.lifeforge.domain.usecase.ObserveUsabilitySessionsUseCase
import com.lifeforge.domain.usecase.SaveUsabilitySessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// Painel da avaliação: sessão em andamento, resultados e exportação
// ============================================================================

@HiltViewModel
class UsabilityViewModel @Inject constructor(
    private val tracker: UsabilitySessionTracker,
    observeSessions: ObserveUsabilitySessionsUseCase,
    private val deleteSession: DeleteUsabilitySessionUseCase,
) : ViewModel() {

    private val participantInput = MutableStateFlow<String?>(null)

    val state: StateFlow<UsabilityUiState> = combine(
        tracker.active,
        observeSessions(),
        participantInput,
    ) { active, sessions, input ->
        UsabilityUiState(
            active = active,
            sessions = sessions,
            summary = UsabilityStats.summarize(sessions),
            // Sugere o próximo código (P01, P02…) até o avaliador digitar outro.
            participantCode = input ?: suggestCode(sessions),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UsabilityUiState())

    fun onParticipantCodeChange(value: String) {
        participantInput.value = value.take(MAX_CODE_LENGTH)
    }

    fun startSession() {
        val code = state.value.participantCode.trim()
        if (code.isEmpty()) return
        tracker.start(code)
        participantInput.value = null
    }

    fun startTask() = tracker.startCurrentTask()

    fun finishTask(outcome: TaskOutcome) = tracker.finishCurrentTask(outcome)

    fun cancelSession() = tracker.cancel()

    fun delete(id: Long) {
        viewModelScope.launch { deleteSession(id) }
    }

    /** CSV das sessões para análise em planilha (separador ';'). */
    fun exportCsv(): String = UsabilityCsv.export(state.value.sessions)

    private fun suggestCode(sessions: List<UsabilitySession>): String =
        "P%02d".format(sessions.size + 1)

    private companion object {
        const val MAX_CODE_LENGTH = 20
    }
}

data class UsabilityUiState(
    val active: ActiveUsabilitySession? = null,
    val sessions: List<UsabilitySession> = emptyList(),
    val summary: UsabilitySummary = UsabilityStats.summarize(emptyList()),
    val participantCode: String = "P01",
)

// ============================================================================
// Questionário SUS
// ============================================================================

@HiltViewModel
class UsabilityQuestionnaireViewModel @Inject constructor(
    private val tracker: UsabilitySessionTracker,
    private val saveSession: SaveUsabilitySessionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(UsabilityQuestionnaireUiState())
    val state: StateFlow<UsabilityQuestionnaireUiState> = _state.asStateFlow()

    fun answer(index: Int, value: Int) = _state.update { current ->
        current.copy(answers = current.answers.toMutableList().also { it[index] = value })
    }

    fun onCommentChange(value: String) = _state.update { it.copy(comment = value.take(500)) }

    fun submit() {
        val current = _state.value
        val answers = current.answers.filterNotNull()
        if (answers.size != SusScale.items.size || current.isSaving) return
        val active = tracker.active.value
        if (active == null || !active.tasksDone) {
            _state.update { it.copy(errorMessage = "Não há sessão com as tarefas concluídas.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val saved = saveSession(tracker.complete(answers, current.comment))
            _state.update { it.copy(isSaving = false, savedScore = saved.susScore, participantCode = saved.participantCode) }
        }
    }
}

data class UsabilityQuestionnaireUiState(
    val answers: List<Int?> = List(SusScale.items.size) { null },
    val comment: String = "",
    val isSaving: Boolean = false,
    val savedScore: Double? = null,
    val participantCode: String? = null,
    val errorMessage: String? = null,
) {
    val canSubmit: Boolean get() = answers.all { it != null } && !isSaving && savedScore == null
}

// ============================================================================
// Barra global da tarefa em execução
// ============================================================================

@HiltViewModel
class UsabilityBarViewModel @Inject constructor(
    private val tracker: UsabilitySessionTracker,
) : ViewModel() {
    val active: StateFlow<ActiveUsabilitySession?> = tracker.active

    fun finishTask(outcome: TaskOutcome) = tracker.finishCurrentTask(outcome)
}
