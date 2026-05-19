package com.lifeforge.presentation.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.LifeForgePasswordField
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.LoadingOverlay

/**
 * Tela de login. Layout simples vertical centralizado — branding
 * "LifeForge" em destaque, dois campos, botão principal e link para
 * registro.
 *
 * `imePadding` no Column raiz garante que o teclado não cobre os
 * campos quando aberto. `verticalScroll` evita layout quebrado em
 * telas pequenas (Android Go, paisagem).
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = "LifeForge",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Entre na sua conta",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            if (state.errorBanner != null) {
                ErrorBanner(
                    message = state.errorBanner!!,
                    onDismiss = viewModel::onErrorBannerDismiss,
                )
                Spacer(Modifier.height(16.dp))
            }

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
                label = "Senha",
                error = state.passwordError,
                imeAction = ImeAction.Done,
                enabled = !state.isSubmitting,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Entrar")
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onNavigateToRegister, enabled = !state.isSubmitting) {
                Text("Não tenho conta — criar uma")
            }
        }

        LoadingOverlay(visible = state.isSubmitting)
    }
}
