package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeforge.presentation.common.EmptyState
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.PendingSyncLabel
import com.lifeforge.presentation.common.RefreshableBox
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.ShapeIcon
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatDayHeader
import com.lifeforge.presentation.common.localDateOf
import com.lifeforge.presentation.common.readableWidth
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Esqueleto compartilhado pelas 3 sub-abas (Receitas, Despesas, Ativos).
 *
 * Cada sub-aba preenche o estado vazio (`isEmpty` + textos + ícone) e o
 * conteúdo da lista (`LazyListScope`, para emitir os itens direto). O
 * cabeçalho ([header]) rola junto com a lista.
 *
 * Atualizar é por gesto (puxar para baixo, com o indicador expressivo do
 * Material 3). O botão "Nova …" encolhe para só o ícone enquanto a lista rola.
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
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    RefreshableBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .readableWidth(),
            contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 104.dp),
        ) {
            if (errorBanner != null) {
                item(key = "error") {
                    ErrorBanner(
                        message = errorBanner,
                        onDismiss = onErrorDismiss,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
            if (isEmpty && !isRefreshing) {
                // Dentro da lista, o gesto de puxar para atualizar continua valendo.
                item(key = "empty") {
                    EmptyState(
                        title = emptyTitle,
                        description = emptyDescription,
                        icon = emptyIcon,
                        modifier = Modifier.padding(top = 48.dp),
                    )
                }
            } else {
                if (header != null) item(key = "header") { header() }
                listContent()
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddClick,
            expanded = fabExpanded,
            // Recolhido, o botão fica só com o ícone: aí o ícone carrega o rótulo.
            icon = { Icon(Icons.Rounded.Add, contentDescription = if (fabExpanded) null else addLabel) },
            text = { Text(addLabel) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
}

/** Natureza do item: define a cor do ícone e do valor na lista. */
enum class EntryKind { INCOME, EXPENSE, ASSET }

/** Uma linha da lista de lançamentos, já formatada por cada aba. */
data class FinanceEntryUi(
    val id: Long,
    val title: String,
    val supporting: String,
    val value: BigDecimal,
    val date: Instant,
    val icon: ImageVector,
    val kind: EntryKind,
    val pendingSync: Boolean,
)

/**
 * Lançamentos agrupados por dia (do mais recente ao mais antigo): cabeçalho com
 * "Hoje"/"Ontem"/"Sexta, 2 de outubro" e o total do dia, e os itens do dia numa
 * lista segmentada. Tocar abre o editor; deslizar para a esquerda pede para excluir.
 */
fun LazyListScope.financeEntriesByDay(
    entries: List<FinanceEntryUi>,
    today: LocalDate,
    deleteNoun: String,
    onClick: (FinanceEntryUi) -> Unit,
    onDelete: (FinanceEntryUi) -> Unit,
) {
    val byDay = entries.groupBy { localDateOf(it.date) }.toSortedMap(compareByDescending { it })
    byDay.forEach { (day, dayEntries) ->
        item(key = "day-$day") {
            DayHeader(
                label = formatDayHeader(day, today),
                total = dayEntries.fold(BigDecimal.ZERO) { acc, e -> acc + e.value },
            )
        }
        itemsIndexed(dayEntries, key = { _, entry -> entry.id }) { index, entry ->
            SwipeToDeleteEntry(
                entry = entry,
                index = index,
                count = dayEntries.size,
                deleteNoun = deleteNoun,
                onClick = { onClick(entry) },
                onDelete = { onDelete(entry) },
                modifier = Modifier
                    .animateItem()
                    .padding(bottom = if (index < dayEntries.lastIndex) ListItemDefaults.SegmentedGap else 0.dp),
            )
        }
    }
}

/** Itens numa única lista segmentada, sem agrupar por dia (ex.: ativos). */
fun LazyListScope.financeEntries(
    entries: List<FinanceEntryUi>,
    deleteNoun: String,
    onClick: (FinanceEntryUi) -> Unit,
    onDelete: (FinanceEntryUi) -> Unit,
) {
    itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
        SwipeToDeleteEntry(
            entry = entry,
            index = index,
            count = entries.size,
            deleteNoun = deleteNoun,
            onClick = { onClick(entry) },
            onDelete = { onDelete(entry) },
            modifier = Modifier
                .animateItem()
                .padding(bottom = if (index < entries.lastIndex) ListItemDefaults.SegmentedGap else 0.dp),
        )
    }
}

@Composable
private fun DayHeader(label: String, total: BigDecimal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        Text(
            formatBrl(total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Item com "deslizar para excluir": o gesto revela a lixeira e pede confirmação
 * (a exclusão não tem desfazer). Para o TalkBack, a mesma ação fica disponível
 * como ação personalizada do item.
 */
@Composable
private fun SwipeToDeleteEntry(
    entry: FinanceEntryUi,
    index: Int,
    count: Int,
    deleteNoun: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    var confirming by remember { mutableStateOf(false) }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        onDismiss = { confirming = true },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shapes.shape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        modifier = modifier.semantics {
            customActions = listOf(CustomAccessibilityAction("Excluir $deleteNoun") { confirming = true; true })
        },
    ) {
        FinanceEntryItem(entry = entry, shapes = shapes, onClick = onClick)
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = {
                confirming = false
                scope.launch { dismissState.reset() }
            },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Excluir $deleteNoun?") },
            text = { Text("\"${entry.title}\", ${formatBrl(entry.value)}. Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onDelete()
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirming = false
                    scope.launch { dismissState.reset() }
                }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun FinanceEntryItem(
    entry: FinanceEntryUi,
    shapes: ListItemShapes,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val (iconContainer, iconContent) = when (entry.kind) {
        EntryKind.INCOME -> colors.primaryContainer to colors.onPrimaryContainer
        EntryKind.EXPENSE -> colors.tertiaryContainer to colors.onTertiaryContainer
        EntryKind.ASSET -> colors.secondaryContainer to colors.onSecondaryContainer
    }
    val amountColor: Color = if (entry.kind == EntryKind.INCOME) colors.primary else colors.onSurface
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        colors = ListItemDefaults.segmentedColors(containerColor = colors.surfaceContainer),
        leadingContent = {
            ShapeIcon(
                icon = entry.icon,
                containerColor = iconContainer,
                contentColor = iconContent,
                shape = CircleShape,
            )
        },
        supportingContent = {
            Column {
                Text(
                    entry.supporting,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.pendingSync) PendingSyncLabel()
            }
        },
        trailingContent = {
            Text(
                formatBrl(entry.value),
                style = MaterialTheme.typography.titleSmall,
                color = amountColor,
                maxLines = 1,
            )
        },
    ) {
        Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Mensagem curta quando o mês escolhido não tem lançamentos. */
@Composable
fun EmptyMonthMessage(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 24.dp),
    )
}
