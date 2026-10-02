package com.lifeforge.domain.repository

import com.lifeforge.domain.model.UsabilitySession
import kotlinx.coroutines.flow.Flow

/**
 * Registro das sessões de avaliação de usabilidade. Fica no aparelho do
 * avaliador e sobrevive ao logout (são dados da pesquisa, não do usuário).
 */
interface UsabilityRepository {
    fun observeSessions(): Flow<List<UsabilitySession>>

    /** Grava a sessão e a devolve com o id atribuído. */
    suspend fun save(session: UsabilitySession): UsabilitySession

    suspend fun delete(id: Long)
}
