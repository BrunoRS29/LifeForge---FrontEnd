package com.lifeforge.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lifeforge.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO do usuário corrente.
 *
 * Usa `LIMIT 1` em todas as queries porque o app garante (via
 * `clearAllTables()` no logout) que existe no máximo uma linha em
 * `users` a qualquer momento.
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM users LIMIT 1")
    fun observeCurrent(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun findCurrent(): UserEntity?

    @Upsert
    suspend fun upsert(entity: UserEntity)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
