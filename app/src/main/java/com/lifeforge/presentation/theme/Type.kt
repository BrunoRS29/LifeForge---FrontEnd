package com.lifeforge.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Tipografia LifeForge — a escala do Material 3 (Roboto, a fonte do sistema), com
 * dois ajustes para um app financeiro:
 *
 * - **Algarismos tabulares** (`tnum`) em toda a escala: cada dígito ocupa a mesma
 *   largura, então valores em listas e tabelas ficam alinhados e um número que muda
 *   (contagem, resultado de simulação) não "dança" na tela. Só os dígitos mudam de
 *   largura; as letras seguem iguais.
 * - **Títulos e manchetes** um pouco mais pesados que o baseline, para destacar os
 *   valores proeminentes (patrimônio, probabilidade de sucesso).
 *
 * As variações *Emphasized* do Material 3 Expressive (`headlineLargeEmphasized`,
 * `titleMediumEmphasized`…) ficam disponíveis para os números de destaque.
 * Tamanhos em `sp`, então a preferência de tamanho de fonte do usuário é respeitada.
 */
private val Baseline = Typography()

private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

val LifeForgeTypography = Typography(
    displayLarge = Baseline.displayLarge.tabular(),
    displayMedium = Baseline.displayMedium.tabular(),
    displaySmall = Baseline.displaySmall.tabular(),
    headlineLarge = Baseline.headlineLarge.copy(fontWeight = FontWeight.SemiBold).tabular(),
    headlineMedium = Baseline.headlineMedium.copy(fontWeight = FontWeight.SemiBold).tabular(),
    headlineSmall = Baseline.headlineSmall.copy(fontWeight = FontWeight.SemiBold).tabular(),
    titleLarge = Baseline.titleLarge.copy(fontWeight = FontWeight.Medium).tabular(),
    titleMedium = Baseline.titleMedium.tabular(),
    titleSmall = Baseline.titleSmall.tabular(),
    bodyLarge = Baseline.bodyLarge.tabular(),
    bodyMedium = Baseline.bodyMedium.tabular(),
    bodySmall = Baseline.bodySmall.tabular(),
    labelLarge = Baseline.labelLarge.tabular(),
    labelMedium = Baseline.labelMedium.tabular(),
    labelSmall = Baseline.labelSmall.tabular(),
    displayLargeEmphasized = Baseline.displayLargeEmphasized.tabular(),
    displayMediumEmphasized = Baseline.displayMediumEmphasized.tabular(),
    displaySmallEmphasized = Baseline.displaySmallEmphasized.tabular(),
    headlineLargeEmphasized = Baseline.headlineLargeEmphasized.tabular(),
    headlineMediumEmphasized = Baseline.headlineMediumEmphasized.tabular(),
    headlineSmallEmphasized = Baseline.headlineSmallEmphasized.tabular(),
    titleLargeEmphasized = Baseline.titleLargeEmphasized.tabular(),
    titleMediumEmphasized = Baseline.titleMediumEmphasized.tabular(),
    titleSmallEmphasized = Baseline.titleSmallEmphasized.tabular(),
    bodyLargeEmphasized = Baseline.bodyLargeEmphasized.tabular(),
    bodyMediumEmphasized = Baseline.bodyMediumEmphasized.tabular(),
    bodySmallEmphasized = Baseline.bodySmallEmphasized.tabular(),
    labelLargeEmphasized = Baseline.labelLargeEmphasized.tabular(),
    labelMediumEmphasized = Baseline.labelMediumEmphasized.tabular(),
    labelSmallEmphasized = Baseline.labelSmallEmphasized.tabular(),
)
