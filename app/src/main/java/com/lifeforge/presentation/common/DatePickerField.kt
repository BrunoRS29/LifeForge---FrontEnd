package com.lifeforge.presentation.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.time.Instant

/**
 * Campo de data readonly que dispara um DatePicker ao toque do ícone.
 * Compartilhado pelas telas de finanças (schedules recorrentes) para não
 * duplicar o boilerplate do Material3 DatePicker.
 */
@Composable
fun DateField(
    label: String,
    date: Instant?,
    onClick: () -> Unit,
    enabled: Boolean = true,
    placeholder: String = "",
) {
    OutlinedTextField(
        value = date?.let(::formatDate) ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        trailingIcon = {
            IconButton(onClick = onClick, enabled = enabled) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Selecionar data")
            }
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogField(
    initial: Instant?,
    onSelect: (Instant) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.toEpochMilli() ?: System.currentTimeMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onSelect(Instant.ofEpochMilli(it)) }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DatePicker(state = pickerState)
    }
}
