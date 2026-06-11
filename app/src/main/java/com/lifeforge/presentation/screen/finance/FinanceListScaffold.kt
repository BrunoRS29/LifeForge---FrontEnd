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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
 * Atualizar é por GESTO: puxe a lista para baixo (swipe-to-refresh, padrão
 * dos apps modernos) — o indicador circular do Material 3 dá o feedback.
 * O scaffold ainda cuida do banner de erro e do FAB de adicionar.
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
    header: (@Composable () -> Unit)? = null,
    listContent: LazyListScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (errorBanner != null) {
                ErrorBanner(
                    message = errorBanner,
                    onDismiss = onErrorDismiss,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            when {
                isEmpty && !isRefreshing -> {
                    // LazyColumn + fillParentMaxSize: o gesto de puxar continua
                    // funcionando mesmo sem itens e o EmptyState fica centrado.
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        item {
                            EmptyState(
                                title = emptyTitle,
                                description = emptyDescription,
                                icon = emptyIcon,
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }
                    }
                }
                else -> {
                    header?.invoke()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
                        ),
                        content = listContent,
                    )
                }
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
