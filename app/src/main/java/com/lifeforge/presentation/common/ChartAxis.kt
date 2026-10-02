package com.lifeforge.presentation.common

import androidx.compose.runtime.Composable
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/*
 * Eixos legíveis para os gráficos do Vico: valores em R$ compactos, gráfico
 * inteiro na largura da tela e rótulos de meses espaçados conforme o horizonte.
 * Sem isto o eixo Y mostrava "7800000" e um horizonte de 27 anos virava uma
 * rolagem horizontal de centenas de pontos.
 */

/** Rótulo curto para eixo em R$: 7.800.000 → "R$ 7,8 mi"; 850.000 → "R$ 850 mil"; 0 → "R$ 0". */
fun formatAxisBrl(value: Double): String {
    val magnitude = abs(value)
    val sign = if (value < 0 && magnitude >= 0.5) "-" else ""
    val (scaled, suffix) = when {
        magnitude >= 1e9 -> magnitude / 1e9 to " bi"
        magnitude >= 1e6 -> magnitude / 1e6 to " mi"
        magnitude >= 1e3 -> magnitude / 1e3 to " mil"
        else -> magnitude to ""
    }
    val number = NumberFormat.getNumberInstance(Locale("pt", "BR")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = if (scaled < 100) 1 else 0
    }.format(scaled)
    return "${sign}R$ $number$suffix"
}

/** Formatter do Vico para eixos verticais em R$. */
val BrlAxisFormatter: CartesianValueFormatter = CartesianValueFormatter { _, value, _ -> formatAxisBrl(value) }

/**
 * Espaçamento, em meses, entre os rótulos de um eixo de meses: até 7 rótulos
 * (4 a 7 a partir de 1 ano e meio) qualquer que seja o horizonte.
 */
fun monthAxisSpacing(totalMonths: Int): Int = when {
    totalMonths <= 18 -> 3
    totalMonths <= 36 -> 6
    totalMonths <= 72 -> 12
    totalMonths <= 144 -> 24
    totalMonths <= 216 -> 36
    totalMonths <= 360 -> 60
    else -> 120
}

/**
 * Rótulos a cada [spacing] meses. [offset] conta a partir do menor x do
 * gráfico: com 12 meses de passado (x de −12 a 0), offset 12 alinha os rótulos
 * ao mês 0 (hoje).
 */
fun monthItemPlacer(spacing: Int, offset: Int = 0): HorizontalAxis.ItemPlacer =
    HorizontalAxis.ItemPlacer.aligned(spacing = { spacing }, offset = { offset })

/** Gráfico inteiro na largura disponível, sem rolagem nem zoom por gesto. */
@Composable
fun rememberFitToWidthZoom(): VicoZoomState =
    rememberVicoZoomState(
        zoomEnabled = false,
        initialZoom = Zoom.Content,
        minZoom = Zoom.Content,
        maxZoom = Zoom.Content,
    )
