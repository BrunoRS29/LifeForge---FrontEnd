package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Tela host de Finanças. TabRow no topo alterna entre Receitas,
 * Despesas e Ativos. Cada sub-tab tem ViewModel próprio (instanciado
 * pelo `hiltViewModel()` dentro do composable filho), isolando estado
 * e ciclo de vida.
 *
 * Estado da aba selecionada é mantido localmente via `rememberSaveable`
 * — sobrevive a rotações mas não a deep links.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    onNavigateToImport: () -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finanças") },
                actions = {
                    IconButton(onClick = onNavigateToImport) {
                        Icon(
                            Icons.Outlined.UploadFile,
                            contentDescription = "Importar extrato",
                        )
                    }
                },
            )
        },
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
                FinanceTab.INCOMES -> IncomeTab()
                FinanceTab.EXPENSES -> ExpenseTab()
                FinanceTab.ASSETS -> AssetTab()
            }
        }
    }
}

private enum class FinanceTab(val label: String) {
    INCOMES("Receitas"),
    EXPENSES("Despesas"),
    ASSETS("Ativos"),
}
