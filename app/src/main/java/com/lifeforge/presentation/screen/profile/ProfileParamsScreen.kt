package com.lifeforge.presentation.screen.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

/** Categorias do menu "Dados para projeções" (item de cada grupo de campos). */
private enum class ParamCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    ESSENTIALS("Essenciais", "Idade, salário, vínculo, aposentadoria, aporte", Icons.Outlined.Star),
    PERSONAL("Pessoal", "Estado civil, filhos, UF, expectativa de vida", Icons.Outlined.Person),
    PROFESSIONAL("Profissional", "Crescimento salarial e risco de desemprego", Icons.Outlined.Work),
    HOUSING("Moradia", "Situação, parcela/aluguel e valor do imóvel", Icons.Outlined.Home),
    VEHICLES("Veículos", "Valor de mercado dos veículos", Icons.Outlined.DirectionsCar),
    TAX("Tributação", "Regime tributário", Icons.Outlined.Percent),
    WEALTH("Patrimônio e dívidas", "Reserva de emergência e dívidas", Icons.Outlined.Savings),
    PLANNING("Planejamento", "Planos de filhos e de imóvel", Icons.Outlined.FamilyRestroom),
}

/**
 * Tela "Dados para projeções" — coleta os parâmetros opcionais do perfil.
 * Organizada como MENU de categorias: tocar numa categoria abre só os campos
 * dela (lista única ficava longa demais). Tudo é salvo via PUT /profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileParamsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileParamsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    // Categoria aberta; null = menu. Saveable para sobreviver à recriação.
    var openCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val openCategory = openCategoryName?.let { name ->
        ParamCategory.entries.firstOrNull { it.name == name }
    }

    LaunchedEffect(state.message, state.error) {
        val msg = state.message ?: state.error
        if (msg != null) {
            snackbar.showSnackbar(msg)
            viewModel.onMessageShown()
        }
    }

    // Voltar do sistema: dentro de uma categoria, volta primeiro ao menu.
    BackHandler(enabled = openCategory != null) { openCategoryName = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(openCategory?.title ?: "Dados para projeções") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (openCategory != null) openCategoryName = null else onNavigateBack()
                        },
                        enabled = !state.isSaving,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (openCategory != null) {
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

            if (openCategory == null) {
                Text(
                    "Campos opcionais — preencha o que quiser para melhorar a " +
                        "precisão das projeções. Toque numa categoria para abrir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ParamCategory.entries.forEach { category ->
                    CategoryCard(
                        category = category,
                        onClick = { openCategoryName = category.name },
                    )
                }
            } else {
                CategoryFields(
                    category = openCategory,
                    viewModel = viewModel,
                    isSaving = state.isSaving,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CategoryCard(category: ParamCategory, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    category.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Campos da categoria aberta — mesmo conteúdo da antiga lista única. */
@Composable
private fun CategoryFields(
    category: ParamCategory,
    viewModel: ProfileParamsViewModel,
    isSaving: Boolean,
) {
    val state by viewModel.state.collectAsState()
    val form = state.form

    when (category) {
        ParamCategory.ESSENTIALS -> {
            NumberField(form.age, { v -> viewModel.update { it.copy(age = v) } }, "Idade", isSaving)
            CurrencyField(
                value = form.monthlySalary,
                onValueChange = { v -> viewModel.update { it.copy(monthlySalary = sanitizeCurrencyInput(v)) } },
                label = "Salário mensal (R$)",
                enabled = !isSaving,
            )
            OptionalEnumDropdown(
                "Tipo de vínculo", EmploymentType.entries, form.employmentType,
                { sel -> viewModel.update { it.copy(employmentType = sel) } }, { it.label }, !isSaving,
            )
            NumberField(form.retirementAge, { v -> viewModel.update { it.copy(retirementAge = v) } }, "Idade desejada de aposentadoria", isSaving)
            CurrencyField(
                value = form.monthlyContribution,
                onValueChange = { v -> viewModel.update { it.copy(monthlyContribution = sanitizeCurrencyInput(v)) } },
                label = "Aporte mensal (R$)",
                enabled = !isSaving,
            )
        }
        ParamCategory.PERSONAL -> {
            OptionalEnumDropdown(
                "Estado civil", MaritalStatus.entries, form.maritalStatus,
                { sel -> viewModel.update { it.copy(maritalStatus = sel) } }, { it.label }, !isSaving,
            )
            NumberField(form.dependents, { v -> viewModel.update { it.copy(dependents = v) } }, "Filhos / dependentes", isSaving)
            LifeForgeTextField(
                value = form.childrenAges,
                onValueChange = { v ->
                    viewModel.update { it.copy(childrenAges = v.filter { c -> c.isDigit() || c == ',' || c == ' ' }) }
                },
                label = "Idades dos filhos (ex.: 3, 7)",
                enabled = !isSaving,
            )
            LifeForgeTextField(
                value = form.state,
                onValueChange = { v -> viewModel.update { it.copy(state = v.take(2)) } },
                label = "Estado (UF)",
                enabled = !isSaving,
            )
            NumberField(form.lifeExpectancy, { v -> viewModel.update { it.copy(lifeExpectancy = v) } }, "Expectativa de vida (anos)", isSaving)
        }
        ParamCategory.PROFESSIONAL -> {
            LifeForgeTextField(
                value = form.expectedSalaryGrowth,
                onValueChange = { v -> viewModel.update { it.copy(expectedSalaryGrowth = sanitizeCurrencyInput(v)) } },
                label = "Crescimento salarial esperado (% ao ano)",
                keyboardType = KeyboardType.Decimal,
                enabled = !isSaving,
            )
            OptionalEnumDropdown(
                "Risco de desemprego", RiskLevel.entries, form.unemploymentRisk,
                { sel -> viewModel.update { it.copy(unemploymentRisk = sel) } }, { it.label }, !isSaving,
            )
        }
        ParamCategory.HOUSING -> {
            OptionalEnumDropdown(
                "Situação de moradia", HousingStatus.entries, form.housingStatus,
                { sel -> viewModel.update { it.copy(housingStatus = sel) } }, { it.label }, !isSaving,
            )
            CurrencyField(
                value = form.housingMonthlyCost,
                onValueChange = { v -> viewModel.update { it.copy(housingMonthlyCost = sanitizeCurrencyInput(v)) } },
                label = "Parcela / aluguel mensal (R$)",
                enabled = !isSaving,
            )
            CurrencyField(
                value = form.propertyValue,
                onValueChange = { v -> viewModel.update { it.copy(propertyValue = sanitizeCurrencyInput(v)) } },
                label = "Valor do imóvel próprio (R$)",
                enabled = !isSaving,
            )
        }
        ParamCategory.VEHICLES -> {
            CurrencyField(
                value = form.vehiclesValue,
                onValueChange = { v -> viewModel.update { it.copy(vehiclesValue = sanitizeCurrencyInput(v)) } },
                label = "Valor de mercado dos veículos (R$)",
                enabled = !isSaving,
            )
        }
        ParamCategory.TAX -> {
            OptionalEnumDropdown(
                "Regime tributário", TaxRegime.entries, form.taxRegime,
                { sel -> viewModel.update { it.copy(taxRegime = sel) } }, { it.label }, !isSaving,
            )
        }
        ParamCategory.WEALTH -> {
            CurrencyField(
                value = form.emergencyReserve,
                onValueChange = { v -> viewModel.update { it.copy(emergencyReserve = sanitizeCurrencyInput(v)) } },
                label = "Reserva de emergência (R$)",
                enabled = !isSaving,
            )
            CurrencyField(
                value = form.totalDebt,
                onValueChange = { v -> viewModel.update { it.copy(totalDebt = sanitizeCurrencyInput(v)) } },
                label = "Total de dívidas (R$)",
                enabled = !isSaving,
            )
        }
        ParamCategory.PLANNING -> {
            SwitchRow("Pretende ter filhos?", form.plansChildren, { v -> viewModel.update { it.copy(plansChildren = v) } }, !isSaving)
            SwitchRow("Pretende comprar/financiar imóvel?", form.plansProperty, { v -> viewModel.update { it.copy(plansProperty = v) } }, !isSaving)
        }
    }
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
