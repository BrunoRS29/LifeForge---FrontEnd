package com.lifeforge.presentation.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.presentation.common.ActionButton
import com.lifeforge.presentation.common.BrandMark
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.LifeForgePasswordField
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.LoadingOverlay
import com.lifeforge.presentation.common.readableWidth

/** Largura máxima do formulário de entrada em telas largas. */
private val AuthMaxWidth = 480.dp

/**
 * Tela de login: a marca (o ícone do app numa forma do Material), o nome e a
 * proposta do app, os dois campos — com tipo de conteúdo para o preenchimento
 * automático dos gerenciadores de senha — e a ação de entrar.
 *
 * O conteúdo fica centralizado quando cabe e rola quando não cabe (telas
 * pequenas, paisagem, teclado aberto); `safeDrawingPadding` respeita as barras
 * do sistema e o teclado.
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .readableWidth(AuthMaxWidth)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                BrandMark()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LifeForge",
                        style = MaterialTheme.typography.displaySmallEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = "Planeje sua vida com milhares de futuros possíveis",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(8.dp))

                if (state.errorBanner != null) {
                    ErrorBanner(message = state.errorBanner!!, onDismiss = viewModel::onErrorBannerDismiss)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.EmailAddress },
                )
                LifeForgePasswordField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Senha",
                    error = state.passwordError,
                    imeAction = ImeAction.Done,
                    enabled = !state.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Password },
                )
                ActionButton(
                    text = "Entrar",
                    icon = Icons.AutoMirrored.Rounded.Login,
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                TextButton(onClick = onNavigateToRegister, enabled = !state.isSubmitting) {
                    Text("Ainda não tem conta? Criar conta")
                }
            }
        }

        LoadingOverlay(visible = state.isSubmitting)
    }
}
