package com.lifeforge.presentation.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.usecase.FinancialSnapshot
import com.lifeforge.presentation.common.AutoSizeText
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.RefreshableBox
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.ShapeIcon
import com.lifeforge.presentation.common.TabTopAppBar
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatPercent
import com.lifeforge.presentation.common.readableWidth
import java.math.BigDecimal

/**
 * Dashboard — visão geral consolidada do usuário.
 *
 * A saudação é o título grande da barra superior, que recolhe ao rolar. Abaixo:
 * o card-herói do patrimônio, as métricas do mês, a saúde das metas, a evolução
 * patrimonial realizada × projetada, o índice de independência financeira, o
 * atalho para a inteligência preditiva e as recorrências detectadas. Tudo é lido
 * do banco local — o painel funciona sem conexão; puxar para baixo atualiza.
 */
@Composable
fun DashboardScreen(
    onOpenPredictions: () -> Unit = {},
    onOpenGoal: (Long) -> Unit = {},
    onSeeAllGoals: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TabTopAppBar(
                title = greeting(state.user?.name),
                subtitle = "Aqui está o resumo das suas finanças",
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        RefreshableBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .readableWidth()
                    .padding(horizontal = ScreenPadding)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (state.errorBanner != null) {
                    ErrorBanner(
                        message = state.errorBanner!!,
                        onDismiss = viewModel::onErrorBannerDismiss,
                    )
                }

                FinancialSnapshotSection(snapshot = state.snapshot)

                // Resumo das metas ativas com o indicador de saúde (proposta 8.4).
                GoalsHealthSection(
                    goals = state.goalsHealth,
                    onOpenGoal = onOpenGoal,
                    onSeeAll = onSeeAllGoals,
                )

                // Evolução patrimonial realizada × projetada — proposta 8.4 / TCC 4.8.
                // Personalizada pelo perfil (horizonte, salário, inflação, retorno).
                state.snapshot?.let { snapshot ->
                    WealthProjectionCard(
                        snapshot = snapshot,
                        profile = state.profile,
                        riskProfile = state.user?.riskProfile,
                        referenceData = state.referenceData,
                        realizedWealth = state.realizedWealth,
                    )
                    FinancialIndependenceCard(
                        snapshot = snapshot,
                        referenceData = state.referenceData,
                    )
                }

                // Inteligência preditiva (microsserviço de IA): renda, despesas e patrimônio.
                PredictionsEntryCard(onClick = onOpenPredictions)

                // Recorrências detectadas automaticamente no histórico.
                state.snapshot?.let { snapshot ->
                    if (snapshot.recurringIncomes.isNotEmpty() || snapshot.recurringExpenses.isNotEmpty()) {
                        RecurringPatternsSection(snapshot = snapshot)
                    }
                }
            }
        }
    }
}

/** "Olá, Gabriel!" — só o primeiro nome; sem nome, "Olá!". */
internal fun greeting(name: String?): String {
    val first = name?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
    return if (first != null) "Olá, $first!" else "Olá!"
}

@Composable
private fun FinancialSnapshotSection(snapshot: FinancialSnapshot?) {
    // Quando snapshot é null (carregando), mostramos "—": sem salto de layout
    // quando os dados chegam.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WealthHeroCard(snapshot = snapshot)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                modifier = Modifier.weight(1f),
                label = "Receita mensal",
                value = snapshot?.monthlyIncome?.let(::formatBrl) ?: "—",
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                iconContent = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            MetricTile(
                modifier = Modifier.weight(1f),
                label = "Despesa mensal",
                value = snapshot?.monthlyExpenses?.let(::formatBrl) ?: "—",
                icon = Icons.AutoMirrored.Outlined.TrendingDown,
                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                iconContent = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/**
 * Card-herói: o patrimônio total em destaque (tipografia enfatizada), com a taxa
 * de poupança e a sobra do mês em pílulas. Uma forma do Material ao fundo dá a
 * assinatura visual do Material 3 Expressive sem competir com o número.
 */
@Composable
private fun WealthHeroCard(snapshot: FinancialSnapshot?) {
    val totalAssets = snapshot?.totalAssets?.let(::formatBrl) ?: "—"
    val savingsRate = snapshot?.savingsRate?.let(::formatPercent)
    val monthlyBalance = snapshot?.let { it.monthlyIncome - it.monthlyExpenses }
    val decoration = MaterialShapes.Cookie12Sided.toShape()
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            // matchParentSize: a forma decorativa não entra na medição do card.
            Box(Modifier.matchParentSize()) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 56.dp, y = (-48).dp)
                        .size(200.dp)
                        .clip(decoration)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null)
                    Text("Patrimônio total", style = MaterialTheme.typography.labelLarge)
                }
                AutoSizeText(
                    text = totalAssets,
                    style = MaterialTheme.typography.displaySmallEmphasized,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
                // As pílulas quebram de linha inteiras quando não cabem lado a lado.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (savingsRate != null) {
                        HeroPill(icon = Icons.Outlined.Savings, text = "Poupança $savingsRate")
                    }
                    if (monthlyBalance != null) {
                        HeroPill(icon = null, text = "Sobra ${signedBrl(monthlyBalance)}/mês")
                    }
                }
            }
        }
    }
}

/** "+R$ 3.373,56" / "−R$ 120,00": o sinal à frente, como em extratos. */
internal fun signedBrl(value: BigDecimal): String =
    if (value.signum() < 0) "−" + formatBrl(value.negate()) else "+" + formatBrl(value)

@Composable
private fun HeroPill(icon: ImageVector?, text: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    icon: ImageVector,
    iconContainer: Color,
    iconContent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShapeIcon(
                icon = icon,
                containerColor = iconContainer,
                contentColor = iconContent,
                shape = MaterialShapes.Cookie4Sided.toShape(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AutoSizeText(
                    text = value,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
