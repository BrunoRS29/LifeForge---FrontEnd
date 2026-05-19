package com.lifeforge.presentation.screen.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.onFailure
import com.lifeforge.domain.usecase.ObserveGoalsUseCase
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
 * (refresh, erro). Refresh automático no init.
 */
@HiltViewModel
class GoalsListViewModel @Inject constructor(
    observeGoals: ObserveGoalsUseCase,
    private val refreshGoals: RefreshGoalsUseCase,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalUiState())

    val state: StateFlow<GoalsListUiState> = combine(
        observeGoals(),
        localState,
    ) { goals, local ->
        GoalsListUiState(
            goals = goals,
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
            refreshGoals().onFailure { error ->
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
    val goals: List<Goal> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorBanner: String? = null,
)
