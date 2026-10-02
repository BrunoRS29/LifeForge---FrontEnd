package com.lifeforge.domain.model

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.usability.UsabilitySessionTracker
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Instrumento da avaliação de usabilidade (TCC, Seção 3.6.3): pontuação SUS,
 * cronometragem das tarefas, indicadores agregados e exportação CSV.
 */
class UsabilityTest {

    /** Relógio controlado pelo teste. */
    private class TestClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
    }

    // ------------------------------------------------------------------------
    // Pontuação SUS (BROOKE, 1996)
    // ------------------------------------------------------------------------

    @Test
    fun `respostas neutras valem 50 e os extremos 0 e 100`() {
        assertThat(SusScale.score(List(10) { 3 })).isEqualTo(50.0)
        assertThat(SusScale.score(List(10) { if (it % 2 == 0) 5 else 1 })).isEqualTo(100.0)
        assertThat(SusScale.score(List(10) { if (it % 2 == 0) 1 else 5 })).isEqualTo(0.0)
    }

    @Test
    fun `itens impares somam resposta menos 1 e pares 5 menos resposta`() {
        // Ímpares: 4,5,4,4,5 → 3+4+3+3+4 = 17; pares: 2,1,2,2,1 → 3+4+3+3+4 = 17; (17+17)×2,5 = 85.
        val answers = listOf(4, 2, 5, 1, 4, 2, 4, 2, 5, 1)

        assertThat(SusScale.score(answers)).isEqualTo(85.0)
    }

    @Test
    fun `respostas incompletas ou fora da escala sao rejeitadas`() {
        assertThrows(IllegalArgumentException::class.java) { SusScale.score(List(9) { 3 }) }
        assertThrows(IllegalArgumentException::class.java) { SusScale.score(List(10) { 6 }) }
    }

    // ------------------------------------------------------------------------
    // Sessão cronometrada
    // ------------------------------------------------------------------------

    @Test
    fun `tracker cronometra cada tarefa e fecha a sessao com o SUS`() {
        val clock = TestClock(Instant.parse("2026-10-02T10:00:00Z"))
        val tracker = UsabilitySessionTracker(clock)

        tracker.start("P01")
        UsabilityTask.entries.forEachIndexed { index, _ ->
            tracker.startCurrentTask()
            clock.now = clock.now.plusSeconds(30L + index * 10L)
            tracker.finishCurrentTask(if (index == 2) TaskOutcome.NOT_COMPLETED else TaskOutcome.COMPLETED)
        }
        assertThat(tracker.active.value!!.tasksDone).isTrue()

        val session = tracker.complete(List(10) { 3 }, comment = "  Gostei do gráfico de leque  ")

        assertThat(session.participantCode).isEqualTo("P01")
        assertThat(session.tasks.map { it.durationMs }).containsExactly(30_000L, 40_000L, 50_000L, 60_000L).inOrder()
        assertThat(session.tasks[2].outcome).isEqualTo(TaskOutcome.NOT_COMPLETED)
        assertThat(session.susScore).isEqualTo(50.0)
        assertThat(session.comment).isEqualTo("Gostei do gráfico de leque")
        assertThat(tracker.active.value).isNull()
    }

    @Test
    fun `tarefa so termina se tiver sido iniciada e o SUS so fecha com as tarefas encerradas`() {
        val tracker = UsabilitySessionTracker(TestClock(Instant.EPOCH))
        tracker.start("P02")

        tracker.finishCurrentTask(TaskOutcome.COMPLETED) // ignorada: cronômetro parado

        assertThat(tracker.active.value!!.currentTaskIndex).isEqualTo(0)
        assertThrows(IllegalStateException::class.java) { tracker.complete(List(10) { 3 }, null) }
    }

    // ------------------------------------------------------------------------
    // Indicadores e exportação
    // ------------------------------------------------------------------------

    private fun session(id: Long, score: Double, firstTask: TaskOutcome, firstMs: Long) = UsabilitySession(
        id = id,
        participantCode = "P0$id",
        startedAt = Instant.parse("2026-10-0${id}T10:00:00Z"),
        tasks = listOf(
            TaskResult(UsabilityTask.CREATE_GOAL, firstTask, firstMs),
            TaskResult(UsabilityTask.RUN_SIMULATION, TaskOutcome.COMPLETED, 90_000),
        ),
        susAnswers = List(10) { 3 },
        susScore = score,
        comment = if (id == 1L) "Achei; ótimo" else null,
    )

    @Test
    fun `resumo traz SUS medio, desvio e taxa de conclusao por tarefa`() {
        val summary = UsabilityStats.summarize(
            listOf(
                session(1, 70.0, TaskOutcome.COMPLETED, 60_000),
                session(2, 80.0, TaskOutcome.NOT_COMPLETED, 200_000),
                session(3, 90.0, TaskOutcome.COMPLETED, 120_000),
            )
        )

        assertThat(summary.participants).isEqualTo(3)
        assertThat(summary.meanSus).isWithin(1e-9).of(80.0)
        assertThat(summary.sdSus).isWithin(1e-9).of(10.0)
        val createGoal = summary.tasks.first { it.task == UsabilityTask.CREATE_GOAL }
        assertThat(createGoal.completionRate).isWithin(1e-9).of(2.0 / 3.0)
        // Tempo médio considera só as execuções concluídas.
        assertThat(createGoal.meanDurationMs).isWithin(1e-9).of(90_000.0)
        val interpret = summary.tasks.first { it.task == UsabilityTask.INTERPRET_RESULTS }
        assertThat(interpret.attempts).isEqualTo(0)
        assertThat(interpret.meanDurationMs).isNull()
    }

    @Test
    fun `resumo sem sessoes nao tem media`() {
        val summary = UsabilityStats.summarize(emptyList())

        assertThat(summary.meanSus).isNull()
        assertThat(summary.sdSus).isNull()
    }

    @Test
    fun `CSV tem uma linha por participante, decimais com virgula e campos escapados`() {
        val csv = UsabilityCsv.export(listOf(session(1, 72.5, TaskOutcome.COMPLETED, 61_500)))
        val lines = csv.lines()

        assertThat(lines).hasSize(2)
        assertThat(lines[0]).startsWith("participante;inicio;tarefa1_concluida;tarefa1_tempo_s")
        assertThat(lines[0]).endsWith("sus_pontuacao;comentario")
        assertThat(lines[1]).startsWith("P01;2026-10-01T10:00:00Z;sim;61,5;")
        assertThat(lines[1]).contains(";72,5;")
        assertThat(lines[1]).endsWith("\"Achei; ótimo\"")
    }
}
