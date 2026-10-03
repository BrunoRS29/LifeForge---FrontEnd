package com.lifeforge.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
 * - ícone em contêiner com forma do Material ([ShapeIcon]);
 * - seções de formulário ([FormSection]) e ações fixas na base ([BottomActionBar]);
 * - escolha única com botões conectados ([ConnectedChoice]);
 * - cartões de conteúdo ([ContentCard]) e seções recolhíveis ([ExpandableSection]).
 */

/** Margem lateral padrão em telas compactas (diretriz de layout: 16 dp). */
val ScreenPadding: Dp = 16.dp

/** Largura máxima de formulários: campos longos demais atrapalham a leitura. */
val FormMaxWidth: Dp = 640.dp

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
    navigationEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
    val navigationIcon: @Composable () -> Unit = {
        IconButton(onClick = onNavigateBack, enabled = navigationEnabled) {
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

/** Variante visual dos botões de ação de tela. */
enum class ActionEmphasis { Primary, Tonal, Outlined }

/**
 * Botão de ação de tela, tamanho médio (56 dp) do Material 3 Expressive: a forma
 * se transforma ao toque (cantos que se fecham) e o alvo é generoso — as ações
 * principais ficam fáceis de alcançar. Use [ActionEmphasis] para a hierarquia:
 * uma ação primária por tela; as demais, tonal ou contorno.
 */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    emphasis: ActionEmphasis = ActionEmphasis.Primary,
) {
    val height = ButtonDefaults.MediumContainerHeight
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)))
            Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
        }
        Text(text, style = ButtonDefaults.textStyleFor(height), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    val sized = modifier.heightIn(min = height)
    val padding = ButtonDefaults.contentPaddingFor(height)
    when (emphasis) {
        ActionEmphasis.Primary -> Button(
            onClick = onClick,
            shapes = ButtonDefaults.shapes(),
            modifier = sized,
            enabled = enabled,
            contentPadding = padding,
            content = content,
        )
        ActionEmphasis.Tonal -> FilledTonalButton(
            onClick = onClick,
            shapes = ButtonDefaults.shapes(),
            modifier = sized,
            enabled = enabled,
            contentPadding = padding,
            content = content,
        )
        ActionEmphasis.Outlined -> OutlinedButton(
            onClick = onClick,
            shapes = ButtonDefaults.shapes(),
            modifier = sized,
            enabled = enabled,
            contentPadding = padding,
            content = content,
        )
    }
}

/**
 * Seção de formulário: cabeçalho (com explicação opcional) e os campos logo
 * abaixo. Agrupar os campos por assunto encurta a leitura de formulários longos.
 */
@Composable
fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = title, supporting = supporting, action = action)
        content()
    }
}

/**
 * Barra de ações fixa na base da tela, na zona de alcance do polegar. Fica acima
 * da barra de navegação do sistema e do teclado, para que "Salvar" continue
 * visível enquanto o formulário é preenchido.
 */
@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .readableWidth()
                .padding(horizontal = ScreenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/**
 * Escolha única entre poucas opções (2 a 4) com os botões conectados do Material 3
 * Expressive: a opção marcada vira pílula com a cor de destaque, e a forma reage
 * ao toque. Usado quando todas as opções cabem lado a lado (tema, perfil de risco,
 * quantidade de cenários, modo da otimização).
 */
@Composable
fun <T> ConnectedChoice(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ((T) -> ImageVector)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = option == selected,
                onCheckedChange = { onSelect(option) },
                enabled = enabled,
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.RadioButton },
            ) {
                if (icon != null) {
                    Icon(icon(option), contentDescription = null, modifier = Modifier.size(ToggleButtonDefaults.IconSize))
                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                }
                Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * Cartão de conteúdo (gráficos, resumos, explicações): contêiner tonal baixo e
 * cantos extragrandes, com cabeçalho de seção opcional.
 */
@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    supporting: String? = null,
    action: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (title != null) SectionHeader(title = title, supporting = supporting, action = action)
            content()
        }
    }
}

/**
 * Seção recolhível (divulgação progressiva): fechada, mostra só o resumo — para
 * opções avançadas que já vêm bem preenchidas e raramente precisam de ajuste.
 */
@Composable
fun ExpandableSection(
    title: String,
    summary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "expand-chevron",
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = if (expanded) "Recolher" else "Expandir") {
                        onExpandedChange(!expanded)
                    }
                    .semantics { stateDescription = if (expanded) "Aberta" else "Fechada" }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotation),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}

/**
 * Barra fixa com a ação principal de um cálculo (simular, otimizar). Durante a
 * execução, o botão diz o que está rodando e um indicador ondulado (Material 3
 * Expressive) sinaliza o trabalho em andamento.
 */
@Composable
fun ProgressActionBar(
    isRunning: Boolean,
    runningText: String,
    idleText: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
) {
    BottomActionBar {
        if (isRunning) {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        ActionButton(
            text = if (isRunning) runningText else idleText,
            icon = icon,
            onClick = onClick,
            enabled = enabled && !isRunning,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
