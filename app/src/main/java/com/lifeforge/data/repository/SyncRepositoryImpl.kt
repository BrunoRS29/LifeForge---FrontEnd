package com.lifeforge.data.repository

import com.lifeforge.data.sync.LastSyncStore
import com.lifeforge.data.sync.NetworkMonitor
import com.lifeforge.data.sync.Outbox
import com.lifeforge.data.sync.SyncEngine
import com.lifeforge.data.sync.SyncScheduler
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.repository.SyncRepository
import com.lifeforge.domain.repository.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** Expõe à apresentação o estado e os comandos da sincronização offline-first. */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val engine: SyncEngine,
    private val outbox: Outbox,
    private val network: NetworkMonitor,
    private val scheduler: SyncScheduler,
    private val lastSync: LastSyncStore,
) : SyncRepository {

    override fun observeStatus(): Flow<SyncStatus> =
        combine(network.isOnline, outbox.observeCount(), lastSync.lastSyncAt) { online, pending, last ->
            SyncStatus(isOnline = online, pendingOperations = pending, lastSyncAt = last)
        }

    override suspend fun syncNow(): DataResult<Unit> = engine.syncAll()

    override fun requestSync() = scheduler.requestSync()

    override fun startBackgroundSync() {
        scheduler.startPeriodic()
        scheduler.requestSync()
    }

    override fun stopBackgroundSync() = scheduler.cancelAll()
}
