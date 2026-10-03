package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.Asset
import com.lifeforge.domain.model.AssetType
import com.lifeforge.presentation.common.AllocationBar
import com.lifeforge.presentation.common.AllocationShare
import com.lifeforge.presentation.common.AutoSizeText
import com.lifeforge.presentation.common.EnumDropdown
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.MoneyField
import com.lifeforge.presentation.common.PercentField
import com.lifeforge.presentation.common.formatAnnualRate
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.icon
import com.lifeforge.presentation.common.label
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Sub-aba de Ativos: o patrimônio total com a alocação por tipo de ativo
 * (barra proporcional + legenda com as porcentagens) e a lista dos ativos.
 * Tocar abre o editor; deslizar para a esquerda exclui.
 *
 * Retorno esperado e volatilidade são digitados em porcentagem ao ano; o
 * domínio guarda a fração (convenção do backend).
 */
@Composable
fun AssetTab(viewModel: AssetViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val assets = remember(state.assets) { state.assets.sortedByDescending { it.currentValue } }
    val entries = remember(assets) { assets.map(::assetEntry) }

    FinanceListScaffold(
        isRefreshing = state.isRefreshing,
        errorBanner = state.errorBanner,
        onErrorDismiss = viewModel::onErrorBannerDismiss,
        onRefresh = viewModel::refresh,
        onAddClick = viewModel::openCreateForm,
        addLabel = "Novo ativo",
        isEmpty = state.assets.isEmpty(),
        emptyTitle = "Sem ativos cadastrados",
        emptyDescription = "Adicione seus ativos para alimentar a otimização da carteira e o cálculo do patrimônio.",
        emptyIcon = Icons.Outlined.AccountBalanceWallet,
        header = { AllocationCard(assets = assets, modifier = Modifier.padding(bottom = 16.dp)) },
    ) {
        financeEntries(
            entries = entries,
            deleteNoun = "ativo",
            onClick = { entry -> assets.firstOrNull { it.id == entry.id }?.let(viewModel::openEditForm) },
            onDelete = { entry -> viewModel.delete(entry.id) },
        )
    }

    state.form?.let { form ->
        AssetFormSheet(
            form = form,
            isSubmitting = state.isSubmitting,
            onNameChange = viewModel::onFormNameChange,
            onTypeChange = viewModel::onFormTypeChange,
            onCurrentValueChange = viewModel::onFormCurrentValueChange,
            onExpectedReturnChange = viewModel::onFormExpectedReturnChange,
            onVolatilityChange = viewModel::onFormVolatilityChange,
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

/** Ativo → linha da lista: tipo, retorno e volatilidade na segunda linha. */
private fun assetEntry(asset: Asset) = FinanceEntryUi(
    id = asset.id,
    title = asset.name,
    supporting = "${asset.assetType.label()} · ${formatAnnualRate(asset.expectedReturn.toDouble())} a.a. · " +
        "vol. ${formatAnnualRate(asset.volatility.toDouble())}",
    value = asset.currentValue,
    date = asset.createdAt,
    icon = asset.assetType.icon(),
    kind = EntryKind.ASSET,
    pendingSync = asset.pendingSync,
)

/** Fatia da carteira de um tipo de ativo. */
internal data class AllocationSlice(val type: AssetType, val value: BigDecimal, val share: Double)

/** Soma por tipo de ativo, do maior para o menor, com a participação de cada um. */
internal fun assetAllocation(assets: List<Asset>): List<AllocationSlice> {
    val total = assets.fold(BigDecimal.ZERO) { acc, a -> acc + a.currentValue }
    if (total.signum() <= 0) return emptyList()
    return assets.groupBy { it.assetType }
        .map { (type, items) ->
            val value = items.fold(BigDecimal.ZERO) { acc, a -> acc + a.currentValue }
            AllocationSlice(type, value, value.divide(total, 6, RoundingMode.HALF_UP).toDouble())
        }
        .sortedByDescending { it.value }
}

@Composable
private fun AllocationCard(assets: List<Asset>, modifier: Modifier = Modifier) {
    val total = assets.fold(BigDecimal.ZERO) { acc, a -> acc + a.currentValue }
    val shares = remember(assets) { assetAllocation(assets).map { AllocationShare(it.type.label(), it.share) } }
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Patrimônio total", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                AutoSizeText(
                    text = formatBrl(total),
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = colors.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (assets.size == 1) "1 ativo" else "${assets.size} ativos",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            AllocationBar(shares = shares)
        }
    }
}

/** Participação com uma casa: 0.4567 → "45,7%". */
internal fun percentText(share: Double): String = formatAnnualRate(share)

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
    onDelete: (() -> Unit)?,
) {
    val enabled = !isSubmitting
    FinanceFormSheet(
        title = if (form.isEditing) "Editar ativo" else "Novo ativo",
        isSubmitting = isSubmitting,
        canSubmit = form.canSubmit,
        submitLabel = if (form.isEditing) "Salvar" else "Adicionar",
        onSubmit = onSubmit,
        onDismiss = onDismiss,
        deleteNoun = "ativo",
        onDelete = onDelete,
    ) {
        LifeForgeTextField(
            value = form.name,
            onValueChange = onNameChange,
            label = "Nome (ex.: Tesouro IPCA+ 2035)",
            error = form.nameError,
            imeAction = ImeAction.Next,
            enabled = enabled,
        )
        EnumDropdown(
            label = "Tipo",
            options = AssetType.entries,
            selected = form.assetType,
            onSelect = onTypeChange,
            labelOf = AssetType::label,
            iconOf = AssetType::icon,
            enabled = enabled,
        )
        MoneyField(
            value = form.currentValueInput,
            onValueChange = onCurrentValueChange,
            label = "Valor atual",
            error = form.currentValueError,
            enabled = enabled,
        )
        PercentField(
            value = form.expectedReturnInput,
            onValueChange = onExpectedReturnChange,
            label = "Retorno esperado",
            suffix = "% a.a.",
            error = form.expectedReturnError,
            enabled = enabled,
        )
        PercentField(
            value = form.volatilityInput,
            onValueChange = onVolatilityChange,
            label = "Volatilidade",
            suffix = "% a.a.",
            supportingText = "Quanto o retorno oscila de um ano para o outro.",
            error = form.volatilityError,
            imeAction = ImeAction.Done,
            enabled = enabled,
        )
    }
}
