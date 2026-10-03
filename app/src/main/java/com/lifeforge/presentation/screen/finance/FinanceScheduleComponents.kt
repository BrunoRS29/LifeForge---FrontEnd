package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifeforge.domain.model.RecurrenceCalculator
import com.lifeforge.domain.model.RecurrenceType
import com.lifeforge.presentation.common.ConnectedChoice
import com.lifeforge.presentation.common.DateField
import com.lifeforge.presentation.common.DatePickerDialogField
import com.lifeforge.presentation.common.LoadingOverlay
import com.lifeforge.presentation.common.ToggleRow
import com.lifeforge.presentation.common.formatDate
import com.lifeforge.presentation.common.label
import java.time.Instant

/*
 * Componentes compartilhados pelos formulários de Receitas, Despesas e Ativos:
 * a folha inferior com título e ações e os campos de recorrência (mensal ou parcelada) com a prévia de quantos lançamentos serão
 * gerados.
 */

/** Qual seletor de data do formulário de recorrência está aberto. */
enum class SchedulePickerTarget { START, END }

/**
 * Folha inferior de formulário: título, campos e as ações no fim — "Excluir"
 * (só na edição, com confirmação), "Cancelar" e a ação principal.
 */
@Composable
fun FinanceFormSheet(
    title: String,
    isSubmitting: Boolean,
    canSubmit: Boolean,
    submitLabel: String,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    deleteNoun: String,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                content()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onDelete != null) {
                        TextButton(
                            onClick = { confirmDelete = true },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Excluir")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancelar") }
                    Button(
                        onClick = onSubmit,
                        enabled = !isSubmitting && canSubmit,
                        shapes = ButtonDefaults.shapes(),
                    ) { Text(submitLabel) }
                }
            }
            LoadingOverlay(visible = isSubmitting)
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Excluir $deleteNoun?") },
            text = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

/**
 * Data do lançamento ou, quando recorrente, a repetição (mensal ou parcelada),
 * início, fim ou número de parcelas e a prévia do que será gerado. Comum aos
 * formulários de receita e de despesa.
 */
@Composable
fun ScheduleFields(
    isEditing: Boolean,
    isRecurrent: Boolean,
    onIsRecurrentChange: (Boolean) -> Unit,
    recurrentSubtitle: String,
    recurring: Boolean,
    onRecurringChange: (Boolean) -> Unit,
    recurringTitle: String,
    recurrenceType: RecurrenceType,
    onRecurrenceTypeChange: (RecurrenceType) -> Unit,
    startDate: Instant,
    onStartDateChange: (Instant) -> Unit,
    endDate: Instant?,
    onEndDateChange: (Instant?) -> Unit,
    installmentsInput: String,
    installmentsValid: Boolean,
    onInstallmentsChange: (String) -> Unit,
    noun: String,
    enabled: Boolean,
) {
    var activePicker by remember { mutableStateOf<SchedulePickerTarget?>(null) }

    // Recorrência só faz sentido ao CRIAR (não ao editar um lançamento).
    if (!isEditing) {
        ToggleRow(
            title = "Repetir",
            subtitle = recurrentSubtitle,
            checked = isRecurrent,
            onCheckedChange = onIsRecurrentChange,
            enabled = enabled,
        )
    }

    if (!isRecurrent) {
        // Lançamento único (ou edição): data de ocorrência + flag do painel.
        DateField(
            label = "Data",
            date = startDate,
            onClick = { activePicker = SchedulePickerTarget.START },
            enabled = enabled,
        )
        ToggleRow(
            title = recurringTitle,
            subtitle = "Entra na taxa de poupança do painel.",
            checked = recurring,
            onCheckedChange = onRecurringChange,
            enabled = enabled,
        )
    } else {
        ConnectedChoice(
            options = listOf(RecurrenceType.MONTHLY, RecurrenceType.INSTALLMENTS),
            selected = recurrenceType,
            onSelect = onRecurrenceTypeChange,
            label = RecurrenceType::label,
            enabled = enabled,
        )
        DateField(
            label = "Início",
            date = startDate,
            onClick = { activePicker = SchedulePickerTarget.START },
            enabled = enabled,
        )
        if (recurrenceType == RecurrenceType.MONTHLY) {
            DateField(
                label = "Fim (opcional)",
                date = endDate,
                onClick = { activePicker = SchedulePickerTarget.END },
                enabled = enabled,
                placeholder = "Sem data final (próximos 12 meses)",
            )
            if (endDate != null) {
                TextButton(onClick = { onEndDateChange(null) }, enabled = enabled) { Text("Remover data final") }
            }
        }
        if (recurrenceType == RecurrenceType.INSTALLMENTS) {
            OutlinedTextField(
                value = installmentsInput,
                onValueChange = onInstallmentsChange,
                label = { Text("Número de parcelas") },
                isError = !installmentsValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SchedulePreview(
            recurrenceType = recurrenceType,
            startDate = startDate,
            endDate = endDate,
            installmentsInput = installmentsInput,
            noun = noun,
        )
    }

    when (activePicker) {
        SchedulePickerTarget.START -> DatePickerDialogField(
            initial = startDate,
            onSelect = { onStartDateChange(it); activePicker = null },
            onDismiss = { activePicker = null },
        )
        SchedulePickerTarget.END -> DatePickerDialogField(
            initial = endDate ?: startDate,
            onSelect = { onEndDateChange(it); activePicker = null },
            onDismiss = { activePicker = null },
        )
        null -> Unit
    }
}

/**
 * Prévia local: usa o MESMO [RecurrenceCalculator] do backend para mostrar
 * quantos registros a recorrência vai gerar, antes de enviar. Manter a regra
 * idêntica evita prévia enganosa.
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
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.EventRepeat, contentDescription = null)
            Text(schedulePreviewText(recurrenceType, startDate, endDate, count, noun), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** "Isso vai gerar 12 despesas a partir de 02/10/2026 até 02/09/2027." */
internal fun schedulePreviewText(
    recurrenceType: RecurrenceType,
    startDate: Instant,
    endDate: Instant?,
    count: Int,
    noun: String,
): String {
    val plural = if (count == 1) noun else "${noun}s"
    val endText = when {
        recurrenceType == RecurrenceType.INSTALLMENTS -> null
        endDate != null -> formatDate(endDate)
        else -> "indefinido"
    }
    return buildString {
        append("Isso vai gerar $count $plural a partir de ${formatDate(startDate)}")
        if (endText != null) append(" até $endText")
        append(".")
    }
}
