package com.lifeforge.presentation.screen.optimization

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.AssetType
import com.lifeforge.domain.model.OptimizationResult
import com.lifeforge.domain.model.RebalanceResult
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.presentation.common.CurrencyField
import com.lifeforge.presentation.common.EnumDropdown
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatProbability
import com.lifeforge.presentation.common.label

/**
 * Tela de Otimização — três modos:
 *
 * - **Aporte**: dado horizonte e meta, qual aporte mensal mínimo?
 * - **Horizonte**: dado aporte e meta, em quantos meses chega?
 * - **Rebalancear**: dado perfil de risco, qual mix de ativos sugerido?
 *
 * Cada modo tem form próprio e card de resultado. Resultados ficam
 * cachados no ViewModel — trocar de aba não apaga o resultado anterior.
 *
 * Os valores default dos forms são pré-populados com cenários típicos
 * brasileiros (Selic ~8%, vol Ibov ~15-20%) para o usuário poder
 * apertar "Calcular" imediatamente e ver o resultado, sem precisar
 * preencher tudo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizationScreen(viewModel: OptimizationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Otimizar") }) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.isRunning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                TabRow(selectedTabIndex = state.selectedMode.ordinal) {
                    OptimizationMode.entries.forEach { mode ->
                        Tab(
                            selected = state.selectedMode == mode,
                            onClick = { viewModel.selectMode(mode) },
                            text = { Text(mode.label) },
                        )
                    }
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

                    when (state.selectedMode) {
                        OptimizationMode.CONTRIBUTION -> ContributionSection(
                            form = state.contributionForm,
                            result = state.contributionResult,
                            isRunning = state.isRunning,
                            onChange = viewModel::onContributionForm,
                            onRun = viewModel::runContribution,
                        )
                        OptimizationMode.HORIZON -> HorizonSection(
                            form = state.horizonForm,
                            result = state.horizonResult,
                            isRunning = state.isRunning,
                            onChange = viewModel::onHorizonForm,
                            onRun = viewModel::runHorizon,
                        )
                        OptimizationMode.REBALANCE -> RebalanceSection(
                            form = state.rebalanceForm,
                            result = state.rebalanceResult,
                            isRunning = state.isRunning,
                            onChange = viewModel::onRebalanceForm,
                            onRun = viewModel::runRebalance,
                        )
                    }
                }
            }
        }
    }
}

private val OptimizationMode.label: String
    get() = when (this) {
        OptimizationMode.CONTRIBUTION -> "Aporte"
        OptimizationMode.HORIZON -> "Horizonte"
        OptimizationMode.REBALANCE -> "Carteira"
    }

// ============================================================================
// Contribution
// ============================================================================

@Composable
private fun ContributionSection(
    form: ContributionForm,
    result: OptimizationResult?,
    isRunning: Boolean,
    onChange: ((ContributionForm) -> ContributionForm) -> Unit,
    onRun: () -> Unit,
) {
    Text(
        "Calcule o aporte mensal mínimo para atingir uma meta no horizonte definido.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    CurrencyField(
        value = form.initialCapital,
        onValueChange = { v -> onChange { it.copy(initialCapital = v.asMonetaryInput()) } },
        label = "Capital inicial (R$)",
        enabled = !isRunning,
    )
    CurrencyField(
        value = form.targetAmount,
        onValueChange = { v -> onChange { it.copy(targetAmount = v.asMonetaryInput()) } },
        label = "Meta (R$)",
        enabled = !isRunning,
    )
    IntField(
        value = form.horizonMonths,
        onValueChange = { v -> onChange { it.copy(horizonMonths = v) } },
        label = "Horizonte (meses)",
        enabled = !isRunning,
    )
    CurrencyField(
        value = form.expectedReturnAnnual,
        onValueChange = { v -> onChange { it.copy(expectedReturnAnnual = v.asMonetaryInput()) } },
        label = "Retorno esperado anual (ex.: 0,08)",
        enabled = !isRunning,
    )
    CurrencyField(
        value = form.volatilityAnnual,
        onValueChange = { v -> onChange { it.copy(volatilityAnnual = v.asMonetaryInput()) } },
        label = "Volatilidade anual (ex.: 0,15)",
        enabled = !isRunning,
    )
    CurrencyField(
        value = form.targetSuccessProbability,
        onValueChange = { v -> onChange { it.copy(targetSuccessProbability = v.asMonetaryInput()) } },
        label = "Probabilidade de sucesso (ex.: 0,90)",
        imeAction = ImeAction.Done,
        enabled = !isRunning,
    )

    Button(
        onClick = onRun,
        enabled = !isRunning && form.canRun,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Calcular aporte mínimo")
    }

    if (result != null) {
        OptimizationResultCard(
            primaryLabel = "Aporte mensal necessário",
            primaryValue = formatBrl(result.optimalValue),
            result = result,
        )
    }
}

// ============================================================================
// Horizon
// ============================================================================

@Composable
private fun HorizonSection(
    form: HorizonForm,
    result: OptimizationResult?,
    isRunning: Boolean,
    onChange: ((HorizonForm) -> HorizonForm) -> Unit,
    onRun: () -> Unit,
) {
    Text(
        "Dado um aporte mensal, calcule em quantos meses a meta é atingida.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    CurrencyField(
        value = form.initialCapital,
        onValueChange = { v -> onChange { it.copy(initialCapital = v.asMonetaryInput()) } },
        label = "Capital inicial (R$)",
        enabled = !isRunning,
    )
    CurrencyField(
        value = form.monthlyContribution,
        onValueChange = { v -> onChange { it.copy(monthlyContribution = v.asMonetaryInput()) } },
        label = "Aporte mensal (R$)",
        enabled = !isRunning,
    )
    CurrencyField(
        value = form.targetAmount,
        onValueChange = { v -> onChange { it.copy(targetAmount = v.asMonetaryInput()) } },
        label = "Meta (R$)",
        enabled = !isRunning,
    )
    CurrencyField(
        value = form.expectedReturnAnnual,
        onValueChange = { v -> onChange { it.copy(expectedReturnAnnual = v.asMonetaryInput()) } },
        label = "Retorno esperado anual",
        enabled = !isRunning,
    )
    CurrencyField(
        value = form.volatilityAnnual,
        onValueChange = { v -> onChange { it.copy(volatilityAnnual = v.asMonetaryInput()) } },
        label = "Volatilidade anual",
        enabled = !isRunning,
    )
    CurrencyField(
        value = form.targetSuccessProbability,
        onValueChange = { v -> onChange { it.copy(targetSuccessProbability = v.asMonetaryInput()) } },
        label = "Probabilidade de sucesso",
        imeAction = ImeAction.Done,
        enabled = !isRunning,
    )

    Button(
        onClick = onRun,
        enabled = !isRunning && form.canRun,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Calcular horizonte")
    }

    if (result != null) {
        val months = result.optimalValue.toInt()
        val years = months / 12
        val remainingMonths = months % 12
        val period = buildString {
            if (years > 0) append("$years ano${if (years > 1) "s" else ""}")
            if (years > 0 && remainingMonths > 0) append(" e ")
            if (remainingMonths > 0) append("$remainingMonths mês${if (remainingMonths > 1) "es" else ""}")
            if (years == 0 && remainingMonths == 0) append("Menos de 1 mês")
        }
        OptimizationResultCard(
            primaryLabel = "Tempo necessário",
            primaryValue = period,
            result = result,
        )
    }
}

// ============================================================================
// Rebalance
// ============================================================================

@Composable
private fun RebalanceSection(
    form: RebalanceForm,
    result: RebalanceResult?,
    isRunning: Boolean,
    onChange: ((RebalanceForm) -> RebalanceForm) -> Unit,
    onRun: () -> Unit,
) {
    Text(
        "Sugestão de mix de ativos baseado no seu perfil de risco e horizonte.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    EnumDropdown(
        label = "Perfil de risco",
        options = RiskProfile.entries,
        selected = form.riskProfile,
        onSelect = { p -> onChange { it.copy(riskProfile = p) } },
        labelOf = RiskProfile::label,
        enabled = !isRunning,
    )
    CurrencyField(
        value = form.currentCapital,
        onValueChange = { v -> onChange { it.copy(currentCapital = v.asMonetaryInput()) } },
        label = "Capital disponível (R$)",
        enabled = !isRunning,
    )
    CurrencyField(
        value = form.targetAmount,
        onValueChange = { v -> onChange { it.copy(targetAmount = v.asMonetaryInput()) } },
        label = "Meta (R$)",
        enabled = !isRunning,
    )
    IntField(
        value = form.monthsToGoal,
        onValueChange = { v -> onChange { it.copy(monthsToGoal = v) } },
        label = "Horizonte (meses)",
        enabled = !isRunning,
        imeAction = ImeAction.Done,
    )

    Button(
        onClick = onRun,
        enabled = !isRunning && form.canRun,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Sugerir carteira")
    }

    if (result != null) {
        RebalanceResultCard(result = result)
    }
}

// ============================================================================
// Result cards
// ============================================================================

@Composable
private fun OptimizationResultCard(
    primaryLabel: String,
    primaryValue: String,
    result: OptimizationResult,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.feasible) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                if (result.feasible) "Resultado" else "Não viável",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(primaryLabel, style = MaterialTheme.typography.labelMedium)
            Text(primaryValue, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Probabilidade atingida: ${formatProbability(result.achievedProbability)} " +
                    "(alvo: ${formatProbability(result.targetProbability)})",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Iterações: ${result.iterations.size} · Tempo: ${result.executionTimeMs} ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RebalanceResultCard(result: RebalanceResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Carteira sugerida", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            result.weights.toList()
                .sortedByDescending { it.second }
                .forEach { (assetType, weight) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            assetType.label(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            formatProbability(weight),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

            Spacer(Modifier.height(12.dp))
            Text(
                "Retorno esperado: ${formatProbability(result.expectedReturnAnnual)} a.a. · " +
                    "Vol: ${formatProbability(result.volatilityAnnual)} a.a.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (result.rationale.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(result.rationale, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ============================================================================
// Helpers
// ============================================================================

@Composable
private fun IntField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
        singleLine = true,
        enabled = enabled,
        modifier = modifier,
    )
}
