package com.lifeforge.presentation.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.model.onFailure
import com.lifeforge.domain.usecase.RegisterUseCase
import com.lifeforge.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel da tela de registro.
 *
 * Igual ao [LoginViewModel] em estrutura, com dois campos extras
 * (nome e perfil de risco opcional). Em sucesso, o
 * [com.lifeforge.presentation.navigation.RootSessionViewModel] reage
 * à nova sessão e navega para Dashboard automaticamente.
 *
 * Perfil de risco é opcional no registro — o backend aplica
 * `MODERATE` como default. O usuário pode trocar depois no Profile.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email, emailError = null, errorBanner = null) }
    }

    fun onNameChange(name: String) {
        _state.update { it.copy(name = name, nameError = null, errorBanner = null) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, passwordError = null, errorBanner = null) }
    }

    fun onRiskProfileChange(profile: RiskProfile?) {
        _state.update { it.copy(riskProfile = profile) }
    }

    fun onErrorBannerDismiss() {
        _state.update { it.copy(errorBanner = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorBanner = null) }

            registerUseCase(
                email = current.email,
                name = current.name,
                password = current.password,
                riskProfile = current.riskProfile,
            ).onFailure { error -> handleError(error) }

            _state.update { it.copy(isSubmitting = false) }
        }
    }

    private fun handleError(error: AppError) {
        when (error) {
            is AppError.Validation -> when (error.field) {
                "email" -> _state.update { it.copy(emailError = error.message) }
                "name" -> _state.update { it.copy(nameError = error.message) }
                "password" -> _state.update { it.copy(passwordError = error.message) }
                else -> _state.update {
                    it.copy(errorBanner = error.message ?: "Dados inválidos")
                }
            }
            // 409: backend retorna Conflict quando o email já existe.
            is AppError.Conflict -> _state.update {
                it.copy(emailError = "E-mail já cadastrado")
            }
            else -> _state.update { it.copy(errorBanner = error.toUserMessage()) }
        }
    }
}

data class RegisterUiState(
    val email: String = "",
    val name: String = "",
    val password: String = "",
    val riskProfile: RiskProfile? = null,
    val emailError: String? = null,
    val nameError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val errorBanner: String? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting &&
            email.isNotBlank() &&
            name.isNotBlank() &&
            password.isNotBlank()
}
