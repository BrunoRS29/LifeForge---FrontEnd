package com.lifeforge.data.sync

import androidx.room.withTransaction
import com.lifeforge.data.db.LifeForgeDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executa um bloco de forma atômica no banco local. Abstraído para que a
 * lógica de sincronização seja testável na JVM sem um banco real.
 */
interface TransactionRunner {
    suspend fun <R> run(block: suspend () -> R): R
}

@Singleton
class RoomTransactionRunner @Inject constructor(
    private val database: LifeForgeDatabase,
) : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R = database.withTransaction { block() }
}
