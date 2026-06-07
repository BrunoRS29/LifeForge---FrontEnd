package com.lifeforge.presentation.screen.imports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.imports.Bank
import com.lifeforge.domain.imports.ClassifiedTransaction
import com.lifeforge.domain.imports.StatementKind
import com.lifeforge.domain.imports.TxnKind
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.formatBrl
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/**
 * Tela de importação de extratos bancários.
 *
 * Fluxo: escolher banco → adicionar arquivos (vários, de bancos diferentes na
 * mesma sessão) → pré-visualizar (movimentos internos vêm desmarcados) →
 * importar em lote. O parsing/classificação ficam no ViewModel/domínio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val pickFiles = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> viewModel.addFiles(uris) }

    val pickFaturas = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> viewModel.addFaturaFiles(uris) }

    val included = state.includedIndices.mapNotNull { state.classified.getOrNull(it) }
    val incomeTotal = included.filter { it.txn.amount.signum() > 0 }
        .fold(BigDecimal.ZERO) { acc, c -> acc + c.txn.amount }
    val expenseTotal = included.filter { it.txn.amount.signum() < 0 }
        .fold(BigDecimal.ZERO) { acc, c -> acc + c.txn.amount.abs() }
    val incomeCount = included.count { it.txn.amount.signum() > 0 }
    val expenseCount = included.count { it.txn.amount.signum() < 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar extrato") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isImporting) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (state.hasData) {
                        TextButton(onClick = viewModel::clearAll, enabled = !state.isImporting) {
                            Text("Limpar")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (state.hasData) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (state.isImporting) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = viewModel::import,
                        enabled = state.includedCount > 0 && !state.isImporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Importar ${state.includedCount} lançamentos")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.error?.let { err ->
                item { ErrorBanner(message = err, onDismiss = viewModel::onErrorDismiss) }
            }

            item {
                SetupCard(
                    state = state,
                    onBankSelected = viewModel::onBankSelected,
                    onUserNameChange = viewModel::onUserNameChange,
                    onImportInvoicesChange = viewModel::onImportInvoicesChange,
                    onAddFiles = { pickFiles.launch(arrayOf("*/*")) },
                    onAddFaturas = { pickFaturas.launch(arrayOf("*/*")) },
                )
            }

            if (state.hasData) {
                item {
                    SummaryCard(
                        incomeCount = incomeCount,
                        incomeTotal = incomeTotal,
                        expenseCount = expenseCount,
                        expenseTotal = expenseTotal,
                        ignoredCount = state.ignoredCount,
                    )
                }
                item {
                    Text(
                        "Revise os lançamentos (movimentos internos vêm desmarcados):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                itemsIndexed(state.classified) { index, item ->
                    TransactionRow(
                        item = item,
                        included = index in state.includedIndices,
                        onToggle = { viewModel.toggleInclude(index) },
                    )
                }
            }
        }
    }

    state.result?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::onResultDismiss,
            title = { Text("Importação concluída") },
            text = {
                Text(
                    "${result.incomesCreated} receitas e ${result.expensesCreated} despesas " +
                        "importadas." + if (result.skipped > 0) " ${result.skipped} linhas ignoradas." else ""
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
private fun SetupCard(
    state: ImportUiState,
    onBankSelected: (Bank) -> Unit,
    onUserNameChange: (String) -> Unit,
    onImportInvoicesChange: (Boolean) -> Unit,
    onAddFiles: () -> Unit,
    onAddFaturas: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Banco do extrato", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Bank.entries.forEach { bank ->
                    FilterChip(
                        selected = state.selectedBank == bank,
                        onClick = { onBankSelected(bank) },
                        label = { Text(bank.label) },
                    )
                }
            }

            OutlinedTextField(
                value = state.userName,
                onValueChange = onUserNameChange,
                label = { Text("Seu nome (opcional)") },
                supportingText = { Text("Ajuda a detectar transferências feitas para você mesmo.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(onClick = onAddFiles, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Adicionar extratos do ${state.selectedBank.label}")
            }

            HorizontalDivider()

            // Chave: importar também as faturas do cartão (somente Nubank).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Importar também as faturas?", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (state.importInvoices)
                            "O pagamento da fatura no extrato é desativado; as despesas vêm dos itens da fatura."
                        else
                            "Só extratos: o pagamento da fatura conta como uma despesa única.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.importInvoices, onCheckedChange = onImportInvoicesChange)
            }

            if (state.importInvoices) {
                OutlinedButton(onClick = onAddFaturas, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar faturas do Nubank (CSV)")
                }
            }

            if (state.sources.isNotEmpty()) {
                HorizontalDivider()
                state.sources.forEach { src ->
                    val tipo = if (src.kind == StatementKind.CARD_INVOICE) "fatura" else "extrato"
                    Text(
                        "• ${src.fileName} — ${src.bank.label} · $tipo · ${src.count} itens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "A importar",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "$incomeCount receitas · ${formatBrl(incomeTotal)}",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "$expenseCount despesas · ${formatBrl(expenseTotal)}",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "$ignoredCount movimentos ignorados (internos/transferências)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private val ROW_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
private fun TransactionRow(
    item: ClassifiedTransaction,
    included: Boolean,
    onToggle: () -> Unit,
) {
    val isIncome = item.txn.amount.signum() > 0
    val valueColor = when {
        item.kind == TxnKind.INTERNAL -> MaterialTheme.colorScheme.onSurfaceVariant
        isIncome -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = included, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(
                item.txn.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    item.txn.date.format(ROW_DATE),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.internalReason?.let { reason ->
                    AssistChip(
                        onClick = onToggle,
                        label = { Text(reason.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
        Text(
            text = (if (isIncome) "+" else "-") + formatBrl(item.txn.amount.abs()),
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
        )
    }
}
