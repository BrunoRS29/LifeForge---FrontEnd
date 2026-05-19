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
import androidx.compose.material.icons.outlined.TrendingUp
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
import com.lifeforge.domain.model.Income
import com.lifeforge.domain.model.IncomeType
import com.lifeforge.presentation.common.CurrencyField
import com.lifeforge.presentation.common.EnumDropdown
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.LoadingOverlay
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.label

/**
 * Sub-aba de Receitas. Lista cards com cada receita; toque no ícone
 * de lixeira deleta. FAB abre um ModalBottomSheet com o form de
 * criação.
 */
@Composable
fun IncomeTab(viewModel: IncomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    FinanceListScaffold(
        isRefreshing = state.isRefreshing,
        errorBanner = state.errorBanner,
        onErrorDismiss = viewModel::onErrorBannerDismiss,
        onRefresh = viewModel::refresh,
        onAddClick = viewModel::openForm,
        addLabel = "Nova receita",
        isEmpty = state.incomes.isEmpty(),
        emptyTitle = "Sem receitas cadastradas",
        emptyDescription = "Adicione suas fontes de renda para o dashboard refletir sua taxa de poupança.",
        emptyIcon = Icons.Outlined.TrendingUp,
    ) {
        items(items = state.incomes, key = { it.id }) { income ->
            IncomeCard(
                income = income,
                onDelete = { viewModel.delete(income.id) },
                modifier = Modifier.padding(bottom = 12.dp),
            )
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
            onSubmit = viewModel::submitForm,
            onDismiss = viewModel::closeForm,
        )
    }
}

@Composable
private fun IncomeCard(
    income: Income,
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
                Text(income.source, style = MaterialTheme.typography.titleMedium)
                Text(
                    income.incomeType.label() +
                        if (income.recurring) " · Recorrente" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatBrl(income.amount),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
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
private fun IncomeFormSheet(
    form: IncomeFormState,
    isSubmitting: Boolean,
    onSourceChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onTypeChange: (IncomeType) -> Unit,
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
            Text("Nova receita", style = MaterialTheme.typography.titleLarge)

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
                label = "Valor (R$)",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Recorrente", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Receitas recorrentes entram na taxa de poupança",
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
