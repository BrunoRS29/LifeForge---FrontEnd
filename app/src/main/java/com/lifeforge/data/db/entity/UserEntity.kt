package com.lifeforge.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Cache local do usuário autenticado.
 *
 * Tem no máximo 1 linha em qualquer momento — quando o usuário faz
 * logout ou troca de conta, [com.lifeforge.data.repository.AuthRepositoryImpl]
 * chama `database.clearAllTables()` para garantir isolamento.
 *
 * `riskProfile` é gravado como String (nome do enum) para manter o schema
 * legível e estável: adicionar um novo perfil de risco no domínio não
 * exige migração de banco.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val email: String,
    val name: String,
    val riskProfile: String,
    val createdAt: Instant,
)
