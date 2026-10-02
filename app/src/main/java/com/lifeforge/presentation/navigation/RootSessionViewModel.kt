package com.lifeforge.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.repository.SyncStatus
import com.lifeforge.domain.usecase.LogoutUseCase
import com.lifeforge.domain.usecase.ObserveSessionUseCase
import com.lifeforge.domain.usecase.ObserveSyncStatusUseCase
import com.lifeforge.domain.usecase.SetBackgroundSyncUseCase
import com.lifeforge.domain.usecase.SyncNowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * em "Sair" ou o token expira), o NavGraph reage via `LaunchedEffect(state)`
 * e troca o grafo, garantindo que telas autenticadas saiam imediatamente da
 * pilha quando o token é apagado.
 *
 * Também liga a sincronização offline-first em segundo plano durante a
 * sessão (e a desliga ao sair) e expõe o estado dela para a barra global.
 */
@HiltViewModel
class RootSessionViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    private val setBackgroundSync: SetBackgroundSyncUseCase,
    private val syncNowUseCase: SyncNowUseCase,
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

    /** Conectividade e alterações aguardando envio (barra de sincronização). */
    val syncStatus: StateFlow<SyncStatus> = observeSyncStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncStatus())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        // Sessão ativa → sincroniza ao abrir o app e periodicamente; sem sessão → cancela.
        viewModelScope.launch {
            state.collect { session ->
                when (session) {
                    SessionUiState.Authenticated -> setBackgroundSync(enabled = true)
                    SessionUiState.Unauthenticated -> setBackgroundSync(enabled = false)
                    SessionUiState.Loading -> Unit
                }
            }
        }
    }

    /** "Sincronizar agora" da barra de status. */
    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            syncNowUseCase()
            _isSyncing.value = false
        }
    }

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
