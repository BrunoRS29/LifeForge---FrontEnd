package com.lifeforge.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme

/**
 * Tema raiz do LifeForge. Envolve toda a hierarquia de Compose para
 * que [MaterialTheme.colorScheme]/`typography`/`shapes` resolvam para
 * os tokens da marca.
 *
 * - `dynamicColor`: Material You (Android 12+) sobrepõe a paleta com
 *   cores derivadas do wallpaper. Para um app de TCC com identidade
 *   visual definida pelo orientador, deixamos **desligado por padrão**
 *   — assim a aparência fica idêntica em qualquer dispositivo da
 *   apresentação. Se quiser ativar pra fins de demo, basta passar `true`.
 * - `darkTheme`: por padrão segue o sistema. Para forçar um tema
 *   específico durante screenshots ou apresentação, passe explicitamente.
 */
private val LifeForgeLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
)

private val LifeForgeDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
)

/**
 * Cantos mais arredondados que o baseline M3 — visual mais suave e atual.
 * `medium` rege os Cards, `extraSmall` os campos de texto e menus.
 */
private val LifeForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun LifeForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LifeForgeDarkColorScheme
        else -> LifeForgeLightColorScheme
    }

    // Edge-to-edge: a barra de status fica TRANSPARENTE (enableEdgeToEdge na
    // MainActivity) e o conteúdo desenha atrás dela — pintar a barra com
    // statusBarColor é um anti-padrão (e deprecated no Android 15). Aqui só
    // ajustamos o CONTRASTE dos ícones, pois o tema do app pode divergir do
    // tema do sistema (preferência in-app SYSTEM/LIGHT/DARK).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LifeForgeTypography,
        shapes = LifeForgeShapes,
    ) {
        // Os gráficos (Vico) NÃO seguem o MaterialTheme automaticamente: sem
        // isto, eixos e legendas usam o tema do SISTEMA — forçando o tema
        // escuro no app com o sistema claro, os rótulos ficavam pretos sobre
        // fundo escuro. O tema M3 do Vico amarra os gráficos ao colorScheme.
        ProvideVicoTheme(rememberM3VicoTheme(), content)
    }
}
