package com.lifeforge.presentation.screen.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.presentation.common.EnumDropdown
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.LoadingIndicator
import com.lifeforge.presentation.common.LoadingOverlay
import com.lifeforge.presentation.common.formatDate
import com.lifeforge.presentation.common.label
import java.time.Instant

/**
 * Tela de criação/edição de meta — reusa o mesmo ViewModel para ambos
 * os modos. Cabeçalho muda dinamicamente entre "Nova meta" e
 * "Editar meta".
 *
 * O `DatePickerDialog` do Material 3 retorna milissegundos UTC. Convertemos
 * para [Instant] na hora de salvar no estado.
 *
 * Em sucesso, o ViewModel emite `SavedAndNavigateBack` via Channel e a
 * tela popa a back stack — UX padrão de "voltar para a lista após
 * salvar".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoalEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventsFlow.collect { event ->
            when (event) {
                GoalEditEvent.SavedAndNavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Editar meta" else "Nova meta") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !state.isSubmitting,
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                LoadingIndicator()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.errorBanner != null) {
                        ErrorBanner(
                            message = state.errorBanner!!,
                            onDismiss = viewModel::onErrorBannerDismiss,
                        )
                    }

                    LifeForgeTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = "Nome da meta",
                        error = state.nameError,
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                        enabled = !state.isSubmitting,
                    )

                    EnumDropdown(
                        label = "Categoria",
                        options = GoalCategory.entries,
                        selected = state.category,
                        onSelect = viewModel::onCategoryChange,
                        labelOf = GoalCategory::label,
                        enabled = !state.isSubmitting,
                    )

                    OutlinedTextField(
                        value = state.targetAmountInput,
                        onValueChange = viewModel::onTargetAmountChange,
                        label = { Text("Valor alvo (R$)") },
                        isError = state.targetAmountError != null,
                        supportingText = if (state.targetAmountError != null) {
                            { Text(state.targetAmountError!!) }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                        singleLine = true,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    DateField(
                        label = "Data alvo",
                        date = state.targetDate,
                        error = state.targetDateError,
                        onClick = { showDatePicker = true },
                        enabled = !state.isSubmitting,
                    )

                    PrioritySlider(
                        value = state.priority,
                        onValueChange = viewModel::onPriorityChange,
                        enabled = !state.isSubmitting,
                    )

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = viewModel::submit,
                        enabled = state.canSubmit,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.isEdit) "Salvar alterações" else "Criar meta")
                    }
                }
            }

            LoadingOverlay(visible = state.isSubmitting)
        }

        if (showDatePicker) {
            DatePickerDialogContent(
                initial = state.targetDate,
                onSelect = { instant ->
                    showDatePicker = false
                    viewModel.onTargetDateChange(instant)
                },
                onDismiss = { showDatePicker = false },
            )
        }
    }
}

@Composable
private fun DateField(
    label: String,
    date: Instant?,
    error: String?,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    // Texto exibido — vazio se ainda não selecionado.
    OutlinedTextField(
        value = date?.let(::formatDate) ?: "",
        onValueChange = { /* readOnly via clique */ },
        readOnly = true,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else null,
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
private fun DatePickerDialogContent(
    initial: Instant?,
    onSelect: (Instant) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.toEpochMilli()
            ?: System.currentTimeMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onSelect(Instant.ofEpochMilli(millis))
                    }
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun PrioritySlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    enabled: Boolean,
) {
    Column {
        Text(
            "Prioridade: $value",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "1 = baixa, 10 = alta",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt().coerceIn(1, 10)) },
            valueRange = 1f..10f,
            steps = 8,  // 10 valores discretos -> 8 steps internos
            enabled = enabled,
        )
    }
}
