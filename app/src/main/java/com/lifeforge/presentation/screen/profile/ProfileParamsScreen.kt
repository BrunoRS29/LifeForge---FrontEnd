package com.lifeforge.presentation.screen.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.EmploymentType
import com.lifeforge.domain.model.HousingStatus
import com.lifeforge.domain.model.MaritalStatus
import com.lifeforge.domain.model.RiskLevel
import com.lifeforge.domain.model.TaxRegime
import com.lifeforge.presentation.common.DetailTopAppBar
import com.lifeforge.presentation.common.FormMaxWidth
import com.lifeforge.presentation.common.GroupItem
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.ListGroup
import com.lifeforge.presentation.common.MoneyField
import com.lifeforge.presentation.common.PercentField
import com.lifeforge.presentation.common.ProgressActionBar
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.ToggleRow
import com.lifeforge.presentation.common.readableWidth
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
 * Organizada como MENU de categorias (lista segmentada): tocar numa categoria
 * abre só os campos dela, com "Salvar" fixo na base. Tudo é salvo via PUT /profile.
 */
@Composable
fun ProfileParamsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileParamsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // Categoria aberta; null = menu. Saveable para sobreviver à recriação.
    var openCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val openCategory = openCategoryName?.let { name -> ParamCategory.entries.firstOrNull { it.name == name } }

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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopAppBar(
                title = openCategory?.title ?: "Dados para projeções",
                subtitle = if (openCategory != null) "Dados para projeções" else null,
                onNavigateBack = { if (openCategory != null) openCategoryName = null else onNavigateBack() },
                scrollBehavior = scrollBehavior,
                navigationEnabled = !state.isSaving,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (openCategory != null) {
                ProgressActionBar(
                    isRunning = state.isSaving,
                    runningText = "Salvando…",
                    idleText = "Salvar",
                    enabled = !state.isLoading,
                    onClick = viewModel::save,
                    icon = Icons.Rounded.Check,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .readableWidth(FormMaxWidth)
                .padding(horizontal = ScreenPadding)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isLoading) LinearWavyProgressIndicator(Modifier.fillMaxWidth())

            if (openCategory == null) {
                Text(
                    "Campos opcionais — preencha o que quiser para deixar as projeções mais precisas. " +
                        "O que você informa aqui tem prioridade sobre a base de referência.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ListGroup(
                    items = ParamCategory.entries.map { category ->
                        GroupItem(
                            headline = category.title,
                            supporting = category.subtitle,
                            icon = category.icon,
                            onClick = { openCategoryName = category.name },
                        )
                    },
                )
            } else {
                CategoryFields(category = openCategory, viewModel = viewModel, isSaving = state.isSaving)
            }
        }
    }
}

/** Campos da categoria aberta. */
@Composable
private fun CategoryFields(
    category: ParamCategory,
    viewModel: ProfileParamsViewModel,
    isSaving: Boolean,
) {
    val state by viewModel.state.collectAsState()
    val form = state.form
    val enabled = !isSaving

    when (category) {
        ParamCategory.ESSENTIALS -> {
            NumberField(form.age, { v -> viewModel.update { it.copy(age = v) } }, "Idade", enabled, suffix = "anos")
            MoneyField(
                value = form.monthlySalary,
                onValueChange = { v -> viewModel.update { it.copy(monthlySalary = sanitizeCurrencyInput(v)) } },
                label = "Salário mensal",
                enabled = enabled,
            )
            OptionalEnumDropdown(
                "Tipo de vínculo", EmploymentType.entries, form.employmentType,
                { sel -> viewModel.update { it.copy(employmentType = sel) } }, { it.label }, enabled,
            )
            NumberField(
                form.retirementAge, { v -> viewModel.update { it.copy(retirementAge = v) } },
                "Idade desejada de aposentadoria", enabled, suffix = "anos",
            )
            MoneyField(
                value = form.monthlyContribution,
                onValueChange = { v -> viewModel.update { it.copy(monthlyContribution = sanitizeCurrencyInput(v)) } },
                label = "Aporte mensal",
                enabled = enabled,
            )
        }
        ParamCategory.PERSONAL -> {
            OptionalEnumDropdown(
                "Estado civil", MaritalStatus.entries, form.maritalStatus,
                { sel -> viewModel.update { it.copy(maritalStatus = sel) } }, { it.label }, enabled,
            )
            NumberField(form.dependents, { v -> viewModel.update { it.copy(dependents = v) } }, "Filhos / dependentes", enabled)
            LifeForgeTextField(
                value = form.childrenAges,
                onValueChange = { v ->
                    viewModel.update { it.copy(childrenAges = v.filter { c -> c.isDigit() || c == ',' || c == ' ' }) }
                },
                label = "Idades dos filhos (ex.: 3, 7)",
                enabled = enabled,
            )
            LifeForgeTextField(
                value = form.state,
                onValueChange = { v -> viewModel.update { it.copy(state = v.take(2)) } },
                label = "Estado (UF)",
                enabled = enabled,
            )
            NumberField(
                form.lifeExpectancy, { v -> viewModel.update { it.copy(lifeExpectancy = v) } },
                "Expectativa de vida", enabled, suffix = "anos",
            )
        }
        ParamCategory.PROFESSIONAL -> {
            PercentField(
                value = form.expectedSalaryGrowth,
                onValueChange = { v -> viewModel.update { it.copy(expectedSalaryGrowth = sanitizeCurrencyInput(v)) } },
                label = "Crescimento salarial esperado",
                suffix = "% a.a.",
                enabled = enabled,
            )
            OptionalEnumDropdown(
                "Risco de desemprego", RiskLevel.entries, form.unemploymentRisk,
                { sel -> viewModel.update { it.copy(unemploymentRisk = sel) } }, { it.label }, enabled,
            )
        }
        ParamCategory.HOUSING -> {
            OptionalEnumDropdown(
                "Situação de moradia", HousingStatus.entries, form.housingStatus,
                { sel -> viewModel.update { it.copy(housingStatus = sel) } }, { it.label }, enabled,
            )
            MoneyField(
                value = form.housingMonthlyCost,
                onValueChange = { v -> viewModel.update { it.copy(housingMonthlyCost = sanitizeCurrencyInput(v)) } },
                label = "Parcela / aluguel mensal",
                enabled = enabled,
            )
            MoneyField(
                value = form.propertyValue,
                onValueChange = { v -> viewModel.update { it.copy(propertyValue = sanitizeCurrencyInput(v)) } },
                label = "Valor do imóvel próprio",
                enabled = enabled,
            )
        }
        ParamCategory.VEHICLES -> {
            MoneyField(
                value = form.vehiclesValue,
                onValueChange = { v -> viewModel.update { it.copy(vehiclesValue = sanitizeCurrencyInput(v)) } },
                label = "Valor de mercado dos veículos",
                enabled = enabled,
            )
        }
        ParamCategory.TAX -> {
            OptionalEnumDropdown(
                "Regime tributário", TaxRegime.entries, form.taxRegime,
                { sel -> viewModel.update { it.copy(taxRegime = sel) } }, { it.label }, enabled,
            )
        }
        ParamCategory.WEALTH -> {
            MoneyField(
                value = form.emergencyReserve,
                onValueChange = { v -> viewModel.update { it.copy(emergencyReserve = sanitizeCurrencyInput(v)) } },
                label = "Reserva de emergência",
                enabled = enabled,
            )
            MoneyField(
                value = form.totalDebt,
                onValueChange = { v -> viewModel.update { it.copy(totalDebt = sanitizeCurrencyInput(v)) } },
                label = "Total de dívidas",
                enabled = enabled,
            )
        }
        ParamCategory.PLANNING -> {
            ToggleRow(
                title = "Pretende ter filhos?",
                checked = form.plansChildren,
                onCheckedChange = { v -> viewModel.update { it.copy(plansChildren = v) } },
                enabled = enabled,
            )
            ToggleRow(
                title = "Pretende comprar ou financiar um imóvel?",
                checked = form.plansProperty,
                onCheckedChange = { v -> viewModel.update { it.copy(plansProperty = v) } },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    suffix: String? = null,
) {
    LifeForgeTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(3)) },
        label = label,
        keyboardType = KeyboardType.Number,
        enabled = enabled,
        suffix = suffix,
    )
}

/**
 * Seleção de enum OPCIONAL (permite "Não informado"/null), com o mesmo menu
 * expressivo do [com.lifeforge.presentation.common.EnumDropdown], que só aceita
 * valor não nulo.
 */
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
    val all: List<T?> = listOf<T?>(null) + options
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
            singleLine = true,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MenuDefaults.standaloneGroupShape,
            containerColor = MenuDefaults.groupStandardContainerColor,
        ) {
            all.forEachIndexed { index, option ->
                DropdownMenuItem(
                    selected = option == selected,
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    text = { Text(option?.let(labelOf) ?: "Não informado") },
                    shapes = MenuDefaults.itemShape(index = index, count = all.size),
                    selectedLeadingIcon = { Icon(Icons.Rounded.Check, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}
