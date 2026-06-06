package com.lifeforge.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

/**
 * Componentes reutilizáveis usados em várias telas. Mantidos juntos
 * porque são pequenos e relacionados (apresentação de estados de
 * carregamento, erro e vazio).
 */

/**
 * Overlay semi-transparente com spinner. Usado durante submissão de
 * forms — bloqueia toques na UI subjacente para impedir double-submit
 * sem precisar rastrear cada botão individualmente.
 *
 * Aplica `pointerInput` consumindo todos os toques quando visível.
 */
@Composable
fun LoadingOverlay(visible: Boolean) {
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) { /* consome toques */ },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
    }
}

/**
 * Banner de erro inline. Aparece acima do conteúdo quando uma operação
 * falha — diferente do Snackbar (transitório), o banner persiste até o
 * usuário descartar ou refazer a ação que disparou o erro.
 */
@Composable
fun ErrorBanner(
    message: String,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        if (onDismiss != null) {
            TextButton(onClick = onDismiss) {
                Text("Ok", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

/**
 * Placeholder para listas vazias. Mostra ícone, título, descrição e
 * (opcional) botão de ação — ex.: "Nenhuma meta cadastrada / Comece
 * agora / [+ Nova meta]".
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    icon: ImageVector = Icons.Outlined.Info,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

/** Spinner centralizado para estados Loading. */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Texto de uma linha que encolhe a fonte até caber na largura disponível.
 *
 * Usado em cards do dashboard, onde valores monetários longos (ex.:
 * `R$ 1.234.567,89`) eram cortados — aqui a fonte diminui em passos até não
 * estourar, preservando o valor completo em vez de truncar. O conteúdo só é
 * desenhado depois de calibrado para evitar um "salto" visível na primeira
 * composição. O BOM do Compose (1.7.x) ainda não traz o `autoSize` estável,
 * por isso o ajuste é manual via [androidx.compose.ui.text.TextLayoutResult].
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minFontSizeSp: Float = 12f,
) {
    val baseSp = if (style.fontSize.isUnspecified) 16f else style.fontSize.value
    var sizeSp by remember(text, baseSp) { mutableStateOf(baseSp) }
    var readyToDraw by remember(text, baseSp) { mutableStateOf(false) }

    Text(
        text = text,
        style = style.copy(fontSize = sizeSp.sp),
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { result ->
            if (result.didOverflowWidth && sizeSp > minFontSizeSp) {
                sizeSp = (sizeSp * 0.92f).coerceAtLeast(minFontSizeSp)
            } else {
                readyToDraw = true
            }
        },
    )
}
