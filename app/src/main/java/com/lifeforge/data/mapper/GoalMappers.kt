package com.lifeforge.data.mapper

import com.lifeforge.data.db.entity.GoalEntity
import com.lifeforge.data.model.dto.GoalDto
import com.lifeforge.data.model.dto.GoalRequestDto
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalCategory
import java.math.BigDecimal
import java.time.Instant

/**
 * Mappers entre as três representações de Goal: DTO (wire), Entity (Room)
 * e Domain (camada `domain/`).
 *
 * O caminho típico de criação:
 * 1. UseCase recebe Goal-like params do ViewModel
 * 2. Repository chama [toRequestDto] para montar o body Retrofit
 * 3. API retorna GoalDto → [GoalDto.toEntity] → grava no Room
 * 4. Flow do Room emite → [GoalEntity.toDomain] → ViewModel
 */

// ============================================================================
// DTO ↔ Entity (sem passar por Domain — atalho para o repository)
// ============================================================================

fun GoalDto.toEntity(): GoalEntity = GoalEntity(
    id = id,
    userId = userId,
    name = name,
    category = category,
    targetAmount = BigDecimal(targetAmount),
    targetDate = Instant.parse(targetDate),
    priority = priority,
    createdAt = Instant.parse(createdAt),
)

// ============================================================================
// Entity → Domain
// ============================================================================

fun GoalEntity.toDomain(): Goal = Goal(
    id = id,
    userId = userId,
    name = name,
    category = GoalCategory.valueOf(category),
    targetAmount = targetAmount,
    targetDate = targetDate,
    priority = priority,
    createdAt = createdAt,
)

// ============================================================================
// Domain → Entity (raramente usado; mantido por simetria)
// ============================================================================

fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    userId = userId,
    name = name,
    category = category.name,
    targetAmount = targetAmount,
    targetDate = targetDate,
    priority = priority,
    createdAt = createdAt,
)

// ============================================================================
// Builder de request — usado no create/update do repository
// ============================================================================

/**
 * Monta o body para POST/PUT a partir dos parâmetros que a UI já validou.
 *
 * BigDecimal vai como `toPlainString` (sem notação científica) e Instant
 * como ISO-8601 — convenções herdadas do backend Ktor.
 */
fun goalRequestDto(
    name: String,
    category: GoalCategory,
    targetAmount: BigDecimal,
    targetDate: Instant,
    priority: Int,
): GoalRequestDto = GoalRequestDto(
    name = name,
    category = category.name,
    targetAmount = targetAmount.toPlainString(),
    targetDate = targetDate.toString(),
    priority = priority,
)
