package com.lifeforge.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lifeforge.domain.repository.SyncStatus

/**
 * Faixa discreta de status da sincronização offline-first, exibida acima da
 * barra de navegação. Só aparece quando há algo a comunicar:
 *
 * - sem conexão → o app segue funcionando com os dados salvos no aparelho;
 * - alterações aguardando envio → quantas são, com a opção de sincronizar já.
 *
 * Anunciada pelo leitor de tela quando muda (live region educada).
 */
@Composable
fun SyncStatusBar(
    status: SyncStatus,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = !status.isOnline || status.pendingOperations > 0
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        val offline = !status.isOnline
        Surface(
            color = if (offline) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (offline) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSyncing) {
                    LoadingIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = if (offline) Icons.Outlined.CloudOff else Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = syncStatusMessage(status),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                if (!offline && status.pendingOperations > 0) {
                    TextButton(onClick = onSyncNow, enabled = !isSyncing) {
                        Text("Sincronizar")
                    }
                }
            }
        }
    }
}

/** Texto da faixa de sincronização (separado para ser testável). */
fun syncStatusMessage(status: SyncStatus): String {
    val pending = status.pendingOperations
    val pendingText = when (pending) {
        0 -> null
        1 -> "1 alteração aguardando envio"
        else -> "$pending alterações aguardando envio"
    }
    return when {
        !status.isOnline && pendingText != null -> "Sem conexão · $pendingText"
        !status.isOnline -> "Sem conexão · exibindo os dados salvos no aparelho"
        pendingText != null -> pendingText.replaceFirstChar { it.uppercase() }
        else -> "Tudo sincronizado"
    }
}

/**
 * Marca discreta de item salvo só no aparelho, aguardando envio ao servidor
 * (padrão offline-first). Ícone + texto: a informação não depende da cor.
 */
@Composable
fun PendingSyncLabel(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.CloudUpload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            "Aguardando envio",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
