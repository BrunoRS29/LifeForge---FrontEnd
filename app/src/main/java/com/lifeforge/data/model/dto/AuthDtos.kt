package com.lifeforge.data.model.dto

import kotlinx.serialization.Serializable

/**
 * DTOs da camada HTTP. Espelham linha-a-linha os DTOs do backend Ktor
 * (`com.lifeforge.dto`). Qualquer divergência aqui quebra a comunicação
 * em runtime.
 *
 * Convenções herdadas do backend:
 * - `BigDecimal` → String (formato decimal sem notação científica)
 * - `Instant` → String (ISO-8601 UTC)
 * - Enums → String (`.name`)
 * - IDs Long em endpoints de simulação/otimização → String
 */

// ============================================================================
// Erros
// ============================================================================

@Serializable
data class ErrorResponseDto(
    val error: String,    // código curto: "VALIDATION", "NOT_FOUND", etc.
    val message: String,  // descrição legível
)

// ============================================================================
// Auth
// ============================================================================

@Serializable
data class RegisterRequestDto(
    val email: String,
    val name: String,
    val password: String,
    val riskProfile: String? = null,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val user: UserDto,
)

// ============================================================================
// User
// ============================================================================

@Serializable
data class UserDto(
    val id: Long,
    val email: String,
    val name: String,
    val riskProfile: String,
    val createdAt: String,
)

/**
 * Body do PATCH /users/me/risk-profile.
 *
 * Endpoint dedicado em vez de PATCH /me generico para tornar a intencao
 * explicita — perfil de risco e o unico campo do usuario editavel nesta
 * sprint. Nome/email teriam fluxos proprios (verificacao de unicidade,
 * confirmacao por email) que ficam fora do escopo do TCC.
 */
@Serializable
data class UpdateRiskProfileRequestDto(
    val riskProfile: String,  // CONSERVATIVE | MODERATE | AGGRESSIVE
)

/** Body do PATCH /users/me/name — troca do nome de exibição. */
@Serializable
data class UpdateNameRequestDto(
    val name: String,
)
