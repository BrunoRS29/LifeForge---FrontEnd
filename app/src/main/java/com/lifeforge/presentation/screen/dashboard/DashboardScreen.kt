package com.lifeforge.presentation.screen.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.RecurringPattern
import com.lifeforge.domain.usecase.FinancialSnapshot
import com.lifeforge.presentation.common.AutoSizeText
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatPercent

/**
 * Dashboard — visão geral consolidada do usuário.
 *
 * Layout: saudação + barra de progresso (durante refresh) + 4 cards
 * com métricas-chave. Em Sprint posterior podemos adicionar:
 * - Lista de top 3 metas com progresso individual
 * - Gráfico de evolução do patrimônio (precisa de histórico — fora do MVP)
 * - Alertas (taxa de poupança baixa, meta próxima do prazo, etc.)
 *
 * O `verticalScroll` permite que o conteúdo cresça sem quebrar layout
 * em telas pequenas conforme adicionarmos seções.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("LifeForge") })
        },
    ) { padding ->
        // Atualização por gesto (puxar para baixo) — padrão dos apps atuais;
        // o indicador circular do Material 3 substitui o botão de refresh.
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Greeting(name = state.user?.name)

                if (state.errorBanner != null) {
                    ErrorBanner(
                        message = state.errorBanner!!,
                        onDismiss = viewModel::onErrorBannerDismiss,
                    )
                }

                FinancialSnapshotSection(snapshot = state.snapshot)

                // Evolução patrimonial real (hoje) × projetada — Seção 8.4 do TCC.
                // Personalizada pelo perfil (horizonte, salário, inflação, retorno).
                state.snapshot?.let { snapshot ->
                    WealthProjectionCard(
                        snapshot = snapshot,
                        profile = state.profile,
                        riskProfile = state.user?.riskProfile,
                        referenceData = state.referenceData,
                    )
                    FinancialIndependenceCard(
                        snapshot = snapshot,
                        referenceData = state.referenceData,
                    )
                }

                // Recorrências detectadas automaticamente no histórico.
                state.snapshot?.let { snapshot ->
                    if (snapshot.recurringIncomes.isNotEmpty() ||
                        snapshot.recurringExpenses.isNotEmpty()
                    ) {
                        RecurringPatternsCard(snapshot = snapshot)
                    }
                }
            }
        }
    }
}

@Composable
private fun Greeting(name: String?) {
    Column {
        Text(
            text = "Olá${if (name != null) ", ${name.firstName()}" else ""}!",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Aqui está o resumo das suas finanças.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Pega o primeiro nome para a saudação. "Gabriel Souza" → "Gabriel". */
private fun String.firstName(): String = trim().substringBefore(' ')

@Composable
private fun FinancialSnapshotSection(snapshot: FinancialSnapshot?) {
    // Hierarquia moderna: card-herói com o patrimônio (a informação mais
    // importante) em destaque com gradiente, e abaixo as métricas do mês.
    // Quando snapshot é null (carregando), mostramos "—" — evita layout
    // shift quando os dados chegam.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WealthHeroCard(
            totalAssets = snapshot?.totalAssets?.let(::formatBrl) ?: "—",
            savingsRate = snapshot?.savingsRate?.let(::formatPercent),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "Receita mensal",
                value = snapshot?.monthlyIncome?.let(::formatBrl) ?: "—",
                icon = Icons.Outlined.TrendingUp,
                accent = MaterialTheme.colorScheme.primary,
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "Despesa mensal",
                value = snapshot?.monthlyExpenses?.let(::formatBrl) ?: "—",
                icon = Icons.Outlined.TrendingDown,
                accent = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

/**
 * Card-herói do dashboard: patrimônio total sobre um gradiente suave
 * (primaryContainer → tertiaryContainer) com a taxa de poupança num
 * "chip" translúcido. É o ponto focal visual da tela.
 */
@Composable
private fun WealthHeroCard(totalAssets: String, savingsRate: String?) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
        ),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(gradient)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Patrimônio total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        AutoSizeText(
            text = totalAssets,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth(),
        )
        if (savingsRate != null) {
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Savings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.height(16.dp),
                    )
                    Text(
                        "Taxa de poupança: $savingsRate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            AutoSizeText(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RecurringPatternsCard(snapshot: FinancialSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Recorrências detectadas", style = MaterialTheme.typography.titleMedium)
            Text(
                "Identificadas no seu histórico (itens que aparecem em 3+ meses).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (snapshot.recurringIncomes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Receitas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                snapshot.recurringIncomes.take(5).forEach { RecurringRow(it, isIncome = true) }
            }
            if (snapshot.recurringExpenses.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Despesas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                snapshot.recurringExpenses.take(5).forEach { RecurringRow(it, isIncome = false) }
            }
        }
    }
}

@Composable
private fun RecurringRow(pattern: RecurringPattern, isIncome: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                pattern.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${pattern.months} meses",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatBrl(pattern.monthlyAmount) + "/mês",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isIncome) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            textAlign = TextAlign.End,
        )
    }
}
