package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.presentation.common.TabTopAppBar
import com.lifeforge.presentation.common.formatMonthYear
import java.time.YearMonth

/**
 * Tela de Finanças: abas Receitas, Despesas e Ativos sob a barra grande, que
 * recolhe ao rolar e mostra o mês em uso no subtítulo.
 *
 * Ações no topo:
 *  - Importar extrato (ícone de upload) → tela de importação.
 *  - Menu "⋮" → excluir todas as receitas / todas as despesas (útil ao
 *    reimportar extratos). Cada ação pede confirmação.
 */
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val tab = FinanceTab.entries[selectedTab]

    LaunchedEffect(state.message, state.error) {
        val msg = state.message ?: state.error
        if (msg != null) {
            snackbar.showSnackbar(msg)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TabTopAppBar(
                title = "Finanças",
                subtitle = if (tab == FinanceTab.ASSETS) "Patrimônio e alocação" else formatMonthYear(selectedMonth),
                scrollBehavior = scrollBehavior,
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
                            leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                confirm = ConfirmAction.INCOMES
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Excluir todas as despesas") },
                            leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
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
            PrimaryTabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                FinanceTab.entries.forEachIndexed { index, financeTab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(financeTab.label) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (tab) {
                FinanceTab.INCOMES -> IncomeTab(selectedMonth = selectedMonth, onMonthChange = onMonthChange)
                FinanceTab.EXPENSES -> ExpenseTab(selectedMonth = selectedMonth, onMonthChange = onMonthChange)
                FinanceTab.ASSETS -> AssetTab()
            }
        }
    }

    confirm?.let { action ->
        val label = if (action == ConfirmAction.INCOMES) "receitas" else "despesas"
        AlertDialog(
            onDismissRequest = { confirm = null },
            icon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
            title = { Text("Excluir todas as $label?") },
            text = {
                Text(
                    "Isso remove TODAS as suas $label, inclusive as importadas dos extratos. " +
                        "Essa ação não pode ser desfeita.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    when (action) {
                        ConfirmAction.INCOMES -> viewModel.deleteAllIncomes()
                        ConfirmAction.EXPENSES -> viewModel.deleteAllExpenses()
                    }
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
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
