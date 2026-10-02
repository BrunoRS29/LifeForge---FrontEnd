package com.lifeforge.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Itens da navegação principal, renderizados pelo `NavigationSuiteScaffold`: barra
 * inferior curta (Material 3 Expressive) em telas compactas e trilho lateral em
 * tablets, dobráveis abertos e paisagem — a mesma lista, o componente certo para
 * cada largura de janela (Seção "Adaptive navigation" das diretrizes).
 *
 * Política ao tocar um destino:
 * - **popUpTo(startDestination)**: limpa a pilha até a raiz, sem empilhar abas
 *   como histórico (comportamento padrão de apps Android);
 * - **saveState/restoreState**: cada aba lembra a posição interna (scroll etc.);
 * - **launchSingleTop**: tocar a aba atual não cria outra instância.
 */
@Composable
fun LifeForgeNavigationItems(
    navController: NavController,
    currentDestination: NavDestination?,
    navigationSuiteType: NavigationSuiteType,
) {
    LifeForgeBottomTabs.forEach { tab ->
        val selected = currentDestination.isTab(tab)
        NavigationSuiteItem(
            selected = selected,
            onClick = {
                navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = {
                Icon(
                    imageVector = if (selected) tab.iconSelected else tab.iconUnselected,
                    contentDescription = null,
                )
            },
            label = { Text(tab.label) },
            navigationSuiteType = navigationSuiteType,
        )
    }
}

/** Tipo de navegação recomendado para a janela atual (barra curta ou trilho). */
@Composable
fun rememberNavigationSuiteType(): NavigationSuiteType =
    NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())

/** O destino é (ou está dentro de) uma das abas principais? */
fun NavDestination?.isTopLevel(): Boolean = LifeForgeBottomTabs.any { isTab(it) }

private fun NavDestination?.isTab(tab: BottomTab): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.route::class) } == true

/**
 * Transições de tela do Material (com o esquema de movimento expressivo do tema):
 *
 * - **entre abas** — *fade through*: o conteúdo some e o novo surge com leve
 *   escala, sem sugerir hierarquia;
 * - **para uma tela de detalhe e de volta** — *eixo compartilhado horizontal*:
 *   a nova tela entra deslizando pouco e com esmaecimento; ao voltar, o inverso.
 *   O voltar preditivo (gesto) acompanha a mesma transição com o dedo.
 */
@Immutable
class LifeForgeScreenTransitions(
    private val offsetSpec: FiniteAnimationSpec<IntOffset>,
    private val fadeSpec: FiniteAnimationSpec<Float>,
) {
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isBetweenTabs()) {
            fadeIn(fadeSpec) + scaleIn(fadeSpec, initialScale = 0.96f)
        } else {
            slideInHorizontally(offsetSpec) { it / SHARED_AXIS_FRACTION } + fadeIn(fadeSpec)
        }
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isBetweenTabs()) {
            fadeOut(fadeSpec)
        } else {
            slideOutHorizontally(offsetSpec) { -it / SHARED_AXIS_FRACTION } + fadeOut(fadeSpec)
        }
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isBetweenTabs()) {
            fadeIn(fadeSpec)
        } else {
            slideInHorizontally(offsetSpec) { -it / SHARED_AXIS_FRACTION } + fadeIn(fadeSpec)
        }
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isBetweenTabs()) {
            fadeOut(fadeSpec)
        } else {
            slideOutHorizontally(offsetSpec) { it / SHARED_AXIS_FRACTION } + fadeOut(fadeSpec)
        }
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.isBetweenTabs(): Boolean =
        initialState.destination.isTopLevel() && targetState.destination.isTopLevel()

    private companion object {
        /** A tela desliza 1/8 da largura: sugere direção sem "arrastar" o conteúdo todo. */
        const val SHARED_AXIS_FRACTION = 8
    }
}

/** Transições com as especificações de movimento do tema (molas do Material 3 Expressive). */
@Composable
fun rememberScreenTransitions(): LifeForgeScreenTransitions {
    val motion = MaterialTheme.motionScheme
    val offsetSpec = motion.defaultSpatialSpec<IntOffset>()
    val fadeSpec = motion.defaultEffectsSpec<Float>()
    return remember(offsetSpec, fadeSpec) { LifeForgeScreenTransitions(offsetSpec, fadeSpec) }
}
