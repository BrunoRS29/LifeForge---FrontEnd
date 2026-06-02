package com.lifeforge.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lifeforge.presentation.screen.auth.LoginScreen
import com.lifeforge.presentation.screen.auth.RegisterScreen
import com.lifeforge.presentation.screen.dashboard.DashboardScreen
import com.lifeforge.presentation.screen.finance.FinanceScreen
import com.lifeforge.presentation.screen.goal.GoalDetailScreen
import com.lifeforge.presentation.screen.goal.GoalEditScreen
import com.lifeforge.presentation.screen.goal.GoalsListScreen
import com.lifeforge.presentation.screen.optimization.OptimizationScreen
import com.lifeforge.presentation.screen.profile.ProfileScreen
import com.lifeforge.presentation.screen.simulation.SimulationScreen
import com.lifeforge.presentation.navigation.Predictions
import com.lifeforge.presentation.navigation.SimulationCalibrated
import com.lifeforge.presentation.screen.prediction.PredictionScreen
import com.lifeforge.presentation.screen.simulation.SimulationCalibratedScreen

/**
 * Grafo de navegação raiz do LifeForge.
 *
 * Arquitetura:
 * - **Single NavHost** com todas as rotas (auth + main + sub-rotas).
 *   Mais simples que multi-graph aninhado para um app deste tamanho.
 * - **Bottom bar condicional**: o [Scaffold] só renderiza a barra
 *   inferior quando o destino atual é uma das 5 abas. Em
 *   Login/Register/GoalDetail/GoalEdit/Simulation a barra desaparece,
 *   dando mais espaço ao conteúdo.
 * - **Auto-routing por sessão**: um [LaunchedEffect] observa o
 *   [RootSessionViewModel] — ao logout, navega para [Login] e limpa
 *   toda a pilha. Ao login, vai para [Dashboard]. Garante que o
 *   usuário nunca consegue voltar com o gesto "back" para uma tela
 *   autenticada após sair.
 */
@Composable
fun LifeForgeNavGraph(
    rootViewModel: RootSessionViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val sessionState by rootViewModel.state.collectAsState()

    // Reage a mudanças de sessão (login/logout).
    LaunchedEffect(sessionState) {
        when (sessionState) {
            SessionUiState.Loading -> Unit
            SessionUiState.Unauthenticated -> {
                navController.navigate(Login) {
                    // Limpa toda a back stack — usuário recém-deslogado
                    // não deve conseguir voltar para tela autenticada.
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
            SessionUiState.Authenticated -> {
                val backStackEntry = navController.currentBackStackEntry
                val onAuthScreen = backStackEntry?.destination?.let { dest ->
                    dest.hasRoute(Login::class) || dest.hasRoute(Register::class)
                } ?: true
                if (onAuthScreen) {
                    navController.navigate(Dashboard) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    // Decide se mostra bottom bar com base no destino atual.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = LifeForgeBottomTabs.any { tab ->
        currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }

    if (sessionState is SessionUiState.Loading) {
        // Splash mínimo — em Sprint 5 podemos trocar por uma tela
        // dedicada com logo enquanto o DataStore inicializa.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                LifeForgeBottomBar(navController)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            // O destino inicial é arbitrário aqui — o LaunchedEffect
            // acima reage à sessão e navega corretamente. Usamos Login
            // como fallback porque é o estado mais comum no app fechado.
            startDestination = Login,
            modifier = Modifier.padding(padding),
        ) {
            // ============== Auth flow ==============
            composable<Login> {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Register) },
                )
            }

            composable<SimulationCalibrated> {
                SimulationCalibratedScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable<Predictions> {
                PredictionScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable<Register> {
                RegisterScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // ============== Main flow (abas) ==============
            composable<Dashboard> {
                DashboardScreen()
            }
            composable<GoalsList> {
                GoalsListScreen(
                    onGoalClick = { goalId -> navController.navigate(GoalDetail(goalId)) },
                    onCreateGoal = { navController.navigate(GoalEdit()) },
                )
            }
            composable<Finance> {
                FinanceScreen()
            }
            composable<Optimization> {
                OptimizationScreen()
            }
            composable<Profile> {
                ProfileScreen(
                    onLogout = { rootViewModel.logout() },
                )
            }

            // ============== Sub-rotas ==============
            composable<GoalDetail> { backStack ->
                val args = backStack.toRoute<GoalDetail>()
                GoalDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(GoalEdit(args.goalId)) },
                    onSimulate = { navController.navigate(Simulation(args.goalId)) },
                    onSimulateWithAi = { navController.navigate(SimulationCalibrated(args.goalId)) },  // <-- NOVO
                )
            }
            composable<GoalEdit> {
                GoalEditScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable<Simulation> {
                SimulationScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
