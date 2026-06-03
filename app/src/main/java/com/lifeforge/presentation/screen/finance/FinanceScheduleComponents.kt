package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lifeforge.domain.model.RecurrenceCalculator
import com.lifeforge.domain.model.RecurrenceType
import com.lifeforge.presentation.common.formatDate
import java.time.Instant

/**
 * Componentes de UI compartilhados entre [IncomeTab] e [ExpenseTab] para o
 * fluxo de recorrência (Sprint 6), evitando duplicação entre as duas abas.
 */

/** Qual date picker do form de schedule está aberto. */
enum class SchedulePickerTarget { START, END }

@Composable
fun ScheduleToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/**
 * Preview local: usa o MESMO [RecurrenceCalculator] do backend para mostrar
 * quantos registros o schedule vai gerar, antes de enviar. Manter a regra
 * idêntica evita preview enganoso.
 */
@Composable
fun SchedulePreview(
    recurrenceType: RecurrenceType,
    startDate: Instant,
    endDate: Instant?,
    installmentsInput: String,
    noun: String,
) {
    val count = remember(recurrenceType, startDate, endDate, installmentsInput) {
        RecurrenceCalculator.count(
            recurrence = recurrenceType,
            startDate = startDate,
            endDate = endDate,
            installmentsTotal = installmentsInput.toIntOrNull(),
        )
    }
    val plural = if (count == 1) noun else "${noun}s"
    val endText = when {
        recurrenceType == RecurrenceType.INSTALLMENTS -> null
        endDate != null -> formatDate(endDate)
        else -> "indefinido"
    }
    Text(
        buildString {
            append("Isso vai gerar $count $plural a partir de ${formatDate(startDate)}")
            if (endText != null) append(" até $endText")
            append(".")
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
