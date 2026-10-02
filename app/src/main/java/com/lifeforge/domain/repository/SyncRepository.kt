package com.lifeforge.domain.repository

import com.lifeforge.domain.model.DataResult
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Estado da sincronização offline-first, exibido pela interface.
 *
 * @property isOnline há rede com acesso à internet neste momento.
 * @property pendingOperations alterações gravadas no aparelho que ainda não
 *   chegaram ao servidor (fila de saída).
 * @property lastSyncAt última sincronização completa bem-sucedida.
 */
data class SyncStatus(
    val isOnline: Boolean = true,
    val pendingOperations: Int = 0,
    val lastSyncAt: Instant? = null,
)

/**
 * Sincronização entre o banco local (fonte única de verdade) e a API.
 *
 * As telas sempre leem do banco local; este contrato apenas controla quando
 * as alterações pendentes sobem e quando o cache é atualizado a partir do
 * servidor — em segundo plano, sem bloquear a interface.
 */
interface SyncRepository {

    fun observeStatus(): Flow<SyncStatus>

    /** Sincroniza agora (envia pendências e atualiza o cache) e devolve o resultado. */
    suspend fun syncNow(): DataResult<Unit>

    /** Pede uma sincronização em segundo plano, executada assim que houver rede. */
    fun requestSync()

    /** Agenda a sincronização periódica em segundo plano (sessão iniciada). */
    fun startBackgroundSync()

    /** Cancela as sincronizações agendadas (logout). */
    fun stopBackgroundSync()
}
