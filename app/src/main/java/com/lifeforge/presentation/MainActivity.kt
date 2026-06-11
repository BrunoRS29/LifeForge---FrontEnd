package com.lifeforge.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lifeforge.data.preferences.ThemeMode
import com.lifeforge.presentation.navigation.LifeForgeNavGraph
import com.lifeforge.presentation.theme.LifeForgeTheme
import com.lifeforge.presentation.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity unica do app (single-activity architecture).
 *
 * - [AndroidEntryPoint]: Hilt injeta nos ViewModels descendentes
 *   (RootSessionViewModel e ViewModels de tela).
 * - [enableEdgeToEdge]: conteudo desenha sob status/navigation bars.
 * - [ThemeViewModel]: observa preferencia de tema (SYSTEM/LIGHT/DARK)
 *   e a [LifeForgeTheme] aplica. Pertence ao escopo do Activity, nao
 *   do Composable, para o tema ser estavel durante toda a sessao do app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
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
                Surface {
                    LifeForgeNavGraph()
                }
            }
        }
    }
}
