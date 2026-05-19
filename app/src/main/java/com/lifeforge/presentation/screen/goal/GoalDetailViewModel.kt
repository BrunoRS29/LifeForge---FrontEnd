package com.lifeforge.presentation.screen.goal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.onFailure
import com.lifeforge.domain.model.onSuccess
import com.lifeforge.domain.usecase.DeleteGoalUseCase
import com.lifeforge.domain.usecase.ObserveGoalUseCase
import com.lifeforge.presentation.common.toUserMessage
import com.lifeforge.presentation.navigation.GoalDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel do detalhe de uma meta.
 *
 * Extrai o `goalId` do [SavedStateHandle] via `toRoute<GoalDetail>()` —
 * forma idiomática de ler argumentos type-safe do Nav Compose 2.8+
 * dentro de ViewModels com Hilt.
 *
 * Eventos one-shot (como "navegue de volta após deletar") são emitidos
 * por um [Channel] consumido como Flow na tela — evita re-emissão em
 * recomposições e respeita o ciclo de vida via `collect`.
 */
@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeGoal: ObserveGoalUseCase,
    private val deleteGoal: DeleteGoalUseCase,
) : ViewModel() {

    private val goalId: Long = savedStateHandle.toRoute<GoalDetail>().goalId

    private val localState = MutableStateFlow(LocalUiState())
    private val events = Channel<GoalDetailEvent>(Channel.BUFFERED)
    val eventsFlow = events.receiveAsFlow()

    val state: StateFlow<GoalDetailUiState> = combine(
        observeGoal(goalId),
        localState,
    ) { goal, local ->
        GoalDetailUiState(
            goal = goal,
            isDeleting = local.isDeleting,
            errorBanner = local.errorBanner,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalDetailUiState(),
    )

    fun delete() {
        if (localState.value.isDeleting) return
        viewModelScope.launch {
            localState.update { it.copy(isDeleting = true, errorBanner = null) }
            deleteGoal(goalId)
                .onSuccess { events.send(GoalDetailEvent.NavigateBack) }
                .onFailure { error ->
                    localState.update { it.copy(errorBanner = error.toUserMessage()) }
                }
            localState.update { it.copy(isDeleting = false) }
        }
    }

    fun onErrorBannerDismiss() {
        localState.update { it.copy(errorBanner = null) }
    }

    private data class LocalUiState(
        val isDeleting: Boolean = false,
        val errorBanner: String? = null,
    )
}

data class GoalDetailUiState(
    val goal: Goal? = null,
    val isDeleting: Boolean = false,
    val errorBanner: String? = null,
)

sealed interface GoalDetailEvent {
    data object NavigateBack : GoalDetailEvent
}
