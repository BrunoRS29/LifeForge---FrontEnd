package com.lifeforge.domain.model

import java.time.Instant
import kotlin.math.sqrt

/**
 * Avaliação de usabilidade com usuários reais (TCC, objetivo específico (g) e
 * Seção 3.6.3): cada participante executa tarefas representativas — o tempo e
 * a conclusão de cada uma são registrados — e responde ao questionário
 * System Usability Scale (SUS).
 */
enum class UsabilityTask(val title: String, val instruction: String) {
    CREATE_GOAL(
        "Cadastrar uma meta",
        "Crie uma meta de vida informando valor e prazo.",
    ),
    REGISTER_FINANCES(
        "Registrar variáveis financeiras",
        "Lance uma receita, uma despesa e um ativo em Finanças.",
    ),
    RUN_SIMULATION(
        "Executar uma simulação",
        "Simule a meta criada, com os parâmetros que quiser ou com IA.",
    ),
    INTERPRET_RESULTS(
        "Interpretar os resultados",
        "Diga ao avaliador a chance de atingir a meta e o valor do cenário pessimista.",
    ),
}

enum class TaskOutcome { COMPLETED, NOT_COMPLETED }

data class TaskResult(
    val task: UsabilityTask,
    val outcome: TaskOutcome,
    val durationMs: Long,
)

/** Sessão concluída de um participante. */
data class UsabilitySession(
    val id: Long,
    val participantCode: String,
    val startedAt: Instant,
    val tasks: List<TaskResult>,
    /** Respostas de 1 (discordo totalmente) a 5 (concordo totalmente), na ordem dos itens. */
    val susAnswers: List<Int>,
    val susScore: Double,
    val comment: String? = null,
)

/**
 * System Usability Scale (BROOKE, 1996): dez afirmações alternando tom
 * positivo (ímpares) e negativo (pares), respondidas em escala de 1 a 5.
 */
object SusScale {

    /** Itens na redação usual em português, referindo-se ao aplicativo avaliado. */
    val items: List<String> = listOf(
        "Eu acho que gostaria de usar este aplicativo com frequência.",
        "Eu achei o aplicativo desnecessariamente complexo.",
        "Eu achei o aplicativo fácil de usar.",
        "Eu acho que precisaria da ajuda de uma pessoa com conhecimentos técnicos para usar o aplicativo.",
        "Eu achei que as várias funções do aplicativo estão bem integradas.",
        "Eu achei que o aplicativo apresenta muita inconsistência.",
        "Eu imagino que a maioria das pessoas aprenderia a usar este aplicativo rapidamente.",
        "Eu achei o aplicativo muito complicado de usar.",
        "Eu me senti muito confiante ao usar o aplicativo.",
        "Eu precisei aprender várias coisas antes de conseguir usar o aplicativo.",
    )

    /** Média de referência da escala em estudos publicados (SAURO, 2011). */
    const val REFERENCE_MEAN = 68.0

    /**
     * Pontuação de 0 a 100: itens ímpares contribuem com (resposta − 1), pares
     * com (5 − resposta); a soma (0–40) é multiplicada por 2,5.
     */
    fun score(answers: List<Int>): Double {
        require(answers.size == items.size) { "O SUS tem ${items.size} itens; recebidas ${answers.size} respostas" }
        require(answers.all { it in 1..5 }) { "Respostas devem estar entre 1 e 5" }
        val sum = answers.withIndex().sumOf { (index, answer) ->
            if (index % 2 == 0) answer - 1 else 5 - answer
        }
        return sum * 2.5
    }
}

/** Indicadores de uma tarefa no conjunto de participantes. */
data class TaskSummary(
    val task: UsabilityTask,
    val attempts: Int,
    /** Fração de participantes que concluíram a tarefa. */
    val completionRate: Double,
    /** Tempo médio das execuções concluídas (null se nenhuma foi concluída). */
    val meanDurationMs: Double?,
)

data class UsabilitySummary(
    val participants: Int,
    val meanSus: Double?,
    /** Desvio-padrão amostral do SUS (null com menos de dois participantes). */
    val sdSus: Double?,
    val tasks: List<TaskSummary>,
)

object UsabilityStats {

    fun summarize(sessions: List<UsabilitySession>): UsabilitySummary {
        val scores = sessions.map { it.susScore }
        val mean = scores.takeIf { it.isNotEmpty() }?.average()
        val sd = if (scores.size >= 2 && mean != null) {
            sqrt(scores.sumOf { (it - mean) * (it - mean) } / (scores.size - 1))
        } else {
            null
        }
        val tasks = UsabilityTask.entries.map { task ->
            val results = sessions.mapNotNull { session -> session.tasks.firstOrNull { it.task == task } }
            val completed = results.filter { it.outcome == TaskOutcome.COMPLETED }
            TaskSummary(
                task = task,
                attempts = results.size,
                completionRate = if (results.isEmpty()) 0.0 else completed.size.toDouble() / results.size,
                meanDurationMs = completed.takeIf { it.isNotEmpty() }?.map { it.durationMs.toDouble() }?.average(),
            )
        }
        return UsabilitySummary(participants = sessions.size, meanSus = mean, sdSus = sd, tasks = tasks)
    }
}

/** Exportação das sessões em CSV (separador ';', decimal com vírgula) para planilhas. */
object UsabilityCsv {

    fun export(sessions: List<UsabilitySession>): String {
        val header = buildList {
            add("participante")
            add("inicio")
            UsabilityTask.entries.forEachIndexed { i, _ ->
                add("tarefa${i + 1}_concluida")
                add("tarefa${i + 1}_tempo_s")
            }
            SusScale.items.indices.forEach { add("sus_q${it + 1}") }
            add("sus_pontuacao")
            add("comentario")
        }
        val rows = sessions.sortedBy { it.startedAt }.map { session ->
            buildList {
                add(session.participantCode)
                add(session.startedAt.toString())
                UsabilityTask.entries.forEach { task ->
                    val result = session.tasks.firstOrNull { it.task == task }
                    add(
                        when (result?.outcome) {
                            TaskOutcome.COMPLETED -> "sim"
                            TaskOutcome.NOT_COMPLETED -> "nao"
                            null -> ""
                        }
                    )
                    add(result?.let { decimal(it.durationMs / 1000.0) }.orEmpty())
                }
                session.susAnswers.forEach { add(it.toString()) }
                add(decimal(session.susScore))
                add(session.comment.orEmpty())
            }
        }
        return (listOf(header) + rows).joinToString("\n") { row -> row.joinToString(";") { escape(it) } }
    }

    private fun decimal(value: Double): String = String.format(java.util.Locale.US, "%.1f", value).replace('.', ',')

    private fun escape(field: String): String =
        if (field.any { it == ';' || it == '"' || it == '\n' }) "\"" + field.replace("\"", "\"\"") + "\"" else field
}
