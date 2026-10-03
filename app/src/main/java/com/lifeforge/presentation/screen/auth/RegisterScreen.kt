package com.lifeforge.presentation.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.EmploymentType
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.presentation.common.ActionButton
import com.lifeforge.presentation.common.BottomActionBar
import com.lifeforge.presentation.common.ConnectedChoice
import com.lifeforge.presentation.common.DetailTopAppBar
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.FormMaxWidth
import com.lifeforge.presentation.common.FormSection
import com.lifeforge.presentation.common.LifeForgePasswordField
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.LoadingOverlay
import com.lifeforge.presentation.common.MoneyField
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.label
import com.lifeforge.presentation.common.readableWidth

/**
 * Cadastro: a conta (com tipos de conteúdo para o preenchimento automático e a
 * sugestão de senha forte do gerenciador de senhas), o perfil de risco opcional
 * e os dados essenciais para projeções, também opcionais. "Criar conta" fica
 * fixo na base, acima do teclado.
 *
 * Perfil de risco: tocar na opção marcada desfaz a escolha — o backend assume
 * `MODERATE` quando nenhuma é enviada.
 */
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val enabled = !state.isSubmitting

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopAppBar(
                title = "Criar conta",
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
                navigationEnabled = enabled,
            )
        },
        bottomBar = {
            BottomActionBar {
                ActionButton(
                    text = "Criar conta",
                    icon = Icons.Rounded.PersonAdd,
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .readableWidth(FormMaxWidth)
                    .padding(horizontal = ScreenPadding)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                if (state.errorBanner != null) {
                    ErrorBanner(message = state.errorBanner!!, onDismiss = viewModel::onErrorBannerDismiss)
                }

                FormSection(title = "Sua conta") {
                    LifeForgeTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = "Nome completo",
                        error = state.nameError,
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.PersonFullName },
                    )
                    LifeForgeTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        label = "E-mail",
                        error = state.emailError,
                        keyboardType = KeyboardType.Email,
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Next,
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.EmailAddress },
                    )
                    LifeForgePasswordField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "Senha (mínimo de 8 caracteres)",
                        error = state.passwordError,
                        imeAction = ImeAction.Next,
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.NewPassword },
                    )
                }

                FormSection(
                    title = "Perfil de risco",
                    supporting = "Opcional. Define o ponto de partida da carteira sugerida; dá para mudar depois.",
                ) {
                    ConnectedChoice<RiskProfile?>(
                        options = RiskProfile.entries,
                        selected = state.riskProfile,
                        // Tocar na opção marcada volta a "não informado".
                        onSelect = { p -> viewModel.onRiskProfileChange(if (p == state.riskProfile) null else p) },
                        label = { it?.label().orEmpty() },
                        enabled = enabled,
                    )
                }

                FormSection(
                    title = "Dados para projeções",
                    supporting = "Opcional. Personaliza as simulações; dá para completar depois no Perfil.",
                ) {
                    LifeForgeTextField(
                        value = state.age,
                        onValueChange = viewModel::onAgeChange,
                        label = "Idade",
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                        enabled = enabled,
                        suffix = "anos",
                    )
                    MoneyField(
                        value = state.monthlySalary,
                        onValueChange = viewModel::onMonthlySalaryChange,
                        label = "Salário mensal",
                        enabled = enabled,
                    )
                    Text(
                        "Tipo de vínculo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    EmploymentTypeChips(
                        selected = state.employmentType,
                        onSelect = viewModel::onEmploymentTypeChange,
                        enabled = enabled,
                    )
                    LifeForgeTextField(
                        value = state.retirementAge,
                        onValueChange = viewModel::onRetirementAgeChange,
                        label = "Idade desejada de aposentadoria",
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                        enabled = enabled,
                        suffix = "anos",
                    )
                    MoneyField(
                        value = state.monthlyContribution,
                        onValueChange = viewModel::onMonthlyContributionChange,
                        label = "Aporte mensal",
                        imeAction = ImeAction.Done,
                        enabled = enabled,
                    )
                }
            }

            LoadingOverlay(visible = state.isSubmitting)
        }
    }
}

@Composable
private fun EmploymentTypeChips(
    selected: EmploymentType?,
    onSelect: (EmploymentType?) -> Unit,
    enabled: Boolean,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EmploymentType.entries.forEach { type ->
            val isSelected = selected == type
            FilterChip(
                selected = isSelected,
                // Tocar no selecionado desfaz a escolha (campo opcional).
                onClick = { onSelect(if (isSelected) null else type) },
                label = { Text(type.label) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
                enabled = enabled,
            )
        }
    }
}
