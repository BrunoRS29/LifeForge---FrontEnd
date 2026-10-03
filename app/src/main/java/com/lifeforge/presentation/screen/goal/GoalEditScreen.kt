package com.lifeforge.presentation.screen.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.presentation.common.ActionButton
import com.lifeforge.presentation.common.BottomActionBar
import com.lifeforge.presentation.common.DateField
import com.lifeforge.presentation.common.DatePickerDialogField
import com.lifeforge.presentation.common.DetailTopAppBar
import com.lifeforge.presentation.common.EnumDropdown
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.FormMaxWidth
import com.lifeforge.presentation.common.FormSection
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.LoadingOverlay
import com.lifeforge.presentation.common.MoneyField
import com.lifeforge.presentation.common.ScreenLoading
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.icon
import com.lifeforge.presentation.common.label
import com.lifeforge.presentation.common.readableWidth
import kotlin.math.roundToInt

/**
 * Tela de criação/edição de meta — reusa o mesmo ViewModel para ambos
 * os modos; o título muda entre "Nova meta" e "Editar meta".
 *
 * Campos agrupados por assunto (sobre a meta, valor e prazo, prioridade) e o
 * botão de salvar fixo na base, acima do teclado: dá para salvar sem rolar.
 * O `DatePickerDialog` do Material 3 retorna milissegundos UTC, convertidos
 * para `Instant` ao salvar no estado.
 *
 * Em sucesso, o ViewModel emite `SavedAndNavigateBack` via Channel e a
 * tela volta para a lista.
 */
@Composable
fun GoalEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoalEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.eventsFlow.collect { event ->
            when (event) {
                GoalEditEvent.SavedAndNavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopAppBar(
                title = if (state.isEdit) "Editar meta" else "Nova meta",
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
                navigationEnabled = !state.isSubmitting,
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                BottomActionBar {
                    ActionButton(
                        text = if (state.isEdit) "Salvar alterações" else "Criar meta",
                        icon = Icons.Rounded.Check,
                        onClick = viewModel::submit,
                        enabled = state.canSubmit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                ScreenLoading()
            } else {
                GoalForm(
                    state = state,
                    onNameChange = viewModel::onNameChange,
                    onCategoryChange = viewModel::onCategoryChange,
                    onTargetAmountChange = viewModel::onTargetAmountChange,
                    onPickDate = { showDatePicker = true },
                    onPriorityChange = viewModel::onPriorityChange,
                    onErrorDismiss = viewModel::onErrorBannerDismiss,
                )
            }
            LoadingOverlay(visible = state.isSubmitting)
        }

        if (showDatePicker) {
            DatePickerDialogField(
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
private fun GoalForm(
    state: GoalEditUiState,
    onNameChange: (String) -> Unit,
    onCategoryChange: (GoalCategory) -> Unit,
    onTargetAmountChange: (String) -> Unit,
    onPickDate: () -> Unit,
    onPriorityChange: (Int) -> Unit,
    onErrorDismiss: () -> Unit,
) {
    val enabled = !state.isSubmitting
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .readableWidth(FormMaxWidth)
            .padding(horizontal = ScreenPadding)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        if (state.errorBanner != null) {
            ErrorBanner(message = state.errorBanner, onDismiss = onErrorDismiss)
        }

        FormSection(title = "Sobre a meta") {
            LifeForgeTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = "Nome da meta",
                error = state.nameError,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
                enabled = enabled,
            )
            EnumDropdown(
                label = "Categoria",
                options = GoalCategory.entries,
                selected = state.category,
                onSelect = onCategoryChange,
                labelOf = GoalCategory::label,
                iconOf = GoalCategory::icon,
                enabled = enabled,
            )
        }

        FormSection(title = "Valor e prazo", supporting = "Quanto você quer acumular e até quando.") {
            MoneyField(
                value = state.targetAmountInput,
                onValueChange = onTargetAmountChange,
                label = "Valor alvo",
                error = state.targetAmountError,
                imeAction = ImeAction.Done,
                enabled = enabled,
            )
            DateField(
                label = "Data alvo",
                date = state.targetDate,
                error = state.targetDateError,
                onClick = onPickDate,
                enabled = enabled,
            )
        }

        FormSection(
            title = "Prioridade",
            supporting = "De 1 (baixa) a 10 (alta).",
            action = { PriorityBadge(value = state.priority) },
        ) {
            PrioritySlider(value = state.priority, onValueChange = onPriorityChange, enabled = enabled)
        }
    }
}

@Composable
private fun PriorityBadge(value: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            "$value de 10",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PrioritySlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    enabled: Boolean,
) {
    Column {
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(1, 10)) },
            valueRange = 1f..10f,
            steps = 8, // 10 valores discretos -> 8 paradas internas
            enabled = enabled,
            modifier = Modifier.semantics { stateDescription = "Prioridade $value de 10" },
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Baixa",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Alta",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
