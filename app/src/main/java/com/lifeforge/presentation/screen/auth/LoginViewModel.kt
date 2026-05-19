package com.lifeforge.presentation.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.onFailure
import com.lifeforge.domain.usecase.LoginUseCase
import com.lifeforge.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel da tela de login.
 *
 * Estado único [LoginUiState] no MutableStateFlow — toda mudança vai
 * por `update { ... }` para preservar atomicidade. A UI é stateless:
 * só observa `state` e invoca os métodos de evento.
 *
 * Após login bem-sucedido, **não navegamos daqui**. O
 * [com.lifeforge.presentation.navigation.RootSessionViewModel] observa
 * `AuthRepository.observeSession()` e o NavGraph reage à transição
 * Unauthenticated → Authenticated automaticamente. Isso elimina o
 * acoplamento entre ViewModel e NavController.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email, emailError = null, errorBanner = null) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, passwordError = null, errorBanner = null) }
    }

    fun onErrorBannerDismiss() {
        _state.update { it.copy(errorBanner = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return  // evita double-submit

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorBanner = null) }

            loginUseCase(current.email, current.password)
                .onFailure { error -> handleError(error) }

            _state.update { it.copy(isSubmitting = false) }
            // Em sucesso, RootSessionViewModel detecta a nova sessão e
            // o NavGraph navega para Dashboard automaticamente.
        }
    }

    private fun handleError(error: AppError) {
        when (error) {
            is AppError.Validation -> when (error.field) {
                "email" -> _state.update { it.copy(emailError = error.message) }
                "password" -> _state.update { it.copy(passwordError = error.message) }
                else -> _state.update {
                    it.copy(errorBanner = error.message ?: "Dados inválidos")
                }
            }
            // 401 no login = credenciais erradas. Mensagem específica
            // (não vaza qual campo errou, por segurança).
            is AppError.Unauthorized -> _state.update {
                it.copy(errorBanner = "E-mail ou senha incorretos.")
            }
            else -> _state.update { it.copy(errorBanner = error.toUserMessage()) }
        }
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val errorBanner: String? = null,
) {
    /**
     * Habilita o botão de submit apenas quando os campos têm conteúdo
     * e nenhuma submissão está em andamento. Validação completa é
     * delegada ao [LoginUseCase].
     */
    val canSubmit: Boolean
        get() = !isSubmitting && email.isNotBlank() && password.isNotBlank()
}
