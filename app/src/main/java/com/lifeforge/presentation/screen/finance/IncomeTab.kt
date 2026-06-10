package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.Income
import com.lifeforge.domain.model.IncomeType
import com.lifeforge.domain.model.RecurrenceType
import com.lifeforge.presentation.common.CurrencyField
import com.lifeforge.presentation.common.DateField
import com.lifeforge.presentation.common.DatePickerDialogField
import com.lifeforge.presentation.common.EnumDropdown
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.LoadingOverlay
import com.lifeforge.presentation.common.firstInstantOfMonth
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatDayMonth
import com.lifeforge.presentation.common.formatMonthYear
import com.lifeforge.presentation.common.label
import com.lifeforge.presentation.common.yearMonthOf
import java.math.BigDecimal
import java.time.Instant

/**
 * Sub-aba de Receitas com navegação por mês: o cabeçalho mostra o mês
 * selecionado + total, a lista filtra os lançamentos daquele mês, cada linha
 * (compacta) exibe a data e abre o editor ao toque, e o form cria/edita (ver
 * [IncomeFormSheet]).
 *
 * As linhas são propositalmente densas (uma por lançamento) porque, após
 * importar extratos, um mês pode ter dezenas de itens — cards grandes
 * tornavam a navegação cansativa.
 */
@Composable
fun IncomeTab(
    selectedMonth: java.time.YearMonth,
    onMonthChange: (java.time.YearMonth) -> Unit,
    viewModel: IncomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val monthIncomes = remember(state.incomes, selectedMonth) {
        state.incomes
            .filter { yearMonthOf(it.receivedAt) == selectedMonth }
            .sortedByDescending { it.receivedAt }
    }
    val monthTotal = remember(monthIncomes) {
        monthIncomes.fold(BigDecimal.ZERO) { acc, i -> acc + i.amount }
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
        addLabel = "Nova receita",
        isEmpty = state.incomes.isEmpty(),
        emptyTitle = "Sem receitas cadastradas",
        emptyDescription = "Adicione suas fontes de renda para o dashboard refletir sua taxa de poupança.",
        emptyIcon = Icons.Outlined.TrendingUp,
        header = {
            MonthNavigator(
                month = selectedMonth,
                onMonthChange = onMonthChange,
                total = monthTotal,
                count = monthIncomes.size,
            )
        },
    ) {
        if (monthIncomes.isEmpty()) {
            item {
                Text(
                    "Nenhuma receita em ${formatMonthYear(selectedMonth)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                )
            }
        } else {
            itemsIndexed(items = monthIncomes, key = { _, income -> income.id }) { index, income ->
                IncomeRow(
                    income = income,
                    onClick = { viewModel.openEditForm(income) },
                    onDelete = { viewModel.delete(income.id) },
                )
                if (index < monthIncomes.lastIndex) HorizontalDivider()
            }
        }
    }

    if (state.form != null) {
        IncomeFormSheet(
            form = state.form!!,
            isSubmitting = state.isSubmitting,
            onSourceChange = viewModel::onFormSourceChange,
            onAmountChange = viewModel::onFormAmountChange,
            onTypeChange = viewModel::onFormTypeChange,
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

/**
 * Linha compacta de uma receita. O toque na linha abre o editor; o ícone de
 * lixeira exclui direto. Metadados (tipo · dia/mês · recorrência) ficam numa
 * segunda linha discreta para manter a lista limpa.
 */
@Composable
private fun IncomeRow(
    income: Income,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                income.source,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(income.incomeType.label())
                    append(" · ")
                    append(formatDayMonth(income.receivedAt))
                    if (income.recurring) append(" · recorrente")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            formatBrl(income.amount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Apagar receita",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeFormSheet(
    form: IncomeFormState,
    isSubmitting: Boolean,
    onSourceChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onTypeChange: (IncomeType) -> Unit,
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
                if (form.isEditing) "Editar receita" else "Nova receita",
                style = MaterialTheme.typography.titleLarge,
            )

            LifeForgeTextField(
                value = form.source,
                onValueChange = onSourceChange,
                label = "Fonte (ex.: Salário Empresa X)",
                error = form.sourceError,
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
                label = "Tipo",
                options = IncomeType.entries,
                selected = form.incomeType,
                onSelect = onTypeChange,
                labelOf = IncomeType::label,
                enabled = !isSubmitting,
            )

            // Recorrência só faz sentido ao CRIAR (não ao editar um lançamento).
            if (!form.isEditing) {
                ScheduleToggleRow(
                    title = "Recorrente?",
                    subtitle = "Gera os lançamentos mensais (passados e futuros) automaticamente.",
                    checked = form.isRecurrent,
                    onCheckedChange = onIsRecurrentChange,
                    enabled = !isSubmitting,
                )
            }

            if (!form.isRecurrent) {
                // Modo único / edição: data de ocorrência + flag do dashboard.
                DateField(
                    label = "Data",
                    date = form.startDate,
                    onClick = { activePicker = SchedulePickerTarget.START },
                    enabled = !isSubmitting,
                )
                ScheduleToggleRow(
                    title = "Conta na renda mensal",
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
                    noun = "recebimento",
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
