package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.YearMonth

/**
 * Tela host de Finanças. TabRow alterna entre Receitas, Despesas e Ativos.
 *
 * Ações no topo:
 *  - Importar extrato (ícone de upload) → tela de importação.
 *  - Menu "⋮" → excluir todas as receitas / todas as despesas (útil ao
 *    reimportar extratos). Cada ação pede confirmação.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    onNavigateToImport: () -> Unit = {},
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    // Saveable: voltar do app (Recents/recriação) preserva a aba ativa
    // (diretriz State_Preservation do core app quality).
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }
    // Mês de visualização COMPARTILHADO entre Receitas e Despesas: alternar
    // de aba não volta para o mês atual. Saveable (como String — YearMonth
    // não é Parcelable) para sobreviver à recriação da Activity.
    var selectedMonthRaw by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val selectedMonth = remember(selectedMonthRaw) { YearMonth.parse(selectedMonthRaw) }
    val onMonthChange: (YearMonth) -> Unit = { selectedMonthRaw = it.toString() }
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.error) {
        val msg = state.message ?: state.error
        if (msg != null) {
            snackbar.showSnackbar(msg)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finanças") },
                actions = {
                    IconButton(onClick = onNavigateToImport) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = "Importar extrato")
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Mais opções")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Excluir todas as receitas") },
                            onClick = {
                                menuOpen = false
                                confirm = ConfirmAction.INCOMES
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Excluir todas as despesas") },
                            onClick = {
                                menuOpen = false
                                confirm = ConfirmAction.EXPENSES
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                FinanceTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.label) },
                    )
                }
            }

            when (FinanceTab.entries[selectedTab]) {
                FinanceTab.INCOMES -> IncomeTab(
                    selectedMonth = selectedMonth,
                    onMonthChange = onMonthChange,
                )
                FinanceTab.EXPENSES -> ExpenseTab(
                    selectedMonth = selectedMonth,
                    onMonthChange = onMonthChange,
                )
                FinanceTab.ASSETS -> AssetTab()
            }
        }
    }

    confirm?.let { action ->
        val label = if (action == ConfirmAction.INCOMES) "receitas" else "despesas"
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Excluir todas as $label?") },
            text = {
                Text(
                    "Isso remove TODAS as suas $label, inclusive as importadas dos extratos. " +
                        "Essa ação não pode ser desfeita."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    when (action) {
                        ConfirmAction.INCOMES -> viewModel.deleteAllIncomes()
                        ConfirmAction.EXPENSES -> viewModel.deleteAllExpenses()
                    }
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Cancelar") }
            },
        )
    }
}

private enum class ConfirmAction { INCOMES, EXPENSES }

private enum class FinanceTab(val label: String) {
    INCOMES("Receitas"),
    EXPENSES("Despesas"),
    ASSETS("Ativos"),
}
