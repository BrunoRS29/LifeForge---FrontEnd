package com.lifeforge.presentation.screen.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.GoalHealth
import com.lifeforge.domain.model.onFailure
import com.lifeforge.domain.model.onSuccess
import com.lifeforge.domain.usecase.ObserveGoalsHealthUseCase
import com.lifeforge.domain.usecase.RefreshSimulationHistoriesUseCase
import com.lifeforge.domain.usecase.RefreshGoalsUseCase
import com.lifeforge.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel da lista de metas. Mesmo padrão das outras telas read-only:
 * combina Flow do Room (fonte da verdade) com estado local de UI
 * (refresh, erro). Refresh automático no init — metas e, em seguida, o
 * histórico de simulações que alimenta o selo de saúde de cada uma.
 */
@HiltViewModel
class GoalsListViewModel @Inject constructor(
    observeGoalsHealth: ObserveGoalsHealthUseCase,
    private val refreshGoals: RefreshGoalsUseCase,
    private val refreshSimulationHistories: RefreshSimulationHistoriesUseCase,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalUiState())

    val state: StateFlow<GoalsListUiState> = combine(
        observeGoalsHealth(),
        localState,
    ) { goals, local ->
        GoalsListUiState(
            // Na lista, a ordem é a do usuário (prioridade, depois prazo).
            goals = goals.sortedWith(compareBy({ it.goal.priority }, { it.goal.targetDate })),
            isRefreshing = local.isRefreshing,
            errorBanner = local.errorBanner,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalsListUiState(),
    )

    init { refresh() }

    fun refresh() {
        if (localState.value.isRefreshing) return
        viewModelScope.launch {
            localState.update { it.copy(isRefreshing = true, errorBanner = null) }
            refreshGoals()
                .onSuccess { refreshSimulationHistories() }
                .onFailure { error ->
                    localState.update { it.copy(errorBanner = error.toUserMessage()) }
                }
            localState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onErrorBannerDismiss() {
        localState.update { it.copy(errorBanner = null) }
    }

    private data class LocalUiState(
        val isRefreshing: Boolean = false,
        val errorBanner: String? = null,
    )
}

data class GoalsListUiState(
    /** Metas com a leitura da última simulação (saúde). */
    val goals: List<GoalHealth> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorBanner: String? = null,
)
