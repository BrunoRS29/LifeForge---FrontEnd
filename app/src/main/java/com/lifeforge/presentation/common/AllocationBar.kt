package com.lifeforge.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Uma fatia de uma alocação: rótulo e participação (0..1). */
data class AllocationShare(val label: String, val share: Double)

/**
 * Barra de alocação: cada fatia ocupa a largura proporcional à sua
 * participação, com a legenda (rótulo + porcentagem) logo abaixo — a cor nunca
 * é a única pista. Usada na carteira atual (aba Ativos) e na sugerida pela
 * otimização.
 */
@Composable
fun AllocationBar(shares: List<AllocationShare>, modifier: Modifier = Modifier) {
    if (shares.isEmpty()) return
    val colors = allocationPalette()
    val sliceColors = shares.indices.map { colors[it % colors.size] }
    val description = "Alocação: " + shares.joinToString { "${it.label} ${formatAnnualRate(it.share)}" }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .semantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            shares.forEachIndexed { index, slice ->
                Box(
                    Modifier
                        .weight(slice.share.toFloat().coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(sliceColors[index]),
                )
            }
        }
        ChartLegend(
            entries = shares.mapIndexed { index, slice -> "${slice.label} ${formatAnnualRate(slice.share)}" to sliceColors[index] },
        )
    }
}

/** Cores das fatias, na ordem: as cores de destaque do tema e variações delas. */
@Composable
private fun allocationPalette(): List<Color> {
    val colors = MaterialTheme.colorScheme
    return listOf(
        colors.primary,
        colors.tertiary,
        colors.secondary,
        colors.primary.copy(alpha = 0.5f),
        colors.tertiary.copy(alpha = 0.5f),
        colors.outline,
    )
}
