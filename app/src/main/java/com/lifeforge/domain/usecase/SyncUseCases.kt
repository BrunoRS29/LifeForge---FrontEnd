package com.lifeforge.domain.usecase

import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.repository.SyncRepository
import com.lifeforge.domain.repository.SyncStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCases da sincronização offline-first. Pass-through — a política (fila,
 * ordem, tentativas) vive na camada de dados; a apresentação só observa o
 * estado e dispara comandos.
 */

class ObserveSyncStatusUseCase @Inject constructor(
    private val repository: SyncRepository,
) {
    operator fun invoke(): Flow<SyncStatus> = repository.observeStatus()
}

/** Sincroniza agora: envia as pendências e atualiza o cache (ação explícita do usuário). */
class SyncNowUseCase @Inject constructor(
    private val repository: SyncRepository,
) {
    suspend operator fun invoke(): DataResult<Unit> = repository.syncNow()
}

/** Liga/desliga a sincronização em segundo plano conforme a sessão. */
class SetBackgroundSyncUseCase @Inject constructor(
    private val repository: SyncRepository,
) {
    operator fun invoke(enabled: Boolean) {
        if (enabled) repository.startBackgroundSync() else repository.stopBackgroundSync()
    }
}
