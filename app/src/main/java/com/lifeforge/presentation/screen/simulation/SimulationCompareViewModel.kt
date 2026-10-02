package com.lifeforge.presentation.screen.simulation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.StrategyComparison
import com.lifeforge.domain.usecase.CompareSimulationsUseCase
import com.lifeforge.presentation.common.toUserMessage
import com.lifeforge.presentation.navigation.SimulationCompare
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Compara duas rodadas de simulação da mesma meta (estratégia A × B). As
 * rodadas vêm do cache local quando possível — a comparação abre offline.
 */
@HiltViewModel
class SimulationCompareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val compareSimulations: CompareSimulationsUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<SimulationCompare>()

    private val _state = MutableStateFlow(SimulationCompareUiState())
    val state: StateFlow<SimulationCompareUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = compareSimulations(route.firstId, route.secondId)) {
                is DataResult.Success -> _state.update { it.copy(isLoading = false, comparison = result.data) }
                is DataResult.Failure -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.error.toUserMessage())
                }
            }
        }
    }
}

data class SimulationCompareUiState(
    val isLoading: Boolean = true,
    val comparison: StrategyComparison? = null,
    val errorMessage: String? = null,
)
