package com.lifeforge.data.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Agenda a sincronização em segundo plano. */
interface SyncScheduler {
    /** Sincroniza assim que houver rede (sobrevive ao fechamento do app). */
    fun requestSync()

    /** Sincronização periódica enquanto há sessão iniciada. */
    fun startPeriodic()

    /** Cancela tudo (logout). */
    fun cancelAll()
}

/**
 * [SyncScheduler] com WorkManager: o sistema executa o [SyncWorker] quando a
 * restrição de rede é satisfeita — inclusive depois de o app ser fechado ou o
 * aparelho reiniciado —, com recuo exponencial entre tentativas.
 */
@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) : SyncScheduler {

    private val connected = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // APPEND_OR_REPLACE: se uma sincronização já está rodando, a nova roda
        // em seguida — garante que operações enfileiradas durante a atual subam.
        workManager.enqueueUniqueWork(WORK_ONE_TIME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    override fun startPeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIOD_HOURS, TimeUnit.HOURS)
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun cancelAll() {
        workManager.cancelUniqueWork(WORK_ONE_TIME)
        workManager.cancelUniqueWork(WORK_PERIODIC)
    }

    private companion object {
        const val WORK_ONE_TIME = "lifeforge-sync"
        const val WORK_PERIODIC = "lifeforge-sync-periodic"
        const val PERIOD_HOURS = 6L
    }
}
