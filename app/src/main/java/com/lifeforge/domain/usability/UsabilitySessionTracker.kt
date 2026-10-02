package com.lifeforge.domain.usability

import com.lifeforge.domain.model.SusScale
import com.lifeforge.domain.model.TaskOutcome
import com.lifeforge.domain.model.TaskResult
import com.lifeforge.domain.model.UsabilitySession
import com.lifeforge.domain.model.UsabilityTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Sessão de avaliação em andamento (estado vivo enquanto o participante usa o app). */
data class ActiveUsabilitySession(
    val participantCode: String,
    val startedAt: Instant,
    /** Índice da tarefa atual em [UsabilityTask.entries]; igual ao total = tarefas encerradas. */
    val currentTaskIndex: Int = 0,
    /** Instante em que a tarefa atual começou; null = ainda não iniciada. */
    val taskStartedAt: Instant? = null,
    val results: List<TaskResult> = emptyList(),
) {
    val totalTasks: Int get() = UsabilityTask.entries.size
    val currentTask: UsabilityTask? get() = UsabilityTask.entries.getOrNull(currentTaskIndex)
    val isTaskRunning: Boolean get() = taskStartedAt != null
    /** Todas as tarefas encerradas: falta só o questionário SUS. */
    val tasksDone: Boolean get() = currentTaskIndex >= totalTasks
}

/**
 * Conduz uma sessão de teste de usabilidade (TCC, Seção 3.6.3): cronometra
 * cada tarefa do roteiro e, ao fim, fecha a sessão com as respostas do SUS.
 *
 * Fica em memória e em escopo de aplicação: o participante navega livremente
 * pelo app durante a tarefa, e o cronômetro segue valendo em qualquer tela.
 */
@Singleton
class UsabilitySessionTracker @Inject constructor(
    private val clock: Clock,
) {
    private val _active = MutableStateFlow<ActiveUsabilitySession?>(null)
    val active: StateFlow<ActiveUsabilitySession?> = _active.asStateFlow()

    fun start(participantCode: String) {
        val code = participantCode.trim()
        require(code.isNotEmpty()) { "Informe o código do participante" }
        _active.value = ActiveUsabilitySession(participantCode = code, startedAt = Instant.now(clock))
    }

    /** Dispara o cronômetro da tarefa atual. */
    fun startCurrentTask() {
        val session = _active.value ?: return
        if (session.tasksDone || session.isTaskRunning) return
        _active.value = session.copy(taskStartedAt = Instant.now(clock))
    }

    /** Encerra a tarefa atual com o desfecho observado e avança para a próxima. */
    fun finishCurrentTask(outcome: TaskOutcome) {
        val session = _active.value ?: return
        val task = session.currentTask ?: return
        val startedAt = session.taskStartedAt ?: return
        val duration = Duration.between(startedAt, Instant.now(clock)).toMillis().coerceAtLeast(0)
        _active.value = session.copy(
            currentTaskIndex = session.currentTaskIndex + 1,
            taskStartedAt = null,
            results = session.results + TaskResult(task, outcome, duration),
        )
    }

    /** Tempo decorrido da tarefa em execução (null se nenhuma está rodando). */
    fun elapsedMs(): Long? {
        val startedAt = _active.value?.taskStartedAt ?: return null
        return Duration.between(startedAt, Instant.now(clock)).toMillis().coerceAtLeast(0)
    }

    /**
     * Fecha a sessão com as respostas do questionário. Devolve a sessão pronta
     * para ser gravada; o rastreador volta a ficar livre.
     */
    fun complete(susAnswers: List<Int>, comment: String?): UsabilitySession {
        val session = checkNotNull(_active.value) { "Nenhuma sessão em andamento" }
        check(session.tasksDone) { "Ainda há tarefas em aberto" }
        val result = UsabilitySession(
            id = 0,
            participantCode = session.participantCode,
            startedAt = session.startedAt,
            tasks = session.results,
            susAnswers = susAnswers,
            susScore = SusScale.score(susAnswers),
            comment = comment?.trim()?.takeIf { it.isNotEmpty() },
        )
        _active.value = null
        return result
    }

    fun cancel() {
        _active.value = null
    }
}
