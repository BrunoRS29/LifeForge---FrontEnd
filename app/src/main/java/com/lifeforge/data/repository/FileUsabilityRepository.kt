package com.lifeforge.data.repository

import android.content.Context
import com.lifeforge.di.IoDispatcher
import com.lifeforge.domain.model.TaskOutcome
import com.lifeforge.domain.model.TaskResult
import com.lifeforge.domain.model.UsabilitySession
import com.lifeforge.domain.model.UsabilityTask
import com.lifeforge.domain.repository.UsabilityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sessões de avaliação de usabilidade num arquivo JSON do armazenamento
 * interno do app — fora do banco Room, que é apagado no logout: são dados
 * da pesquisa do TCC e não podem se perder ao trocar de conta.
 */
@Singleton
class FileUsabilityRepository @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : UsabilityRepository {

    private val file = File(File(context.filesDir, "usability"), "sessions.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val serializer = ListSerializer(SessionRecord.serializer())
    private val mutex = Mutex()
    private val cache = MutableStateFlow<List<UsabilitySession>?>(null)

    override fun observeSessions(): Flow<List<UsabilitySession>> = flow {
        if (cache.value == null) mutex.withLock { load() }
        emitAll(cache.filterNotNull())
    }

    override suspend fun save(session: UsabilitySession): UsabilitySession = mutex.withLock {
        val current = load()
        val saved = session.copy(id = (current.maxOfOrNull { it.id } ?: 0L) + 1L)
        write(current + saved)
        saved
    }

    override suspend fun delete(id: Long) = mutex.withLock {
        write(load().filterNot { it.id == id })
    }

    private suspend fun load(): List<UsabilitySession> = cache.value ?: withContext(io) {
        val records = if (file.exists()) {
            runCatching { json.decodeFromString(serializer, file.readText()) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        records.mapNotNull { it.toDomain() }.sortedByDescending { it.startedAt }
    }.also { cache.value = it }

    private suspend fun write(sessions: List<UsabilitySession>) {
        val sorted = sessions.sortedByDescending { it.startedAt }
        withContext(io) {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "sessions.json.tmp")
            tmp.writeText(json.encodeToString(serializer, sorted.map { it.toRecord() }))
            // Troca atômica: um arquivo pela metade nunca substitui o anterior.
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
        cache.value = sorted
    }

    @Serializable
    private data class TaskRecord(val task: String, val outcome: String, val durationMs: Long)

    @Serializable
    private data class SessionRecord(
        val id: Long,
        val participantCode: String,
        val startedAt: String,
        val tasks: List<TaskRecord>,
        val susAnswers: List<Int>,
        val susScore: Double,
        val comment: String? = null,
    )

    private fun UsabilitySession.toRecord() = SessionRecord(
        id = id,
        participantCode = participantCode,
        startedAt = startedAt.toString(),
        tasks = tasks.map { TaskRecord(it.task.name, it.outcome.name, it.durationMs) },
        susAnswers = susAnswers,
        susScore = susScore,
        comment = comment,
    )

    private fun SessionRecord.toDomain(): UsabilitySession? = runCatching {
        UsabilitySession(
            id = id,
            participantCode = participantCode,
            startedAt = Instant.parse(startedAt),
            tasks = tasks.map { TaskResult(UsabilityTask.valueOf(it.task), TaskOutcome.valueOf(it.outcome), it.durationMs) },
            susAnswers = susAnswers,
            susScore = susScore,
            comment = comment,
        )
    }.getOrNull()
}
