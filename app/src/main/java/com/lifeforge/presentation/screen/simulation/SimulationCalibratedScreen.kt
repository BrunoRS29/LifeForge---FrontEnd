package com.lifeforge.presentation.screen.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.CalibrationSummary
import com.lifeforge.presentation.common.CurrencyField
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatProbability
import com.lifeforge.presentation.common.sanitizeCurrencyInput

/**
 * Tela de Simulacao Calibrada por IA (Sprint 5).
 *
 * Diferencas vs SimulationScreen (Sprint 2):
 *  - Subtitulo e descricao explicam o que muda
 *  - Form sem `monthlyContribution` (sera derivado)
 *  - Indicador de progresso mais detalhado (loading de 3-6s)
 *  - Apos sucesso, mostra primeiro o CalibrationSummaryCard explicando
 *    a derivacao do aporte, DEPOIS os graficos do Monte Carlo
 *
 * Reutiliza o [ResultSection] da SimulationScreen (mesmo pacote
 * `com.lifeforge.presentation.screen.simulation`) para nao duplicar o
 * codigo pesado de Vico. Importante: a visibilidade do `ResultSection`
 * em SimulationScreen.kt precisa ser `internal` (nao `private`) para
 * que esta tela consiga referencia-lo. Mudanca de UMA linha la.
 *
 * IMPORTANTE: NAO definimos um wrapper `private fun ResultSection` aqui -
 * isso geraria ambiguidade com o ResultSection do SimulationScreen.kt
 * (mesmo pacote, mesma assinatura, Kotlin nao consegue resolver).
 * Chamamos direto a funcao do mesmo pacote.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationCalibratedScreen(
    onNavigateBack: () -> Unit,
    viewModel: SimulationCalibratedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.goalName?.let { "IA — $it" } ?: "Simular com IA",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isRunning) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.errorBanner != null) {
                    ErrorBanner(
                        message = state.errorBanner!!,
                        onDismiss = viewModel::onErrorBannerDismiss,
                    )
                }

                AiCalloutCard()

                CalibratedParameterForm(
                    form = state.form,
                    isRunning = state.isRunning,
                    onChange = viewModel::onFormChange,
                )

                Button(
                    onClick = viewModel::runCalibrated,
                    enabled = !state.isRunning && state.form.canRun,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.isRunning) {
                            state.progressMessage ?: "Calibrando..."
                        } else "Simular com IA"
                    )
                }

                state.result?.let { result ->
                    Spacer(Modifier.height(8.dp))
                    CalibrationSummaryCard(summary = result.calibration)
                    Spacer(Modifier.height(8.dp))
                    // Chamada DIRETA ao ResultSection do SimulationScreen.kt
                    // (mesmo pacote). Requer visibilidade `internal` la.
                    ResultSection(result = result.simulation)
                }
            }
        }
    }
}

// ============================================================================
// AI callout
// ============================================================================

@Composable
private fun AiCalloutCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Column {
                Text(
                    text = "Tudo calibrado pelo seu perfil",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Voce nao estima nada de mercado: a IA preve sua renda e " +
                        "despesas pelo seu historico (e calcula o aporte mensal), e o " +
                        "retorno, a volatilidade, a inflacao e o risco de desemprego vem " +
                        "da base de referencia calibrada ao seu perfil de risco e vinculo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// ============================================================================
// Form
// ============================================================================

@Composable
private fun CalibratedParameterForm(
    form: CalibratedSimulationForm,
    isRunning: Boolean,
    onChange: ((CalibratedSimulationForm) -> CalibratedSimulationForm) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Parametros da simulacao", style = MaterialTheme.typography.titleMedium)

            CurrencyField(
                value = form.initialCapitalInput,
                onValueChange = { v ->
                    onChange { it.copy(initialCapitalInput = sanitizeCurrencyInput(v)) }
                },
                label = "Capital inicial (R$)",
                enabled = !isRunning,
            )

            // monthlyContribution AUSENTE intencionalmente (derivado pela IA).
            // Premissas de mercado (retorno, volatilidade, inflacao, desemprego)
            // tambem NAO sao digitadas: o backend as calibra pelo seu perfil.

            CurrencyField(
                value = form.targetAmountInput,
                onValueChange = { v ->
                    onChange { it.copy(targetAmountInput = sanitizeCurrencyInput(v)) }
                },
                label = "Valor alvo (R$)",
                enabled = !isRunning,
            )

            CurrencyField(
                value = form.horizonMonthsInput,
                onValueChange = { v ->
                    onChange { it.copy(horizonMonthsInput = v.filter(Char::isDigit)) }
                },
                label = "Horizonte (meses)",
                enabled = !isRunning,
            )
        }
    }
}

// ============================================================================
// CalibrationSummaryCard
// ============================================================================

/**
 * Cartao que explica COMO a IA derivou os parametros calibrados.
 * Requisito direto do TCC: transparencia em decisoes assistidas por ML.
 *
 * Estrutura:
 *  - Renda projetada (verde)
 *  - (-) Despesa projetada (vermelha)
 *  - = Aporte derivado (destaque)
 *  - Aviso quando capping zerou a contribuicao
 */
@Composable
fun CalibrationSummaryCard(summary: CalibrationSummary) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Como a IA calibrou os parametros",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            HorizontalDivider()

            CalibrationLine(
                label = "Renda mensal projetada",
                value = formatBrl(summary.predictedMonthlyIncome),
            )
            CalibrationLine(
                label = "(−) Despesa mensal projetada",
                value = formatBrl(summary.predictedMonthlyExpense),
            )

            HorizontalDivider()

            CalibrationLine(
                label = "(=) Aporte derivado",
                value = formatBrl(summary.appliedMonthlyContribution),
                highlight = true,
            )

            if (summary.cappedToZero) {
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Despesa maior que renda: aporte zerado " +
                            "(${formatBrl(summary.rawMonthlyContribution)} antes do " +
                            "ajuste).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Volatilidade aplicada: ${formatProbability(summary.appliedVolatilityAnnual)} " +
                    "(combina mercado + variacao da renda)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "Predicao de renda #${summary.incomePredictionId} • " +
                    "Predicao de despesa #${summary.expensePredictionId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalibrationLine(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
