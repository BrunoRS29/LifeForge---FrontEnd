package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lifeforge.presentation.common.EmptyState
import com.lifeforge.presentation.common.ErrorBanner

/**
 * Esqueleto compartilhado pelas 3 sub-abas (Receitas, Despesas, Ativos).
 *
 * Cada sub-aba só precisa preencher:
 * - `isEmpty` + `emptyTitle` + `emptyDescription` + `emptyIcon`: state vazio
 * - `listContent`: o que renderizar quando há itens (LazyListScope para
 *   o caller emitir `items(...)` direto)
 *
 * O scaffold cuida do progress bar no topo durante refresh, banner de
 * erro dismissable, FAB que dispara `onAddClick`, e o botão de refresh
 * no canto superior direito.
 */
@Composable
fun FinanceListScaffold(
    isRefreshing: Boolean,
    errorBanner: String?,
    onErrorDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onAddClick: () -> Unit,
    addLabel: String,
    isEmpty: Boolean,
    emptyTitle: String,
    emptyDescription: String,
    emptyIcon: ImageVector,
    listContent: LazyListScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Barra de ação topo (refresh).
            Box(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar")
                }
            }

            if (errorBanner != null) {
                ErrorBanner(
                    message = errorBanner,
                    onDismiss = onErrorDismiss,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            when {
                isEmpty && !isRefreshing -> {
                    EmptyState(
                        title = emptyTitle,
                        description = emptyDescription,
                        icon = emptyIcon,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
                    ),
                    content = listContent,
                )
            }
        }

        // FAB fixo no canto inferior direito.
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(addLabel) },
            )
        }
    }
}

@Composable
fun SimpleAddFab(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Icon(Icons.Rounded.Add, contentDescription = "Adicionar")
    }
}
