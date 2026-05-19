package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.Expense
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.presentation.common.CurrencyField
import com.lifeforge.presentation.common.EnumDropdown
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.LoadingOverlay
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.label

/**
 * Sub-aba de Despesas. Estruturalmente espelha [IncomeTab] — diferenças
 * estão no label ("descrição" em vez de "fonte"), categoria em vez de
 * tipo, e ícone/cor de destaque (tertiary para despesas).
 */
@Composable
fun ExpenseTab(viewModel: ExpenseViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    FinanceListScaffold(
        isRefreshing = state.isRefreshing,
        errorBanner = state.errorBanner,
        onErrorDismiss = viewModel::onErrorBannerDismiss,
        onRefresh = viewModel::refresh,
        onAddClick = viewModel::openForm,
        addLabel = "Nova despesa",
        isEmpty = state.expenses.isEmpty(),
        emptyTitle = "Sem despesas cadastradas",
        emptyDescription = "Acompanhar despesas recorrentes ajuda a calcular sua taxa de poupança real.",
        emptyIcon = Icons.Outlined.TrendingDown,
    ) {
        items(items = state.expenses, key = { it.id }) { expense ->
            ExpenseCard(
                expense = expense,
                onDelete = { viewModel.delete(expense.id) },
                modifier = Modifier.padding(bottom = 12.dp),
            )
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
            onSubmit = viewModel::submitForm,
            onDismiss = viewModel::closeForm,
        )
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, style = MaterialTheme.typography.titleMedium)
                Text(
                    expense.category.label() +
                        if (expense.recurring) " · Recorrente" else "",
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
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Nova despesa", style = MaterialTheme.typography.titleLarge)

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
                label = "Valor (R$)",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Recorrente", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Despesas recorrentes entram na taxa de poupança",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = form.recurring,
                    onCheckedChange = onRecurringChange,
                    enabled = !isSubmitting,
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
                    Text("Adicionar")
                }
            }
        }
        LoadingOverlay(visible = isSubmitting)
    }
}
