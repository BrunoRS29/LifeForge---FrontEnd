package com.lifeforge.presentation.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.usecase.FinancialSnapshot
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
            TopAppBar(
                title = { Text("LifeForge") },
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !state.isRefreshing,
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Progress bar fina no topo durante refresh.
            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

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
                state.snapshot?.let { snapshot ->
                    WealthProjectionCard(snapshot = snapshot)
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
    // 4 cards em grid 2x2. Quando snapshot é null (carregando),
    // mostramos os mesmos cards com "—" — evita layout shift quando
    // os dados chegam.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "Patrimônio total",
                value = snapshot?.totalAssets?.let(::formatBrl) ?: "—",
                icon = Icons.Outlined.AccountBalanceWallet,
                accent = MaterialTheme.colorScheme.primary,
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "Taxa de poupança",
                value = snapshot?.savingsRate?.let(::formatPercent) ?: "—",
                icon = Icons.Outlined.Savings,
                accent = MaterialTheme.colorScheme.secondary,
            )
        }
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
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
