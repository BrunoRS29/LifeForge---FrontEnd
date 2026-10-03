package com.lifeforge.presentation.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import java.time.Instant

/**
 * Campo de data somente leitura que abre o seletor de data ao toque — no campo
 * inteiro, não só no ícone (alvo de toque maior). Compartilhado pelos formulários
 * de metas e de lançamentos recorrentes.
 */
@Composable
fun DateField(
    label: String,
    date: Instant?,
    onClick: () -> Unit,
    enabled: Boolean = true,
    placeholder: String = "",
    error: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release && currentEnabled) currentOnClick()
        }
    }
    OutlinedTextField(
        value = date?.let(::formatDate) ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        trailingIcon = {
            IconButton(onClick = onClick, enabled = enabled) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Selecionar data")
            }
        },
        enabled = enabled,
        singleLine = true,
        interactionSource = interactionSource,
        modifier = modifier,
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
        initialSelectedDateMillis = instantToPickerMillis(initial ?: Instant.now()),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onSelect(pickerMillisToInstant(it)) }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DatePicker(state = pickerState)
    }
}
