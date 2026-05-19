package com.lifeforge.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.usecase.LogoutUseCase
import com.lifeforge.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel raiz que observa a sessão de autenticação e dirige o
 * destino inicial do NavHost.
 *
 * Estados possíveis:
 * - [SessionUiState.Loading]: leitura inicial do DataStore — exibe
 *   splash/loading. Estado curto (~1 frame normalmente).
 * - [SessionUiState.Unauthenticated]: sem token ou token sem usuário
 *   correspondente — manda para [Login].
 * - [SessionUiState.Authenticated]: token + usuário presentes — manda
 *   para [Dashboard].
 *
 * Quando a sessão muda enquanto o app está aberto (ex.: usuário clica
 * em "Sair"), o NavGraph reage via `LaunchedEffect(state)` e troca o
 * grafo, garantindo que telas autenticadas saiam imediatamente da
 * pilha quando o token é apagado.
 */
@HiltViewModel
class RootSessionViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    val state: StateFlow<SessionUiState> = observeSession()
        .map { session ->
            if (session != null) SessionUiState.Authenticated
            else SessionUiState.Unauthenticated
        }
        .stateIn(
            scope = viewModelScope,
            // SharingStarted.Eagerly: começamos a coletar imediatamente
            // para não termos atraso na primeira navegação. O custo é
            // baixo — uma única assinatura do Flow do Room + DataStore.
            started = SharingStarted.Eagerly,
            initialValue = SessionUiState.Loading,
        )

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            // Após logout, observeSession emite null → state vira
            // Unauthenticated → o NavGraph reage e navega para Login.
        }
    }
}

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object Unauthenticated : SessionUiState
    data object Authenticated : SessionUiState
}
