package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.Expense
import com.lifeforge.domain.model.ExpenseCategory
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
 * Sub-aba de Despesas. Espelha [IncomeTab] — mês com total, lançamentos
 * agrupados por dia, tocar para editar, deslizar para excluir — e acrescenta o
 * filtro por categoria: depois de importar extratos, um mês costuma ter muitas
 * despesas, e filtrar deixa a navegação bem mais direta.
 */
@Composable
fun ExpenseTab(
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var categoryFilter by rememberSaveable { mutableStateOf<ExpenseCategory?>(null) }

    val monthExpenses = remember(state.expenses, selectedMonth) {
        state.expenses.filter { yearMonthOf(it.spentAt) == selectedMonth }.sortedByDescending { it.spentAt }
    }
    val visibleExpenses = remember(monthExpenses, categoryFilter) {
        categoryFilter?.let { cat -> monthExpenses.filter { it.category == cat } } ?: monthExpenses
    }
    val monthTotal = remember(visibleExpenses) { visibleExpenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount } }
    // Categorias presentes no mês — só essas viram filtro.
    val monthCategories = remember(monthExpenses) { monthExpenses.map { it.category }.distinct() }
    val entries = remember(visibleExpenses) { visibleExpenses.map(::expenseEntry) }
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
        addLabel = "Nova despesa",
        isEmpty = state.expenses.isEmpty(),
        emptyTitle = "Sem despesas cadastradas",
        emptyDescription = "Acompanhar as despesas ajuda a calcular sua taxa de poupança real.",
        emptyIcon = Icons.AutoMirrored.Outlined.TrendingDown,
        header = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                MonthNavigator(
                    month = selectedMonth,
                    onMonthChange = { newMonth ->
                        categoryFilter = null
                        onMonthChange(newMonth)
                    },
                    total = monthTotal,
                    count = visibleExpenses.size,
                )
                if (monthCategories.size > 1) {
                    CategoryFilterRow(
                        categories = monthCategories,
                        selected = categoryFilter,
                        onSelect = { categoryFilter = it },
                    )
                }
            }
        },
    ) {
        if (entries.isEmpty()) {
            item(key = "empty-month") { EmptyMonthMessage("Nenhuma despesa em ${formatMonthYear(selectedMonth)}.") }
        } else {
            financeEntriesByDay(
                entries = entries,
                today = today,
                deleteNoun = "despesa",
                onClick = { entry -> visibleExpenses.firstOrNull { it.id == entry.id }?.let(viewModel::openEditForm) },
                onDelete = { entry -> viewModel.delete(entry.id) },
            )
        }
    }

    state.form?.let { form ->
        ExpenseFormSheet(
            form = form,
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
            onDelete = form.editingId?.let { id ->
                {
                    viewModel.closeForm()
                    viewModel.delete(id)
                }
            },
        )
    }
}

/** Despesa → linha da lista: categoria e recorrência na segunda linha. */
private fun expenseEntry(expense: Expense) = FinanceEntryUi(
    id = expense.id,
    title = expense.description,
    supporting = expense.category.label() + if (expense.recurring) " · recorrente" else "",
    value = expense.amount,
    date = expense.spentAt,
    icon = expense.category.icon(),
    kind = EntryKind.EXPENSE,
    pendingSync = expense.pendingSync,
)

/** Filtros por categoria (com o ícone de cada uma); "Todas" limpa o filtro. */
@Composable
private fun CategoryFilterRow(
    categories: List<ExpenseCategory>,
    selected: ExpenseCategory?,
    onSelect: (ExpenseCategory?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("Todas") },
                leadingIcon = if (selected == null) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
            )
        }
        items(categories) { cat ->
            val isSelected = selected == cat
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(if (isSelected) null else cat) },
                label = { Text(cat.label()) },
                leadingIcon = {
                    Icon(
                        if (isSelected) Icons.Rounded.Check else cat.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}

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
    onDelete: (() -> Unit)?,
) {
    val enabled = !isSubmitting
    FinanceFormSheet(
        title = if (form.isEditing) "Editar despesa" else "Nova despesa",
        isSubmitting = isSubmitting,
        canSubmit = form.canSubmit,
        submitLabel = when {
            form.isEditing -> "Salvar"
            form.isRecurrent -> "Criar recorrência"
            else -> "Adicionar"
        },
        onSubmit = onSubmit,
        onDismiss = onDismiss,
        deleteNoun = "despesa",
        onDelete = onDelete,
    ) {
        LifeForgeTextField(
            value = form.description,
            onValueChange = onDescriptionChange,
            label = "Descrição (ex.: Aluguel apartamento)",
            error = form.descriptionError,
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
            label = "Categoria",
            options = ExpenseCategory.entries,
            selected = form.category,
            onSelect = onCategoryChange,
            labelOf = ExpenseCategory::label,
            iconOf = ExpenseCategory::icon,
            enabled = enabled,
        )
        ScheduleFields(
            isEditing = form.isEditing,
            isRecurrent = form.isRecurrent,
            onIsRecurrentChange = onIsRecurrentChange,
            recurrentSubtitle = "Gera os lançamentos mensais (ou as parcelas) automaticamente.",
            recurring = form.recurring,
            onRecurringChange = onRecurringChange,
            recurringTitle = "Conta na despesa mensal",
            recurrenceType = form.recurrenceType,
            onRecurrenceTypeChange = onRecurrenceTypeChange,
            startDate = form.startDate,
            onStartDateChange = onStartDateChange,
            endDate = form.endDate,
            onEndDateChange = onEndDateChange,
            installmentsInput = form.installmentsInput,
            installmentsValid = form.installmentsValid,
            onInstallmentsChange = onInstallmentsChange,
            noun = "despesa",
            enabled = enabled,
        )
    }
}
