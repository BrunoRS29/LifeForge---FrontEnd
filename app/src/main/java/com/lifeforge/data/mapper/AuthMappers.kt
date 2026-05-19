package com.lifeforge.data.mapper

import com.lifeforge.data.db.entity.UserEntity
import com.lifeforge.data.model.dto.UserDto
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.model.User
import java.time.Instant

/**
 * Mappers do agregado de autenticação. Sem entity-update porque o user
 * é cache-only: sempre vem do backend, nunca sobe para lá.
 */

// ============================================================================
// DTO → Domain
// ============================================================================

/**
 * Converte o DTO de usuário do wire para o modelo de domínio.
 * Falha (lança) se [riskProfile] não for um valor válido — esse caso
 * indica contrato quebrado entre cliente e servidor e deve subir como
 * AppError.Unknown via `mapCatching` no repositório.
 */
fun UserDto.toDomain(): User = User(
    id = id,
    email = email,
    name = name,
    riskProfile = RiskProfile.valueOf(riskProfile),
    createdAt = Instant.parse(createdAt),
)

// ============================================================================
// DTO → Entity (atalho para gravar direto sem passar por domain)
// ============================================================================

fun UserDto.toEntity(): UserEntity = UserEntity(
    id = id,
    email = email,
    name = name,
    riskProfile = riskProfile,
    createdAt = Instant.parse(createdAt),
)

// ============================================================================
// Entity → Domain
// ============================================================================

fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    name = name,
    riskProfile = RiskProfile.valueOf(riskProfile),
    createdAt = createdAt,
)

// ============================================================================
// Domain → Entity (usado quando precisamos persistir um User vindo de
// camadas superiores — em prática só o AuthRepositoryImpl chama este)
// ============================================================================

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    email = email,
    name = name,
    riskProfile = riskProfile.name,
    createdAt = createdAt,
)
