package com.lifeforge.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Peças do design system do LifeForge (Material 3 Expressive), usadas por todas as
 * telas para que espaçamentos, barras e listas sejam consistentes:
 *
 * - largura de leitura em telas grandes ([readableWidth]);
 * - barras superiores que recolhem com o scroll ([TabTopAppBar], [DetailTopAppBar]);
 * - cabeçalho de seção ([SectionHeader]);
 * - listas segmentadas, no estilo das configurações do Android ([ListGroup]);
 * - puxar para atualizar com o indicador expressivo ([RefreshableBox]);
 * - ícone em contêiner com forma do Material ([ShapeIcon]).
 */

/** Margem lateral padrão em telas compactas (diretriz de layout: 16 dp). */
val ScreenPadding: Dp = 16.dp

/**
 * Largura máxima do conteúdo. Em tablets e paisagem o conteúdo não se estica
 * por toda a tela (linhas de 45–75 caracteres; campos e botões sem esticar).
 */
val MaxContentWidth: Dp = 840.dp

/** Centraliza o conteúdo e limita a largura a [max] em janelas largas. */
fun Modifier.readableWidth(max: Dp = MaxContentWidth): Modifier =
    fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = max)

/**
 * Barra superior das abas principais: título grande, com subtítulo opcional, que
 * recolhe para a barra compacta ao rolar (o status bar ganha a cor de contêiner
 * enquanto há conteúdo por baixo).
 */
@Composable
fun TabTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    LargeFlexibleTopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        subtitle = subtitle?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

/** Barra superior das telas de detalhe e formulários: título e voltar. */
@Composable
fun DetailTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    subtitle: String? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
    val navigationIcon: @Composable () -> Unit = {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
        }
    }
    val titleContent: @Composable () -> Unit = {
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (subtitle != null) {
        TopAppBar(
            title = titleContent,
            subtitle = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = colors,
        )
    } else {
        TopAppBar(
            title = titleContent,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = colors,
        )
    }
}

/** Título de seção (com explicação e ação opcionais), marcado como cabeçalho para o TalkBack. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        action?.invoke()
    }
}

/** Ícone dentro de um contêiner com forma do Material (destaque discreto em listas e cards). */
@Composable
fun ShapeIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    shape: Shape = MaterialShapes.Cookie6Sided.toShape(),
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(size * 0.55f))
    }
}

/** Item de uma [ListGroup]. `trailing` substitui a seta padrão (ex.: um Switch). */
class GroupItem(
    val headline: String,
    val supporting: String? = null,
    val icon: ImageVector? = null,
    val onClick: (() -> Unit)? = null,
    val trailing: (@Composable () -> Unit)? = null,
    val destructive: Boolean = false,
)

/**
 * Lista segmentada do Material 3 Expressive (como nas configurações do Android):
 * os itens de um grupo ficam em contêineres separados por 2 dp, com os cantos
 * externos mais arredondados — agrupamento visual sem divisórias.
 */
@Composable
fun ListGroup(
    items: List<GroupItem>,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 8.dp)
                    .semantics { heading() },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            items.forEachIndexed { index, item ->
                val contentColor =
                    if (item.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                SegmentedListItem(
                    onClick = item.onClick ?: {},
                    enabled = item.onClick != null || item.trailing != null,
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = items.size),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = contentColor,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        disabledContentColor = contentColor,
                    ),
                    leadingContent = item.icon?.let { icon ->
                        {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (item.destructive) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    },
                    supportingContent = item.supporting?.let { text ->
                        { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    },
                    trailingContent = item.trailing ?: if (item.onClick != null && !item.destructive) {
                        {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        null
                    },
                ) {
                    Text(item.headline)
                }
            }
        }
    }
}

/** Puxar para atualizar com o indicador de carregamento expressivo do Material. */
@Composable
fun RefreshableBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
        content = content,
    )
}
