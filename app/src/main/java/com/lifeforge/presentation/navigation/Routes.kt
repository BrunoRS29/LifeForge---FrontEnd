package com.lifeforge.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/**
 * Rotas type-safe do LifeForge usando o suporte a [kotlinx.serialization]
 * que entrou no Navigation Compose 2.8+.
 *
 * Em vez de strings frágeis ("goal/{id}"), cada destino é um objeto ou
 * data class serializável. O Nav Compose lê a anotação @Serializable
 * para construir a rota e expor os argumentos com tipos preservados.
 * Vantagens: refactor seguro, sem typos, sem casts.
 *
 * Convenção: `object` para rotas sem parâmetros, `data class` para
 * rotas com argumentos.
 */

// ============================================================================
// Auth flow
// ============================================================================

@Serializable
data object Login

@Serializable
data object Register

// ============================================================================
// Main flow — abas do bottom bar
// ============================================================================

@Serializable
data object Dashboard

@Serializable
data object GoalsList

@Serializable
data object Finance

@Serializable
data object Optimization

@Serializable
data object Profile

// ============================================================================
// Sub-rotas (full-screen, sem bottom bar)
// ============================================================================

@Serializable
data class GoalDetail(val goalId: Long)

/**
 * Reutilizada para criar (id = null) e editar (id != null).
 * Default `null` para que `navController.navigate(GoalEdit())` cria uma nova.
 */
@Serializable
data class GoalEdit(val goalId: Long? = null)

@Serializable
data class Simulation(val goalId: Long)

// ============================================================================
// Metadata para o bottom bar
// ============================================================================

/**
 * Descritor de aba do bottom navigation. Mantém juntos rota, label e
 * ícones (preenchido quando selecionada, contorno quando não).
 *
 * `route: Any` porque cada destino é de um tipo diferente
 * (`Dashboard`, `GoalsList`, etc.) — todos `@Serializable`, mas sem
 * superclasse comum no Nav Compose. O `Any` é seguro aqui porque o
 * NavController valida dispatching internamente.
 */
data class BottomTab(
    val route: Any,
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector,
)

val LifeForgeBottomTabs: List<BottomTab> = listOf(
    BottomTab(
        route = Dashboard,
        label = "Dashboard",
        iconSelected = Icons.Rounded.Dashboard,
        iconUnselected = Icons.Outlined.Dashboard,
    ),
    BottomTab(
        route = GoalsList,
        label = "Metas",
        iconSelected = Icons.Rounded.Flag,
        iconUnselected = Icons.Outlined.Flag,
    ),
    BottomTab(
        route = Finance,
        label = "Finanças",
        iconSelected = Icons.Rounded.Wallet,
        iconUnselected = Icons.Outlined.Wallet,
    ),
    BottomTab(
        route = Optimization,
        label = "Otimizar",
        iconSelected = Icons.Rounded.AutoGraph,
        iconUnselected = Icons.Outlined.AutoGraph,
    ),
    BottomTab(
        route = Profile,
        label = "Perfil",
        iconSelected = Icons.Rounded.AccountCircle,
        iconUnselected = Icons.Outlined.AccountCircle,
    ),
)
