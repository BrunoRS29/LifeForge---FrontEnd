package com.lifeforge.presentation.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.User
import com.lifeforge.domain.model.UserProfile
import com.lifeforge.domain.usecase.FinancialSnapshot
import com.lifeforge.domain.usecase.GetFinancialSnapshotUseCase
import com.lifeforge.domain.usecase.GetUserProfileUseCase
import com.lifeforge.domain.usecase.ObserveCurrentUserUseCase
import com.lifeforge.domain.usecase.RefreshAssetsUseCase
import com.lifeforge.domain.usecase.RefreshExpensesUseCase
import com.lifeforge.domain.usecase.RefreshIncomesUseCase
import com.lifeforge.presentation.common.parseCurrencyInput
import com.lifeforge.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel do Dashboard. Combina três fontes:
 *
 * 1. Flow do usuário corrente (para o cabeçalho "Olá, Gabriel")
 * 2. Flow do snapshot financeiro (calculado pelo
 *    [GetFinancialSnapshotUseCase] via `combine` de income/expense/asset)
 * 3. Estado local de refresh + erro
 *
 * O refresh dispara as 3 chamadas (incomes, expenses, assets) **em
 * paralelo** via `async` — espera o `awaitAll` para saber se todas
 * passaram. Em falha de qualquer uma, mostramos a primeira mensagem
 * de erro mas continuamos exibindo o cache local atualizado parcialmente.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase,
    getFinancialSnapshot: GetFinancialSnapshotUseCase,
    private val refreshIncomes: RefreshIncomesUseCase,
    private val refreshExpenses: RefreshExpensesUseCase,
    private val refreshAssets: RefreshAssetsUseCase,
    private val getUserProfile: GetUserProfileUseCase,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalUiState())

    val state: StateFlow<DashboardUiState> = combine(
        observeCurrentUser(),
        getFinancialSnapshot(
            // Salário do perfil é a fonte de verdade da renda mensal. Derivado
            // do perfil já carregado em localState (sem fetch extra); distinct
            // evita recalcular o snapshot a cada toggle de refreshing.
            configuredSalary = localState
                .map { it.profile?.monthlySalary?.let(::parseCurrencyInput) }
                .distinctUntilChanged(),
        ),
        localState,
    ) { user, snapshot, local ->
        DashboardUiState(
            user = user,
            snapshot = snapshot,
            profile = local.profile,
            isRefreshing = local.isRefreshing,
            errorBanner = local.errorBanner,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    init { refresh() }

    fun refresh() {
        if (localState.value.isRefreshing) return
        // Perfil é network-only e best-effort: alimenta a projeção personalizada
        // sem bloquear o resto do dashboard nem virar banner de erro.
        viewModelScope.launch {
            (getUserProfile() as? DataResult.Success)?.let { ok ->
                localState.update { it.copy(profile = ok.data) }
            }
        }
        viewModelScope.launch {
            localState.update { it.copy(isRefreshing = true, errorBanner = null) }

            // Paralelizar as 3 refreshes — todas independentes, mesma
            // janela de erro. coroutineScope garante que se uma falhar
            // de forma inesperada, as outras são canceladas juntas.
            val firstError: String? = coroutineScope {
                val results = listOf(
                    async { refreshIncomes() },
                    async { refreshExpenses() },
                    async { refreshAssets() },
                ).awaitAll()
                results.firstNotNullOfOrNull { result ->
                    if (result is com.lifeforge.domain.model.DataResult.Failure) {
                        result.error.toUserMessage()
                    } else null
                }
            }

            localState.update {
                it.copy(isRefreshing = false, errorBanner = firstError)
            }
        }
    }

    fun onErrorBannerDismiss() {
        localState.update { it.copy(errorBanner = null) }
    }

    private data class LocalUiState(
        val isRefreshing: Boolean = false,
        val errorBanner: String? = null,
        val profile: UserProfile? = null,
    )
}

data class DashboardUiState(
    val user: User? = null,
    val snapshot: FinancialSnapshot? = null,
    val profile: UserProfile? = null,
    val isRefreshing: Boolean = false,
    val errorBanner: String? = null,
)
