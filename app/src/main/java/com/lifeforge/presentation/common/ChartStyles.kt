package com.lifeforge.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.layer.continuous
import com.patrykandpatrick.vico.compose.cartesian.layer.dashed
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider

/*
 * Estilos de linha dos gráficos (Vico), com as cores do tema. Cada série recebe
 * um papel visual explícito — em vez de depender da ordem das cores do tema —
 * para que a legenda e o gráfico nunca divirjam quando uma série some.
 */

/** Linha cheia, de pontas arredondadas: a série principal (ex.: o realizado). */
@Composable
fun rememberSolidLine(color: Color, thickness: Dp = 3.dp): LineCartesianLayer.Line =
    LineCartesianLayer.rememberLine(
        fill = remember(color) { LineCartesianLayer.LineFill.single(fill(color)) },
        stroke = LineCartesianLayer.LineStroke.continuous(thickness = thickness, cap = StrokeCap.Round),
    )

/** Linha com área em degradê até o eixo: destaca a série de interesse (ex.: a projeção). */
@Composable
fun rememberAreaLine(color: Color, thickness: Dp = 3.dp): LineCartesianLayer.Line {
    val areaFill = remember(color) {
        LineCartesianLayer.AreaFill.single(
            fill(
                ShaderProvider.verticalGradient(
                    intArrayOf(color.copy(alpha = 0.28f).toArgb(), color.copy(alpha = 0f).toArgb()),
                ),
            ),
        )
    }
    return LineCartesianLayer.rememberLine(
        fill = remember(color) { LineCartesianLayer.LineFill.single(fill(color)) },
        stroke = LineCartesianLayer.LineStroke.continuous(thickness = thickness, cap = StrokeCap.Round),
        areaFill = areaFill,
    )
}

/** Linha tracejada: referência ou linha de base (ex.: só os aportes, sem rendimento). */
@Composable
fun rememberDashedLine(color: Color, thickness: Dp = 2.dp): LineCartesianLayer.Line =
    LineCartesianLayer.rememberLine(
        fill = remember(color) { LineCartesianLayer.LineFill.single(fill(color)) },
        stroke = LineCartesianLayer.LineStroke.dashed(
            thickness = thickness,
            cap = StrokeCap.Round,
            dashLength = 6.dp,
            gapLength = 5.dp,
        ),
    )
