package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.Expense
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.RecurrenceType
import com.lifeforge.presentation.common.CurrencyField
import com.lifeforge.presentation.common.DateField
import com.lifeforge.presentation.common.DatePickerDialogField
import com.lifeforge.presentation.common.EnumDropdown
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.LoadingOverlay
import com.lifeforge.presentation.common.firstInstantOfMonth
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatDate
import com.lifeforge.presentation.common.formatMonthYear
import com.lifeforge.presentation.common.label
import com.lifeforge.presentation.common.yearMonthOf
import java.math.BigDecimal
import java.time.Instant

/**
 * Sub-aba de Despesas. Espelha [IncomeTab]: navegação por mês, data e
 * editar/excluir nos cards, e form que cria (único/recorrente) ou edita.
 */
@Composable
fun ExpenseTab(viewModel: ExpenseViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var selectedMonth by remember { mutableStateOf(yearMonthOf(Instant.now())) }

    val monthExpenses = remember(state.expenses, selectedMonth) {
        state.expenses
            .filter { yearMonthOf(it.spentAt) == selectedMonth }
            .sortedByDescending { it.spentAt }
    }
    val monthTotal = remember(monthExpenses) {
        monthExpenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }
    }

    FinanceListScaffold(
        isRefreshing = state.isRefreshing,
        errorBanner = state.errorBanner,
        onErrorDismiss = viewModel::onErrorBannerDismiss,
        onRefresh = viewModel::refresh,
        onAddClick = {
            val now = Instant.now()
            val default = if (selectedMonth == yearMonthOf(now)) now else firstInstantOfMonth(selectedMonth)
            viewModel.openForm(defaultDate = default)
        },
        addLabel = "Nova despesa",
        isEmpty = state.expenses.isEmpty(),
        emptyTitle = "Sem despesas cadastradas",
        emptyDescription = "Acompanhar despesas recorrentes ajuda a calcular sua taxa de poupança real.",
        emptyIcon = Icons.Outlined.TrendingDown,
        header = {
            MonthNavigator(
                monthLabel = formatMonthYear(selectedMonth),
                total = monthTotal,
                onPrev = { selectedMonth = selectedMonth.minusMonths(1) },
                onNext = { selectedMonth = selectedMonth.plusMonths(1) },
            )
        },
    ) {
        if (monthExpenses.isEmpty()) {
            item {
                Text(
                    "Nenhuma despesa em ${formatMonthYear(selectedMonth)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                )
            }
        } else {
            items(items = monthExpenses, key = { it.id }) { expense ->
                ExpenseCard(
                    expense = expense,
                    onEdit = { viewModel.openEditForm(expense) },
                    onDelete = { viewModel.delete(expense.id) },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }

    if (state.form != null) {
        ExpenseFormSheet(
            form = state.form!!,
            isSubmitting = state.isSubmitting,
            onDescriptionChange = viewModel::onFormDescriptionChange,
            onAmountChange = viewModel::onFormAmountChange,
            onCategoryChange = viewModel::onFormCategoryChange,
            onRecurringChange = viewModel::onFormRecurringChange,
            onIsRecurrentChange = viewModel::onFormIsRecurrentChange,
            onRecurrenceTypeChange = viewModel::onFormRecurrenceTypeChange,
            onStartDateChange = viewModel::onFormStartDateChange,
            onEndDateChange = viewModel::onFormEndDateChange,
            onInstallmentsChange = viewModel::onFormInstallmentsChange,
            onSubmit = viewModel::submitForm,
            onDismiss = viewModel::closeForm,
        )
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, style = MaterialTheme.typography.titleMedium)
                Text(
                    expense.category.label() + if (expense.recurring) " · Recorrente" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Gasto em ${formatDate(expense.spentAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatBrl(expense.amount),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Editar despesa")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Apagar",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFormSheet(
    form: ExpenseFormState,
    isSubmitting: Boolean,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (ExpenseCategory) -> Unit,
    onRecurringChange: (Boolean) -> Unit,
    onIsRecurrentChange: (Boolean) -> Unit,
    onRecurrenceTypeChange: (RecurrenceType) -> Unit,
    onStartDateChange: (Instant) -> Unit,
    onEndDateChange: (Instant?) -> Unit,
    onInstallmentsChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activePicker by remember { mutableStateOf<SchedulePickerTarget?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (form.isEditing) "Editar despesa" else "Nova despesa",
                style = MaterialTheme.typography.titleLarge,
            )

            LifeForgeTextField(
                value = form.description,
                onValueChange = onDescriptionChange,
                label = "Descrição (ex.: Aluguel apartamento)",
                error = form.descriptionError,
                imeAction = ImeAction.Next,
                enabled = !isSubmitting,
            )
            CurrencyField(
                value = form.amountInput,
                onValueChange = onAmountChange,
                label = if (form.isRecurrent) "Valor por ocorrência (R$)" else "Valor (R$)",
                error = form.amountError,
                enabled = !isSubmitting,
            )
            EnumDropdown(
                label = "Categoria",
                options = ExpenseCategory.entries,
                selected = form.category,
                onSelect = onCategoryChange,
                labelOf = ExpenseCategory::label,
                enabled = !isSubmitting,
            )

            if (!form.isEditing) {
                ScheduleToggleRow(
                    title = "Recorrente?",
                    subtitle = "Gera os lançamentos mensais (ou parcelas) automaticamente.",
                    checked = form.isRecurrent,
                    onCheckedChange = onIsRecurrentChange,
                    enabled = !isSubmitting,
                )
            }

            if (!form.isRecurrent) {
                DateField(
                    label = "Data",
                    date = form.startDate,
                    onClick = { activePicker = SchedulePickerTarget.START },
                    enabled = !isSubmitting,
                )
                ScheduleToggleRow(
                    title = "Conta na despesa mensal",
                    subtitle = "Entra na taxa de poupança do dashboard.",
                    checked = form.recurring,
                    onCheckedChange = onRecurringChange,
                    enabled = !isSubmitting,
                )
            } else {
                EnumDropdown(
                    label = "Repetição",
                    options = listOf(RecurrenceType.MONTHLY, RecurrenceType.INSTALLMENTS),
                    selected = form.recurrenceType,
                    onSelect = onRecurrenceTypeChange,
                    labelOf = RecurrenceType::label,
                    enabled = !isSubmitting,
                )
                DateField(
                    label = "Início",
                    date = form.startDate,
                    onClick = { activePicker = SchedulePickerTarget.START },
                    enabled = !isSubmitting,
                )
                if (form.recurrenceType == RecurrenceType.MONTHLY) {
                    DateField(
                        label = "Fim (opcional)",
                        date = form.endDate,
                        onClick = { activePicker = SchedulePickerTarget.END },
                        enabled = !isSubmitting,
                        placeholder = "Indefinido (+12 meses)",
                    )
                    if (form.endDate != null) {
                        TextButton(
                            onClick = { onEndDateChange(null) },
                            enabled = !isSubmitting,
                        ) { Text("Limpar data final") }
                    }
                }
                if (form.recurrenceType == RecurrenceType.INSTALLMENTS) {
                    OutlinedTextField(
                        value = form.installmentsInput,
                        onValueChange = onInstallmentsChange,
                        label = { Text("Número de parcelas") },
                        isError = !form.installmentsValid,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        singleLine = true,
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SchedulePreview(
                    recurrenceType = form.recurrenceType,
                    startDate = form.startDate,
                    endDate = form.endDate,
                    installmentsInput = form.installmentsInput,
                    noun = "despesa",
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onSubmit,
                    enabled = !isSubmitting && form.canSubmit,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            form.isEditing -> "Salvar"
                            form.isRecurrent -> "Criar recorrência"
                            else -> "Adicionar"
                        },
                    )
                }
            }
        }
        LoadingOverlay(visible = isSubmitting)
    }

    when (activePicker) {
        SchedulePickerTarget.START -> DatePickerDialogField(
            initial = form.startDate,
            onSelect = { onStartDateChange(it); activePicker = null },
            onDismiss = { activePicker = null },
        )
        SchedulePickerTarget.END -> DatePickerDialogField(
            initial = form.endDate ?: form.startDate,
            onSelect = { onEndDateChange(it); activePicker = null },
            onDismiss = { activePicker = null },
        )
        null -> Unit
    }
}
