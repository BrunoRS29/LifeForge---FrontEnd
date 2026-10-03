package com.lifeforge.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lifeforge.domain.model.GoalHealth
import com.lifeforge.domain.model.GoalHealthStatus

/**
 * Selo de "saúde" de uma meta, com cor E ícone E texto (a informação não
 * depende só da cor — diretriz de acessibilidade). Mesmo semáforo do gauge
 * da simulação: ≥ 80% no caminho, 50–80% atenção, < 50% em risco.
 */
@Composable
fun GoalHealthChip(health: GoalHealth, modifier: Modifier = Modifier) =
    HealthStatusPill(status = health.status, text = goalHealthLabel(health), modifier = modifier)

/** Pílula com a cor, o ícone e o [text] de um estado de saúde — o mesmo semáforo do selo. */
@Composable
fun HealthStatusPill(status: GoalHealthStatus, text: String, modifier: Modifier = Modifier) {
    val style = healthStyle(status)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(style.container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(style.icon, contentDescription = null, tint = style.content, modifier = Modifier.size(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = style.content,
        )
    }
}

/** Rótulo curto do estado: "No caminho", "Atenção", "Em risco"… */
fun healthStatusLabel(status: GoalHealthStatus): String = when (status) {
    GoalHealthStatus.ON_TRACK -> "No caminho"
    GoalHealthStatus.ATTENTION -> "Atenção"
    GoalHealthStatus.AT_RISK -> "Em risco"
    GoalHealthStatus.NOT_SIMULATED -> "Ainda não simulada"
    GoalHealthStatus.PENDING_SYNC -> "Aguardando sincronização"
}

/** Texto do selo de saúde (separado para teste e para descrições de acessibilidade). */
fun goalHealthLabel(health: GoalHealth): String {
    val label = healthStatusLabel(health.status)
    val probability = health.latest?.successProbability?.let(::formatProbability)
    return when (health.status) {
        GoalHealthStatus.ON_TRACK, GoalHealthStatus.ATTENTION, GoalHealthStatus.AT_RISK -> "$label · $probability"
        GoalHealthStatus.NOT_SIMULATED, GoalHealthStatus.PENDING_SYNC -> label
    }
}

/** "faltam 2 anos e 3 meses" / "faltam 5 meses" / "prazo encerrado". */
fun formatMonthsLeft(months: Long): String {
    if (months <= 0) return "prazo encerrado"
    val years = months / 12
    val rest = months % 12
    val yearsText = when (years) {
        0L -> null
        1L -> "1 ano"
        else -> "$years anos"
    }
    val monthsText = when (rest) {
        0L -> null
        1L -> "1 mês"
        else -> "$rest meses"
    }
    return "faltam " + listOfNotNull(yearsText, monthsText).joinToString(" e ")
}

private data class HealthStyle(val container: Color, val content: Color, val icon: ImageVector)

@Composable
private fun healthStyle(status: GoalHealthStatus): HealthStyle {
    val colors = MaterialTheme.colorScheme
    return when (status) {
        GoalHealthStatus.ON_TRACK ->
            HealthStyle(colors.primaryContainer, colors.onPrimaryContainer, Icons.Outlined.CheckCircle)
        GoalHealthStatus.ATTENTION ->
            HealthStyle(colors.secondaryContainer, colors.onSecondaryContainer, Icons.Outlined.WarningAmber)
        GoalHealthStatus.AT_RISK ->
            HealthStyle(colors.errorContainer, colors.onErrorContainer, Icons.Outlined.ErrorOutline)
        GoalHealthStatus.NOT_SIMULATED ->
            HealthStyle(colors.surfaceVariant, colors.onSurfaceVariant, Icons.AutoMirrored.Outlined.HelpOutline)
        GoalHealthStatus.PENDING_SYNC ->
            HealthStyle(colors.surfaceVariant, colors.onSurfaceVariant, Icons.Outlined.CloudUpload)
    }
}

/** Cor de destaque da saúde (anel de probabilidade, ícones): o mesmo semáforo do selo. */
@Composable
fun goalHealthColor(status: GoalHealthStatus): Color {
    val colors = MaterialTheme.colorScheme
    return when (status) {
        GoalHealthStatus.ON_TRACK -> colors.primary
        GoalHealthStatus.ATTENTION -> colors.secondary
        GoalHealthStatus.AT_RISK -> colors.error
        GoalHealthStatus.NOT_SIMULATED, GoalHealthStatus.PENDING_SYNC -> colors.outline
    }
}
