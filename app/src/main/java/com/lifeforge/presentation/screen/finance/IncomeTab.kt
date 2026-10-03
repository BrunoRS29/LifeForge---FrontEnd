package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.Income
import com.lifeforge.domain.model.IncomeType
import com.lifeforge.domain.model.RecurrenceType
import com.lifeforge.presentation.common.EnumDropdown
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.MoneyField
import com.lifeforge.presentation.common.firstInstantOfMonth
import com.lifeforge.presentation.common.formatMonthYear
import com.lifeforge.presentation.common.icon
import com.lifeforge.presentation.common.label
import com.lifeforge.presentation.common.localDateOf
import com.lifeforge.presentation.common.yearMonthOf
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth

/**
 * Sub-aba de Receitas com navegação por mês: o cabeçalho mostra o mês e o
 * total, e a lista traz os lançamentos do mês agrupados por dia. Tocar num
 * lançamento abre o editor ([IncomeFormSheet]); deslizar para a esquerda exclui.
 */
@Composable
fun IncomeTab(
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    viewModel: IncomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val monthIncomes = remember(state.incomes, selectedMonth) {
        state.incomes.filter { yearMonthOf(it.receivedAt) == selectedMonth }.sortedByDescending { it.receivedAt }
    }
    val monthTotal = remember(monthIncomes) { monthIncomes.fold(BigDecimal.ZERO) { acc, i -> acc + i.amount } }
    val entries = remember(monthIncomes) { monthIncomes.map(::incomeEntry) }
    val today = localDateOf(Instant.now())

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
        emptyDescription = "Adicione suas fontes de renda para o painel refletir sua taxa de poupança.",
        emptyIcon = Icons.AutoMirrored.Outlined.TrendingUp,
        header = {
            MonthNavigator(
                month = selectedMonth,
                onMonthChange = onMonthChange,
                total = monthTotal,
                count = monthIncomes.size,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        },
    ) {
        if (entries.isEmpty()) {
            item(key = "empty-month") { EmptyMonthMessage("Nenhuma receita em ${formatMonthYear(selectedMonth)}.") }
        } else {
            financeEntriesByDay(
                entries = entries,
                today = today,
                deleteNoun = "receita",
                onClick = { entry -> monthIncomes.firstOrNull { it.id == entry.id }?.let(viewModel::openEditForm) },
                onDelete = { entry -> viewModel.delete(entry.id) },
            )
        }
    }

    state.form?.let { form ->
        IncomeFormSheet(
            form = form,
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
            onDelete = form.editingId?.let { id ->
                {
                    viewModel.closeForm()
                    viewModel.delete(id)
                }
            },
        )
    }
}

/** Receita → linha da lista: tipo e recorrência na segunda linha. */
private fun incomeEntry(income: Income) = FinanceEntryUi(
    id = income.id,
    title = income.source,
    supporting = income.incomeType.label() + if (income.recurring) " · recorrente" else "",
    value = income.amount,
    date = income.receivedAt,
    icon = income.incomeType.icon(),
    kind = EntryKind.INCOME,
    pendingSync = income.pendingSync,
)

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
    onDelete: (() -> Unit)?,
) {
    val enabled = !isSubmitting
    FinanceFormSheet(
        title = if (form.isEditing) "Editar receita" else "Nova receita",
        isSubmitting = isSubmitting,
        canSubmit = form.canSubmit,
        submitLabel = when {
            form.isEditing -> "Salvar"
            form.isRecurrent -> "Criar recorrência"
            else -> "Adicionar"
        },
        onSubmit = onSubmit,
        onDismiss = onDismiss,
        deleteNoun = "receita",
        onDelete = onDelete,
    ) {
        LifeForgeTextField(
            value = form.source,
            onValueChange = onSourceChange,
            label = "Fonte (ex.: Salário Empresa X)",
            error = form.sourceError,
            imeAction = ImeAction.Next,
            enabled = enabled,
        )
        MoneyField(
            value = form.amountInput,
            onValueChange = onAmountChange,
            label = if (form.isRecurrent) "Valor por ocorrência" else "Valor",
            error = form.amountError,
            enabled = enabled,
        )
        EnumDropdown(
            label = "Tipo",
            options = IncomeType.entries,
            selected = form.incomeType,
            onSelect = onTypeChange,
            labelOf = IncomeType::label,
            iconOf = IncomeType::icon,
            enabled = enabled,
        )
        ScheduleFields(
            isEditing = form.isEditing,
            isRecurrent = form.isRecurrent,
            onIsRecurrentChange = onIsRecurrentChange,
            recurrentSubtitle = "Gera os recebimentos mensais (passados e futuros) automaticamente.",
            recurring = form.recurring,
            onRecurringChange = onRecurringChange,
            recurringTitle = "Conta na renda mensal",
            recurrenceType = form.recurrenceType,
            onRecurrenceTypeChange = onRecurrenceTypeChange,
            startDate = form.startDate,
            onStartDateChange = onStartDateChange,
            endDate = form.endDate,
            onEndDateChange = onEndDateChange,
            installmentsInput = form.installmentsInput,
            installmentsValid = form.installmentsValid,
            onInstallmentsChange = onInstallmentsChange,
            noun = "recebimento",
            enabled = enabled,
        )
    }
}
