package com.lifeforge.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifeforge.presentation.common.SyncStatusBar
import com.lifeforge.presentation.common.UsabilityTaskBar
import com.lifeforge.presentation.screen.auth.LoginScreen
import com.lifeforge.presentation.screen.auth.RegisterScreen
import com.lifeforge.presentation.screen.dashboard.DashboardScreen
import com.lifeforge.presentation.screen.finance.FinanceScreen
import com.lifeforge.presentation.screen.goal.GoalDetailScreen
import com.lifeforge.presentation.screen.goal.GoalEditScreen
import com.lifeforge.presentation.screen.goal.GoalsListScreen
import com.lifeforge.presentation.screen.imports.ImportScreen
import com.lifeforge.presentation.screen.optimization.OptimizationScreen
import com.lifeforge.presentation.screen.prediction.PredictionScreen
import com.lifeforge.presentation.screen.profile.ProfileParamsScreen
import com.lifeforge.presentation.screen.profile.ProfileScreen
import com.lifeforge.presentation.screen.simulation.SimulationCalibratedScreen
import com.lifeforge.presentation.screen.simulation.SimulationCompareScreen
import com.lifeforge.presentation.screen.simulation.SimulationScreen
import com.lifeforge.presentation.screen.usability.UsabilityBarViewModel
import com.lifeforge.presentation.screen.usability.UsabilityQuestionnaireScreen
import com.lifeforge.presentation.screen.usability.UsabilityScreen

/**
 * Grafo de navegação raiz do LifeForge.
 *
 * Arquitetura:
 * - **Single NavHost** com todas as rotas (auth + main + sub-rotas).
 *   Mais simples que multi-graph aninhado para um app deste tamanho.
 * - **Navegação adaptativa**: `NavigationSuiteScaffold` mostra a barra
 *   inferior curta (telas compactas) ou o trilho lateral (tablets,
 *   paisagem) nas 5 abas e se recolhe, animada, nas telas de detalhe,
 *   formulários e no login — mais espaço para o conteúdo.
 * - **Transições do Material**: *fade through* entre abas e eixo
 *   horizontal compartilhado para entrar e sair de detalhes (ver
 *   [LifeForgeScreenTransitions]), acompanhando o voltar preditivo.
 * - **Auto-routing por sessão**: um [LaunchedEffect] observa o
 *   [RootSessionViewModel] — ao logout, navega para [Login] e limpa
 *   toda a pilha. Ao login, vai para [Dashboard]. Garante que o
 *   usuário nunca consegue voltar com o gesto "back" para uma tela
 *   autenticada após sair.
 */
@OptIn(ExperimentalLayoutApi::class)
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

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val authenticated = sessionState is SessionUiState.Authenticated
    // Com o teclado aberto, a navegação e as faixas globais saem de cena: o
    // formulário ganha a altura toda e a ação fixa encosta no teclado.
    val imeVisible = WindowInsets.isImeVisible
    val showNavigation = authenticated && currentDestination.isTopLevel() && !imeVisible

    // Enquanto a sessão é lida (DataStore), a splash nativa continua na tela
    // (setKeepOnScreenCondition na MainActivity) — nada a desenhar aqui.
    if (sessionState is SessionUiState.Loading) {
        Box(Modifier.fillMaxSize())
        return
    }

    val syncStatus by rootViewModel.syncStatus.collectAsState()
    val isSyncing by rootViewModel.isSyncing.collectAsState()
    val usabilityBarViewModel: UsabilityBarViewModel = hiltViewModel()
    val usabilitySession by usabilityBarViewModel.active.collectAsState()

    val navigationSuiteType = rememberNavigationSuiteType()
    val navigationState = rememberNavigationSuiteScaffoldState(
        if (showNavigation) NavigationSuiteScaffoldValue.Visible else NavigationSuiteScaffoldValue.Hidden,
    )
    LaunchedEffect(showNavigation) {
        if (showNavigation) navigationState.show() else navigationState.hide()
    }
    val transitions = rememberScreenTransitions()

    // Faixas globais abaixo do conteúdo: tarefa da avaliação de usabilidade em
    // andamento e estado da sincronização offline-first.
    val syncBarVisible = authenticated && (!syncStatus.isOnline || syncStatus.pendingOperations > 0)
    val barsBelowContent = !imeVisible && (syncBarVisible || (authenticated && usabilitySession != null))

    NavigationSuiteScaffold(
        navigationItems = {
            LifeForgeNavigationItems(navController, currentDestination, navigationSuiteType)
        },
        navigationSuiteType = navigationSuiteType,
        state = navigationState,
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    // Com uma faixa abaixo, a tela não precisa reservar a área
                    // dos gestos do sistema — a faixa cuida disso.
                    .then(
                        if (barsBelowContent && !showNavigation) {
                            Modifier.consumeWindowInsets(
                                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                NavHost(
                    navController = navController,
                    // O destino inicial é arbitrário aqui — o LaunchedEffect
                    // acima reage à sessão e navega corretamente. Usamos Login
                    // como fallback porque é o estado mais comum no app fechado.
                    startDestination = Login,
                    enterTransition = transitions.enter,
                    exitTransition = transitions.exit,
                    popEnterTransition = transitions.popEnter,
                    popExitTransition = transitions.popExit,
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
                        DashboardScreen(
                            onOpenPredictions = { navController.navigate(Predictions) },
                            onOpenGoal = { goalId -> navController.navigate(GoalDetail(goalId)) },
                            onSeeAllGoals = { navController.navigateToTab(GoalsList) },
                        )
                    }
                    composable<GoalsList> {
                        GoalsListScreen(
                            onGoalClick = { goalId -> navController.navigate(GoalDetail(goalId)) },
                            onCreateGoal = { navController.navigate(GoalEdit()) },
                        )
                    }
                    composable<Finance> {
                        FinanceScreen(
                            onNavigateToImport = { navController.navigate(StatementImport) },
                        )
                    }
                    composable<Optimization> {
                        OptimizationScreen()
                    }
                    composable<Profile> {
                        ProfileScreen(
                            onLogout = { rootViewModel.logout() },
                            onNavigateToParams = { navController.navigate(ProfileParams) },
                            onNavigateToPredictions = { navController.navigate(Predictions) },
                            onNavigateToUsability = { navController.navigate(UsabilityEvaluation) },
                        )
                    }

                    // ============== Sub-rotas ==============
                    composable<GoalDetail> {
                        // A tela informa o id ATUAL da meta: uma meta criada offline troca
                        // o id temporário pelo definitivo ao sincronizar.
                        GoalDetailScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onEdit = { goalId -> navController.navigate(GoalEdit(goalId)) },
                            onSimulate = { goalId -> navController.navigate(Simulation(goalId)) },
                            onSimulateWithAi = { goalId -> navController.navigate(SimulationCalibrated(goalId)) },
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
                            onCompare = { goalId, firstId, secondId ->
                                navController.navigate(SimulationCompare(goalId, firstId, secondId))
                            },
                        )
                    }
                    composable<SimulationCompare> {
                        SimulationCompareScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                    composable<StatementImport> {
                        ImportScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                    composable<UsabilityEvaluation> {
                        UsabilityScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onOpenQuestionnaire = { navController.navigate(UsabilityQuestionnaire) },
                        )
                    }
                    composable<UsabilityQuestionnaire> {
                        UsabilityQuestionnaireScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                    composable<ProfileParams> {
                        ProfileParamsScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                }
            }
            if (authenticated && !imeVisible) {
                // Avaliação de usabilidade: tarefa cronometrada visível em qualquer tela.
                UsabilityTaskBar(
                    active = usabilitySession,
                    onFinish = usabilityBarViewModel::finishTask,
                    onOpenQuestionnaire = { navController.navigate(UsabilityQuestionnaire) },
                )
                // Faixa offline-first: sem conexão / alterações aguardando envio.
                // Sem a navegação abaixo, ela mesma respeita a área de gestos.
                SyncStatusBar(
                    status = syncStatus,
                    isSyncing = isSyncing,
                    onSyncNow = rootViewModel::syncNow,
                    modifier = if (showNavigation) Modifier else Modifier.navigationBarsPadding(),
                )
            }
        }
    }
}
