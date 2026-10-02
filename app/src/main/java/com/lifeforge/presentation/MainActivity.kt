package com.lifeforge.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lifeforge.data.preferences.ThemeMode
import com.lifeforge.presentation.navigation.LifeForgeNavGraph
import com.lifeforge.presentation.navigation.RootSessionViewModel
import com.lifeforge.presentation.navigation.SessionUiState
import com.lifeforge.presentation.theme.LifeForgeTheme
import com.lifeforge.presentation.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity unica do app (single-activity architecture).
 *
 * - [AndroidEntryPoint]: Hilt injeta nos ViewModels descendentes.
 * - Splash nativa: o ícone fica na tela até a sessão ser lida (token no
 *   DataStore) — sem um spinner intermediário entre a splash e a primeira tela.
 * - [enableEdgeToEdge]: conteudo desenha sob status/navigation bars.
 * - [ThemeViewModel]: observa preferencia de tema (SYSTEM/LIGHT/DARK)
 *   e a [LifeForgeTheme] aplica. Pertence ao escopo do Activity, nao
 *   do Composable, para o tema ser estavel durante toda a sessao do app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    /** O mesmo ViewModel que o NavGraph usa (escopo da Activity). */
    private val rootViewModel: RootSessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            rootViewModel.state.value is SessionUiState.Loading
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themeViewModel.themeMode.collectAsState()
            val dynamicColor by themeViewModel.dynamicColor.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            LifeForgeTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                LifeForgeNavGraph(rootViewModel)
            }
        }
    }
}
