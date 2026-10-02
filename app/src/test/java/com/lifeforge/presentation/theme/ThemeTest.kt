package com.lifeforge.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Escolha do esquema de cor pela preferência de contraste e acessibilidade dos esquemas. */
class ThemeTest {

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return (maxOf(la, lb) / minOf(la, lb)).toDouble()
    }

    @Test
    fun `faixas de contraste do sistema seguem o Material`() {
        assertThat(contrastLevelOf(0f)).isEqualTo(ContrastLevel.Standard)
        assertThat(contrastLevelOf(0.32f)).isEqualTo(ContrastLevel.Standard)
        assertThat(contrastLevelOf(0.5f)).isEqualTo(ContrastLevel.Medium)
        assertThat(contrastLevelOf(1f)).isEqualTo(ContrastLevel.High)
        assertThat(contrastLevelOf(-1f)).isEqualTo(ContrastLevel.Standard)
    }

    @Test
    fun `texto secundario fica mais contrastado a cada nivel`() {
        for (dark in listOf(false, true)) {
            val ratios = ContrastLevel.entries.map { level ->
                val scheme = brandColorScheme(dark, level)
                contrast(scheme.onSurfaceVariant, scheme.surfaceContainerHighest)
            }
            assertThat(ratios).isInStrictOrder()
        }
    }

    @Test
    fun `pares de texto do esquema padrao passam de 4,5 para 1`() {
        for (dark in listOf(false, true)) {
            val s = brandColorScheme(dark, ContrastLevel.Standard)
            listOf(
                s.onSurface to s.surface,
                s.onSurfaceVariant to s.surfaceContainerHighest,
                s.primary to s.surface,
                s.secondary to s.surface,
                s.onPrimary to s.primary,
                s.onPrimaryContainer to s.primaryContainer,
                s.onSecondaryContainer to s.secondaryContainer,
                s.onErrorContainer to s.errorContainer,
            ).forEach { (text, background) ->
                assertThat(contrast(text, background)).isAtLeast(4.5)
            }
        }
    }

    @Test
    fun `superficies em camadas usam o neutro da marca e nao o lilas do baseline`() {
        val light = brandColorScheme(darkTheme = false, contrast = ContrastLevel.Standard)
        // Baseline lilás do Material: surfaceContainer = #F3EDF7.
        assertThat(light.surfaceContainer).isNotEqualTo(Color(0xFFF3EDF7))
        assertThat(light.surfaceContainer.green).isAtLeast(light.surfaceContainer.red)
    }
}
