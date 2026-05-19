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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.lifeforge.domain.model.Asset
import com.lifeforge.domain.model.AssetType
import com.lifeforge.presentation.common.CurrencyField
import com.lifeforge.presentation.common.EnumDropdown
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.LoadingOverlay
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.label

/**
 * Sub-aba de Ativos. Cards mostram nome, tipo e valor atual; toque
 * abre o form em modo edição, ícone de lixeira deleta.
 *
 * Form tem 5 campos (vs 4 do Income/Expense): nome, tipo, valor atual,
 * retorno esperado anual, volatilidade anual. Os dois últimos são
 * percentuais em decimal (ex.: 0.08 = 8% a.a.) — convenção do backend.
 */
@Composable
fun AssetTab(viewModel: AssetViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    FinanceListScaffold(
        isRefreshing = state.isRefreshing,
        errorBanner = state.errorBanner,
        onErrorDismiss = viewModel::onErrorBannerDismiss,
        onRefresh = viewModel::refresh,
        onAddClick = viewModel::openCreateForm,
        addLabel = "Novo ativo",
        isEmpty = state.assets.isEmpty(),
        emptyTitle = "Sem ativos cadastrados",
        emptyDescription = "Adicione seus ativos para alimentar a otimização de carteira e o cálculo de patrimônio.",
        emptyIcon = Icons.Outlined.AccountBalanceWallet,
    ) {
        items(items = state.assets, key = { it.id }) { asset ->
            AssetCard(
                asset = asset,
                onClick = { viewModel.openEditForm(asset) },
                onDelete = { viewModel.delete(asset.id) },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }

    if (state.form != null) {
        AssetFormSheet(
            form = state.form!!,
            isSubmitting = state.isSubmitting,
            onNameChange = viewModel::onFormNameChange,
            onTypeChange = viewModel::onFormTypeChange,
            onCurrentValueChange = viewModel::onFormCurrentValueChange,
            onExpectedReturnChange = viewModel::onFormExpectedReturnChange,
            onVolatilityChange = viewModel::onFormVolatilityChange,
            onSubmit = viewModel::submitForm,
            onDismiss = viewModel::closeForm,
        )
    }
}

@Composable
private fun AssetCard(
    asset: Asset,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
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
                Text(asset.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    asset.assetType.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatBrl(asset.currentValue),
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
private fun AssetFormSheet(
    form: AssetFormState,
    isSubmitting: Boolean,
    onNameChange: (String) -> Unit,
    onTypeChange: (AssetType) -> Unit,
    onCurrentValueChange: (String) -> Unit,
    onExpectedReturnChange: (String) -> Unit,
    onVolatilityChange: (String) -> Unit,
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
            Text(
                if (form.isEditing) "Editar ativo" else "Novo ativo",
                style = MaterialTheme.typography.titleLarge,
            )

            LifeForgeTextField(
                value = form.name,
                onValueChange = onNameChange,
                label = "Nome (ex.: Tesouro IPCA+ 2035)",
                error = form.nameError,
                imeAction = ImeAction.Next,
                enabled = !isSubmitting,
            )
            EnumDropdown(
                label = "Tipo",
                options = AssetType.entries,
                selected = form.assetType,
                onSelect = onTypeChange,
                labelOf = AssetType::label,
                enabled = !isSubmitting,
            )
            CurrencyField(
                value = form.currentValueInput,
                onValueChange = onCurrentValueChange,
                label = "Valor atual (R$)",
                error = form.currentValueError,
                enabled = !isSubmitting,
            )
            CurrencyField(
                value = form.expectedReturnInput,
                onValueChange = onExpectedReturnChange,
                label = "Retorno esperado anual (ex.: 0,08 = 8%)",
                error = form.expectedReturnError,
                enabled = !isSubmitting,
            )
            CurrencyField(
                value = form.volatilityInput,
                onValueChange = onVolatilityChange,
                label = "Volatilidade anual (ex.: 0,15 = 15%)",
                error = form.volatilityError,
                imeAction = ImeAction.Done,
                enabled = !isSubmitting,
            )

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
                    Text(if (form.isEditing) "Salvar" else "Adicionar")
                }
            }
        }
        LoadingOverlay(visible = isSubmitting)
    }
}
