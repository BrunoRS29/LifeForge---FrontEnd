package com.lifeforge.presentation.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.LifeForgePasswordField
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.LoadingOverlay
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * Tela de registro. Diferente do login, tem top bar com back button
 * porque é uma navegação "filha" (push em cima da pilha de auth).
 *
 * O grupo de chips de perfil de risco é opcional — backend assume
 * `MODERATE` quando nenhum é selecionado. Texto explicativo embaixo
 * deixa isso claro para o usuário.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar conta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isSubmitting) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                if (state.errorBanner != null) {
                    ErrorBanner(
                        message = state.errorBanner!!,
                        onDismiss = viewModel::onErrorBannerDismiss,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                LifeForgeTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = "Nome completo",
                    error = state.nameError,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                    enabled = !state.isSubmitting,
                )
                Spacer(Modifier.height(12.dp))
                LifeForgeTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "E-mail",
                    error = state.emailError,
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next,
                    enabled = !state.isSubmitting,
                )
                Spacer(Modifier.height(12.dp))
                LifeForgePasswordField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Senha (mínimo 8 caracteres)",
                    error = state.passwordError,
                    imeAction = ImeAction.Done,
                    enabled = !state.isSubmitting,
                )
                Spacer(Modifier.height(24.dp))

                Text(
                    "Perfil de risco (opcional)",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Define o ponto base do rebalanceamento sugerido. " +
                        "Pode ser alterado depois.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                RiskProfileChips(
                    selected = state.riskProfile,
                    onSelect = viewModel::onRiskProfileChange,
                    enabled = !state.isSubmitting,
                )

                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Criar conta")
                }
                Spacer(Modifier.height(16.dp))
            }

            LoadingOverlay(visible = state.isSubmitting)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RiskProfileChips(
    selected: RiskProfile?,
    onSelect: (RiskProfile?) -> Unit,
    enabled: Boolean,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RiskProfile.entries.forEach { profile ->
            FilterChip(
                selected = selected == profile,
                onClick = {
                    // Tocar no chip selecionado desfaz a seleção (volta a opcional).
                    onSelect(if (selected == profile) null else profile)
                },
                label = { Text(profile.label()) },
                enabled = enabled,
            )
        }
    }
}

private fun RiskProfile.label(): String = when (this) {
    RiskProfile.CONSERVATIVE -> "Conservador"
    RiskProfile.MODERATE -> "Moderado"
    RiskProfile.AGGRESSIVE -> "Arrojado"
}
