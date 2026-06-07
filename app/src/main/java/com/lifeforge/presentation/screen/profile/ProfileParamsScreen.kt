package com.lifeforge.presentation.screen.profile

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.EmploymentType
import com.lifeforge.domain.model.HousingStatus
import com.lifeforge.domain.model.MaritalStatus
import com.lifeforge.domain.model.RiskLevel
import com.lifeforge.domain.model.TaxRegime
import com.lifeforge.presentation.common.CurrencyField
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.sanitizeCurrencyInput

/**
 * Tela "Dados para projeções" — coleta os parâmetros opcionais do perfil
 * (Fase 1). Organizada em seções; tudo é salvo via PUT /profile. Quanto mais
 * o usuário preencher, mais precisas ficam as projeções (consumo na Fase 2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileParamsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileParamsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val form = state.form
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.error) {
        val msg = state.message ?: state.error
        if (msg != null) {
            snackbar.showSnackbar(msg)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dados para projeções") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isSaving) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                if (state.isSaving) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving && !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Salvar") }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            Text(
                "Campos opcionais — preencha o que quiser para melhorar a precisão das projeções.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHeader("Essenciais")
            NumberField(form.age, { v -> viewModel.update { it.copy(age = v) } }, "Idade", state.isSaving)
            CurrencyField(
                value = form.monthlySalary,
                onValueChange = { v -> viewModel.update { it.copy(monthlySalary = sanitizeCurrencyInput(v)) } },
                label = "Salário mensal (R$)",
                enabled = !state.isSaving,
            )
            OptionalEnumDropdown(
                "Tipo de vínculo", EmploymentType.entries, form.employmentType,
                { sel -> viewModel.update { it.copy(employmentType = sel) } }, { it.label }, !state.isSaving,
            )
            NumberField(form.retirementAge, { v -> viewModel.update { it.copy(retirementAge = v) } }, "Idade desejada de aposentadoria", state.isSaving)
            CurrencyField(
                value = form.monthlyContribution,
                onValueChange = { v -> viewModel.update { it.copy(monthlyContribution = sanitizeCurrencyInput(v)) } },
                label = "Aporte mensal (R$)",
                enabled = !state.isSaving,
            )

            SectionHeader("Pessoal")
            OptionalEnumDropdown(
                "Estado civil", MaritalStatus.entries, form.maritalStatus,
                { sel -> viewModel.update { it.copy(maritalStatus = sel) } }, { it.label }, !state.isSaving,
            )
            NumberField(form.dependents, { v -> viewModel.update { it.copy(dependents = v) } }, "Filhos / dependentes", state.isSaving)
            LifeForgeTextField(
                value = form.state,
                onValueChange = { v -> viewModel.update { it.copy(state = v.take(2)) } },
                label = "Estado (UF)",
                enabled = !state.isSaving,
            )

            SectionHeader("Profissional")
            LifeForgeTextField(
                value = form.expectedSalaryGrowth,
                onValueChange = { v -> viewModel.update { it.copy(expectedSalaryGrowth = sanitizeCurrencyInput(v)) } },
                label = "Crescimento salarial esperado (% ao ano)",
                keyboardType = KeyboardType.Decimal,
                enabled = !state.isSaving,
            )
            OptionalEnumDropdown(
                "Risco de desemprego", RiskLevel.entries, form.unemploymentRisk,
                { sel -> viewModel.update { it.copy(unemploymentRisk = sel) } }, { it.label }, !state.isSaving,
            )

            SectionHeader("Moradia")
            OptionalEnumDropdown(
                "Situação de moradia", HousingStatus.entries, form.housingStatus,
                { sel -> viewModel.update { it.copy(housingStatus = sel) } }, { it.label }, !state.isSaving,
            )
            CurrencyField(
                value = form.housingMonthlyCost,
                onValueChange = { v -> viewModel.update { it.copy(housingMonthlyCost = sanitizeCurrencyInput(v)) } },
                label = "Parcela / aluguel mensal (R$)",
                enabled = !state.isSaving,
            )

            SectionHeader("Tributação")
            OptionalEnumDropdown(
                "Regime tributário", TaxRegime.entries, form.taxRegime,
                { sel -> viewModel.update { it.copy(taxRegime = sel) } }, { it.label }, !state.isSaving,
            )

            SectionHeader("Patrimônio e dívidas")
            CurrencyField(
                value = form.emergencyReserve,
                onValueChange = { v -> viewModel.update { it.copy(emergencyReserve = sanitizeCurrencyInput(v)) } },
                label = "Reserva de emergência (R$)",
                enabled = !state.isSaving,
            )
            CurrencyField(
                value = form.totalDebt,
                onValueChange = { v -> viewModel.update { it.copy(totalDebt = sanitizeCurrencyInput(v)) } },
                label = "Total de dívidas (R$)",
                enabled = !state.isSaving,
            )

            SectionHeader("Planejamento")
            SwitchRow("Pretende ter filhos?", form.plansChildren, { v -> viewModel.update { it.copy(plansChildren = v) } }, !state.isSaving)
            SwitchRow("Pretende comprar/financiar imóvel?", form.plansProperty, { v -> viewModel.update { it.copy(plansProperty = v) } }, !state.isSaving)

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Spacer(Modifier.height(4.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isSaving: Boolean,
) {
    LifeForgeTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(3)) },
        label = label,
        keyboardType = KeyboardType.Number,
        enabled = !isSaving,
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/**
 * Dropdown para enum OPCIONAL (permite "Não informado"/null). Espelha o
 * [com.lifeforge.presentation.common.EnumDropdown], que só aceita valor não-nulo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionalEnumDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    labelOf: (T) -> String,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected?.let(labelOf) ?: "Não informado",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            enabled = enabled,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Não informado") },
                onClick = { onSelect(null); expanded = false },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelOf(option)) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}
