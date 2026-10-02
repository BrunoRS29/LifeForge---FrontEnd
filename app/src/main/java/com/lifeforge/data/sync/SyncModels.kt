package com.lifeforge.data.sync

import com.lifeforge.domain.model.AppError

/** Estado de sincronização de uma linha do banco local (coluna `syncState`). */
enum class SyncState { SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE }

/** Entidades editáveis sem conexão. */
enum class OutboxEntityType { GOAL, INCOME, EXPENSE, ASSET }

/** Operação pendente na fila de saída. */
enum class OutboxOperation { CREATE, UPDATE, DELETE }

/** Resultado do envio de uma operação pendente ao servidor. */
sealed interface PushOutcome {

    /** Aplicada no servidor; [serverId] é o id definitivo da entidade. */
    data class Synced(val serverId: Long) : PushOutcome

    /**
     * Rejeitada pelo servidor (validação, conflito, recurso inexistente):
     * tentar de novo não adianta — a operação é descartada e a alteração
     * local, desfeita.
     */
    data class Rejected(val error: AppError) : PushOutcome

    /** Falha transitória (rede, 5xx, sessão expirada): fica na fila. */
    data class Retry(val error: AppError) : PushOutcome
}

/** Classifica um erro de API em rejeição definitiva ou nova tentativa. */
internal fun AppError.toPushOutcome(): PushOutcome = when (this) {
    is AppError.Validation, is AppError.Conflict, is AppError.NotFound -> PushOutcome.Rejected(this)
    else -> PushOutcome.Retry(this)
}
