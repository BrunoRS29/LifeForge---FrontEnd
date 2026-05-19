package com.lifeforge.presentation.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlin.reflect.KClass

/**
 * Bottom bar do fluxo autenticado. Renderizado pelo Scaffold do
 * NavGraph **apenas** quando o destino atual é uma das abas (ver
 * [shouldShowBottomBar] em [LifeForgeNavGraph]).
 *
 * Política de navegação ao tocar uma aba:
 * - **popUpTo(startDestination)**: limpa a back stack até a raiz,
 *   evitando empilhar abas como histórico. Isso reproduz o
 *   comportamento padrão de apps Android (Twitter, Instagram, etc.).
 * - **saveState/restoreState**: cada aba lembra sua posição interna
 *   ao ser revisitada — ex.: scrollar a lista de Metas e ir para
 *   Perfil/voltar deve preservar o scroll.
 * - **launchSingleTop**: tocar a aba atual não cria nova instância.
 */
@Composable
fun LifeForgeBottomBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        LifeForgeBottomTabs.forEach { tab ->
            val selected = currentDestination.matchesRoute(tab.route::class)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.iconSelected else tab.iconUnselected,
                        contentDescription = tab.label,
                    )
                },
                label = { Text(tab.label) },
            )
        }
    }
}

/**
 * Verifica se o destino atual corresponde a uma rota tipada.
 *
 * `NavDestination.hasRoute(KClass)` é a API recomendada do Nav
 * Compose 2.8+ para comparar destinos serializáveis sem stringificar.
 */
private fun NavDestination?.matchesRoute(route: KClass<*>): Boolean =
    this?.hierarchy?.any { it.hasRoute(route) } == true
