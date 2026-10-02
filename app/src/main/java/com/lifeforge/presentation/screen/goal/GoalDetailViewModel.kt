package com.lifeforge.presentation.screen.goal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalHealth
import com.lifeforge.domain.model.GoalHealthEvaluator
import com.lifeforge.domain.model.SimulationSummary
import com.lifeforge.domain.model.isLocalOnly
import com.lifeforge.domain.model.onFailure
import com.lifeforge.domain.model.onSuccess
import com.lifeforge.domain.usecase.DeleteGoalUseCase
import com.lifeforge.domain.usecase.ObserveGoalUseCase
import com.lifeforge.domain.usecase.ObserveSimulationsByGoalUseCase
import com.lifeforge.domain.usecase.RefreshSimulationsByGoalUseCase
import com.lifeforge.presentation.common.toUserMessage
import com.lifeforge.presentation.navigation.GoalDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel do detalhe de uma meta.
 *
 * Extrai o `goalId` do [SavedStateHandle] via `toRoute<GoalDetail>()` —
 * forma idiomática de ler argumentos type-safe do Nav Compose 2.8+
 * dentro de ViewModels com Hilt.
 *
 * Mostra também a "saúde" da meta (última simulação) a partir do histórico
 * local, atualizado em segundo plano ao abrir a tela.
 *
 * Offline-first: uma meta criada sem conexão tem id temporário; quando ela
 * sincroniza com a tela aberta, o repositório passa a emitir a versão com o id
 * definitivo — por isso as ações usam `goal.id` atual, não o id da rota.
 *
 * Eventos one-shot (como "navegue de volta após deletar") são emitidos
 * por um [Channel] consumido como Flow na tela — evita re-emissão em
 * recomposições e respeita o ciclo de vida via `collect`.
 */
@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeGoal: ObserveGoalUseCase,
    private val observeSimulationsByGoal: ObserveSimulationsByGoalUseCase,
    private val refreshSimulationsByGoal: RefreshSimulationsByGoalUseCase,
    private val deleteGoal: DeleteGoalUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val routeGoalId: Long = savedStateHandle.toRoute<GoalDetail>().goalId

    private val localState = MutableStateFlow(LocalUiState())
    private val events = Channel<GoalDetailEvent>(Channel.BUFFERED)
    val eventsFlow = events.receiveAsFlow()

    private val goalFlow = observeGoal(routeGoalId)

    private val goalWithLatest = goalFlow.flatMapLatest { goal ->
        if (goal == null || goal.isLocalOnly()) {
            flowOf(goal to null)
        } else {
            observeSimulationsByGoal(goal.id).map { history -> goal to history.firstOrNull() }
        }
    }

    val state: StateFlow<GoalDetailUiState> = combine(
        goalWithLatest,
        localState,
    ) { (goal, latest), local ->
        GoalDetailUiState(
            goal = goal,
            health = goal?.let { GoalHealthEvaluator.evaluate(it, latest, Instant.now(clock)) },
            latestSimulation = latest,
            isDeleting = local.isDeleting,
            errorBanner = local.errorBanner,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalDetailUiState(),
    )

    init {
        // Histórico de simulações (best-effort, em segundo plano): atualiza a
        // saúde da meta com simulações feitas em outro aparelho ou pela IA.
        viewModelScope.launch {
            val goal = goalFlow.filterNotNull().map { it.id }.distinctUntilChanged().first { it > 0 }
            refreshSimulationsByGoal(goal)
        }
    }

    fun delete() {
        if (localState.value.isDeleting) return
        val id = state.value.goal?.id ?: routeGoalId
        viewModelScope.launch {
            localState.update { it.copy(isDeleting = true, errorBanner = null) }
            deleteGoal(id)
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
    /** Leitura da última simulação (no caminho / atenção / em risco…). */
    val health: GoalHealth? = null,
    val latestSimulation: SimulationSummary? = null,
    val isDeleting: Boolean = false,
    val errorBanner: String? = null,
)

sealed interface GoalDetailEvent {
    data object NavigateBack : GoalDetailEvent
}
