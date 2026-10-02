package com.lifeforge.presentation.theme

import android.app.Activity
import android.app.UiModeManager
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme

/**
 * Escala de formas do LifeForge (Material 3 Expressive): cantos generosos, mais
 * suaves que o baseline. `medium` rege os cards; `extraLarge`, diálogos e os cards
 * de destaque; `extraSmall`, campos de texto e menus.
 */
internal val LifeForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
    largeIncreased = RoundedCornerShape(24.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
)

/** Nível de contraste pedido pelo usuário nas configurações do Android 14+. */
enum class ContrastLevel { Standard, Medium, High }

/**
 * Tema raiz do LifeForge (Material 3 Expressive).
 *
 * - **Cores:** os esquemas da marca em [Color.kt] — claro/escuro conforme a
 *   preferência do app (Sistema/Claro/Escuro) e, no Android 14+, no nível de
 *   contraste escolhido pelo usuário no sistema (padrão, médio ou alto).
 *   `dynamicColor` (Material You, Android 12+) troca a paleta pela do papel de
 *   parede; é uma opção do usuário no Perfil, desligada por padrão para a
 *   identidade visual ficar igual em qualquer aparelho.
 * - **Movimento:** `MotionScheme.expressive()` — animações com física de mola,
 *   que os componentes do Material usam automaticamente.
 * - **Formas e tipografia:** [LifeForgeShapes] e [LifeForgeTypography].
 * - **Gráficos:** o Vico não segue o MaterialTheme sozinho; o tema M3 do Vico
 *   amarra eixos e legendas ao esquema de cores (sem isso, com o app escuro e o
 *   sistema claro, os rótulos ficavam pretos sobre fundo escuro).
 */
@Composable
fun LifeForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val contrast = rememberSystemContrast()
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> brandColorScheme(darkTheme, contrast)
    }

    // Edge-to-edge: as barras do sistema ficam transparentes (enableEdgeToEdge na
    // MainActivity) e o conteúdo desenha atrás delas. Aqui só ajustamos a cor dos
    // ÍCONES das barras, porque o tema do app pode divergir do tema do sistema.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = LifeForgeShapes,
        typography = LifeForgeTypography,
    ) {
        ProvideVicoTheme(rememberM3VicoTheme(), content)
    }
}

/** Esquema da marca para o tema e o nível de contraste. */
fun brandColorScheme(darkTheme: Boolean, contrast: ContrastLevel): ColorScheme = when (contrast) {
    ContrastLevel.Standard -> if (darkTheme) LifeForgeDarkColorScheme else LifeForgeLightColorScheme
    ContrastLevel.Medium ->
        if (darkTheme) LifeForgeDarkMediumContrastColorScheme else LifeForgeLightMediumContrastColorScheme
    ContrastLevel.High ->
        if (darkTheme) LifeForgeDarkHighContrastColorScheme else LifeForgeLightHighContrastColorScheme
}

/**
 * Contraste escolhido pelo usuário em Configurações > Acessibilidade (Android 14+),
 * acompanhando mudanças com o app aberto. Valores do sistema: 0 = padrão,
 * 0,5 = médio, 1 = alto.
 */
@Composable
private fun rememberSystemContrast(): ContrastLevel {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return ContrastLevel.Standard
    val context = LocalContext.current
    val uiModeManager = remember(context) { context.getSystemService(UiModeManager::class.java) }
        ?: return ContrastLevel.Standard
    var contrast by remember(uiModeManager) { mutableFloatStateOf(uiModeManager.contrast) }
    DisposableEffect(uiModeManager) {
        val listener = UiModeManager.ContrastChangeListener { contrast = it }
        uiModeManager.addContrastChangeListener(ContextCompat.getMainExecutor(context), listener)
        onDispose { uiModeManager.removeContrastChangeListener(listener) }
    }
    return contrastLevelOf(contrast)
}

/** Faixas do Material: a partir de 2/3 é alto; de 1/3, médio. */
fun contrastLevelOf(systemContrast: Float): ContrastLevel = when {
    systemContrast >= 2f / 3f -> ContrastLevel.High
    systemContrast >= 1f / 3f -> ContrastLevel.Medium
    else -> ContrastLevel.Standard
}
