package com.lifeforge.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de cores LifeForge.
 *
 * Filosofia visual:
 * - **Primary teal** (#1B5E5C — verde-azulado profundo): transmite
 *   confiança e crescimento, valores centrais para um app financeiro.
 * - **Secondary gold** (#C77800): acento para conquistas, valores
 *   alvo atingidos, CTAs principais. Conecta com a metáfora do nome
 *   "LifeForge" (forjar — calor, ouro).
 * - **Tertiary slate** (#4D5360): neutro de apoio para metadados,
 *   timestamps, texto auxiliar.
 *
 * Os tokens seguem a convenção Material 3 — para cada cor "main", há
 * `OnX` (texto/ícone sobre ela), `XContainer` (container preenchido)
 * e `OnXContainer` (texto/ícone no container).
 */

// ============================================================================
// Light scheme
// ============================================================================

internal val LightPrimary = Color(0xFF1B5E5C)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFA8F2EC)
internal val LightOnPrimaryContainer = Color(0xFF00201E)

internal val LightSecondary = Color(0xFFC77800)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFFFDDB6)
internal val LightOnSecondaryContainer = Color(0xFF2A1700)

internal val LightTertiary = Color(0xFF4D5360)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFD0D6E5)
internal val LightOnTertiaryContainer = Color(0xFF09111D)

internal val LightError = Color(0xFFBA1A1A)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF410002)

internal val LightBackground = Color(0xFFF6FBFA)
internal val LightOnBackground = Color(0xFF161C1C)
internal val LightSurface = Color(0xFFF6FBFA)
internal val LightOnSurface = Color(0xFF161C1C)
internal val LightSurfaceVariant = Color(0xFFDBE5E4)
internal val LightOnSurfaceVariant = Color(0xFF3F4948)
internal val LightOutline = Color(0xFF6F7978)

// ============================================================================
// Dark scheme
// ============================================================================

internal val DarkPrimary = Color(0xFF8CD5D0)
internal val DarkOnPrimary = Color(0xFF003735)
internal val DarkPrimaryContainer = Color(0xFF00504D)
internal val DarkOnPrimaryContainer = Color(0xFFA8F2EC)

internal val DarkSecondary = Color(0xFFFFB876)
internal val DarkOnSecondary = Color(0xFF482900)
internal val DarkSecondaryContainer = Color(0xFF673D00)
internal val DarkOnSecondaryContainer = Color(0xFFFFDDB6)

internal val DarkTertiary = Color(0xFFB4BBC9)
internal val DarkOnTertiary = Color(0xFF1F2632)
internal val DarkTertiaryContainer = Color(0xFF353C48)
internal val DarkOnTertiaryContainer = Color(0xFFD0D6E5)

internal val DarkError = Color(0xFFFFB4AB)
internal val DarkOnError = Color(0xFF690005)
internal val DarkErrorContainer = Color(0xFF93000A)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)

internal val DarkBackground = Color(0xFF0E1414)
internal val DarkOnBackground = Color(0xFFDDE4E3)
internal val DarkSurface = Color(0xFF0E1414)
internal val DarkOnSurface = Color(0xFFDDE4E3)
internal val DarkSurfaceVariant = Color(0xFF3F4948)
internal val DarkOnSurfaceVariant = Color(0xFFBFC9C8)
internal val DarkOutline = Color(0xFF899392)
