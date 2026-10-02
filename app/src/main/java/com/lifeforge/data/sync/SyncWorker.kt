package com.lifeforge.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Executa um ciclo de sincronização ([SyncEngine.syncAll]) em segundo plano.
 *
 * - sucesso → concluído;
 * - sessão expirada (401) → encerra sem repetir: a fila é preservada e sobe
 *   depois que o usuário entrar de novo;
 * - falha transitória (rede, servidor) → o WorkManager repete com recuo
 *   exponencial.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val engine: SyncEngine,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (val result = engine.syncAll()) {
        is DataResult.Success -> Result.success()
        is DataResult.Failure -> {
            Timber.w("Sincronização falhou (tentativa %d): %s", runAttemptCount + 1, result.error)
            when {
                result.error is AppError.Unauthorized -> Result.failure()
                runAttemptCount + 1 >= MAX_ATTEMPTS -> Result.failure()
                else -> Result.retry()
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
    }
}
