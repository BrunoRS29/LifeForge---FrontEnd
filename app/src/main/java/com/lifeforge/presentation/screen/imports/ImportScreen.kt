package com.lifeforge.presentation.screen.imports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownloadDone
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.imports.Bank
import com.lifeforge.domain.imports.ClassifiedTransaction
import com.lifeforge.domain.imports.StatementKind
import com.lifeforge.domain.imports.TxnKind
import com.lifeforge.presentation.common.ActionButton
import com.lifeforge.presentation.common.ActionEmphasis
import com.lifeforge.presentation.common.ConnectedChoice
import com.lifeforge.presentation.common.DetailTopAppBar
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.FormSection
import com.lifeforge.presentation.common.GroupItem
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.ListGroup
import com.lifeforge.presentation.common.ProgressActionBar
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.SectionHeader
import com.lifeforge.presentation.common.ToggleRow
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.readableWidth
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/**
 * Importação de extratos bancários e faturas de cartão.
 *
 * Fluxo: escolher o banco → adicionar arquivos (vários, de bancos diferentes na
 * mesma sessão) → revisar (movimentos internos vêm desmarcados) → importar em
 * lote, com a ação fixa na base. O parsing e a classificação ficam no
 * ViewModel/domínio.
 */
@Composable
fun ImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addFiles(uris)
    }
    val pickFaturas = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addFaturaFiles(uris)
    }

    val included = state.includedIndices.mapNotNull { state.classified.getOrNull(it) }
    val incomes = included.filter { it.txn.amount.signum() > 0 }
    val expenses = included.filter { it.txn.amount.signum() < 0 }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopAppBar(
                title = "Importar extratos",
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
                navigationEnabled = !state.isImporting,
                actions = {
                    if (state.hasData) {
                        TextButton(onClick = viewModel::clearAll, enabled = !state.isImporting) { Text("Limpar") }
                    }
                },
            )
        },
        bottomBar = {
            if (state.hasData) {
                ProgressActionBar(
                    isRunning = state.isImporting,
                    runningText = "Importando…",
                    idleText = "Importar ${state.includedCount} lançamentos",
                    enabled = state.includedCount > 0,
                    onClick = viewModel::import,
                    icon = Icons.Outlined.FileDownloadDone,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .readableWidth(),
            contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 24.dp),
        ) {
            state.error?.let { err ->
                item(key = "error") {
                    ErrorBanner(message = err, onDismiss = viewModel::onErrorDismiss, modifier = Modifier.padding(bottom = 16.dp))
                }
            }

            item(key = "setup") {
                SetupSection(
                    state = state,
                    onBankSelected = viewModel::onBankSelected,
                    onUserNameChange = viewModel::onUserNameChange,
                    onImportInvoicesChange = viewModel::onImportInvoicesChange,
                    onAddFiles = { pickFiles.launch(arrayOf("*/*")) },
                    onAddFaturas = { pickFaturas.launch(arrayOf("*/*")) },
                )
            }

            if (state.hasData) {
                item(key = "summary") {
                    SummaryCard(
                        incomeCount = incomes.size,
                        incomeTotal = incomes.fold(BigDecimal.ZERO) { acc, c -> acc + c.txn.amount },
                        expenseCount = expenses.size,
                        expenseTotal = expenses.fold(BigDecimal.ZERO) { acc, c -> acc + c.txn.amount.abs() },
                        ignoredCount = state.ignoredCount,
                        modifier = Modifier.padding(top = 28.dp),
                    )
                }
                item(key = "review-header") {
                    SectionHeader(
                        title = "Revise os lançamentos",
                        supporting = "Movimentos internos (entre suas contas, aplicações) vêm desmarcados.",
                        modifier = Modifier.padding(top = 28.dp, bottom = 12.dp),
                    )
                }
                itemsIndexed(state.classified) { index, item ->
                    TransactionItem(
                        item = item,
                        included = index in state.includedIndices,
                        onToggle = { viewModel.toggleInclude(index) },
                        index = index,
                        count = state.classified.size,
                    )
                }
            }
        }
    }

    state.result?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::onResultDismiss,
            icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
            title = { Text("Importação concluída") },
            text = {
                Text(
                    "${result.incomesCreated} receitas e ${result.expensesCreated} despesas importadas." +
                        if (result.skipped > 0) " ${result.skipped} linhas ignoradas." else "",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onResultDismiss()
                    onNavigateBack()
                }) { Text("Concluir") }
            },
        )
    }
}

@Composable
private fun SetupSection(
    state: ImportUiState,
    onBankSelected: (Bank) -> Unit,
    onUserNameChange: (String) -> Unit,
    onImportInvoicesChange: (Boolean) -> Unit,
    onAddFiles: () -> Unit,
    onAddFaturas: () -> Unit,
) {
    val enabled = !state.isImporting
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        FormSection(title = "Extratos da conta", supporting = "Escolha o banco e adicione um ou mais arquivos.") {
            ConnectedChoice(
                options = Bank.entries,
                selected = state.selectedBank,
                onSelect = onBankSelected,
                label = { it.label },
                enabled = enabled,
            )
            LifeForgeTextField(
                value = state.userName,
                onValueChange = onUserNameChange,
                label = "Seu nome (opcional)",
                enabled = enabled,
            )
            Text(
                "Ajuda a reconhecer transferências feitas para você mesmo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ActionButton(
                text = "Adicionar extratos do ${state.selectedBank.label}",
                icon = Icons.Outlined.UploadFile,
                onClick = onAddFiles,
                enabled = enabled,
                emphasis = ActionEmphasis.Tonal,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        FormSection(title = "Faturas do cartão") {
            // Importar também as faturas do cartão (somente Nubank).
            ToggleRow(
                title = "Importar também as faturas",
                subtitle = if (state.importInvoices) {
                    "O pagamento da fatura no extrato é desativado; as despesas vêm dos itens da fatura."
                } else {
                    "Só extratos: o pagamento da fatura conta como uma despesa única."
                },
                checked = state.importInvoices,
                onCheckedChange = onImportInvoicesChange,
                enabled = enabled,
            )
            if (state.importInvoices) {
                ActionButton(
                    text = "Adicionar faturas do Nubank (CSV)",
                    icon = Icons.Outlined.CreditCard,
                    onClick = onAddFaturas,
                    enabled = enabled,
                    emphasis = ActionEmphasis.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.sources.isNotEmpty()) {
            ListGroup(
                title = "Arquivos adicionados",
                items = state.sources.map { src ->
                    val isInvoice = src.kind == StatementKind.CARD_INVOICE
                    GroupItem(
                        headline = src.fileName,
                        supporting = "${src.bank.label} · ${if (isInvoice) "fatura" else "extrato"} · ${src.count} itens",
                        icon = if (isInvoice) Icons.Outlined.CreditCard else Icons.Outlined.Description,
                    )
                },
            )
        }
    }
}

@Composable
private fun SummaryCard(
    incomeCount: Int,
    incomeTotal: BigDecimal,
    expenseCount: Int,
    expenseTotal: BigDecimal,
    ignoredCount: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("A importar", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryStat("$incomeCount receitas", formatBrl(incomeTotal), Modifier.weight(1f))
                SummaryStat("$expenseCount despesas", formatBrl(expenseTotal), Modifier.weight(1f))
            }
            Text("$ignoredCount movimentos ignorados (internos ou transferências)", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 1)
    }
}

private val ROW_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Lançamento a revisar: a linha inteira marca/desmarca; movimentos internos dizem o motivo. */
@Composable
private fun TransactionItem(
    item: ClassifiedTransaction,
    included: Boolean,
    onToggle: () -> Unit,
    index: Int,
    count: Int,
) {
    val isIncome = item.txn.amount.signum() > 0
    val valueColor = when {
        item.kind == TxnKind.INTERNAL -> MaterialTheme.colorScheme.onSurfaceVariant
        isIncome -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val amount = (if (isIncome) "+" else "−") + formatBrl(item.txn.amount.abs())
    SegmentedListItem(
        checked = included,
        onCheckedChange = { onToggle() },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            selectedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = { Checkbox(checked = included, onCheckedChange = null) },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.txn.date.format(ROW_DATE), color = MaterialTheme.colorScheme.onSurfaceVariant)
                item.internalReason?.let { reason ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = CircleShape,
                    ) {
                        Text(
                            reason.label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        },
        trailingContent = { Text(amount, style = MaterialTheme.typography.titleSmall, color = valueColor) },
        modifier = Modifier.padding(bottom = if (index < count - 1) ListItemDefaults.SegmentedGap else 0.dp),
    ) {
        Text(item.txn.description, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
