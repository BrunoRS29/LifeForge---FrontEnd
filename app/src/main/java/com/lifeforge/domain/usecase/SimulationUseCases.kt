package com.lifeforge.domain.usecase

import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.SimulationParameters
import com.lifeforge.domain.model.SimulationResult
import com.lifeforge.domain.model.SimulationSummary
import com.lifeforge.domain.repository.SimulationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCases de simulação Monte Carlo.
 *
 * [RunSimulationUseCase] valida o conjunto de parâmetros antes de
 * disparar a chamada cara (~1-3s no backend para 10k iterações).
 * Conforme a especificação técnica do TCC, a contagem mínima de
 * iterações é 10.000.
 */

class RunSimulationUseCase @Inject constructor(
    private val repository: SimulationRepository,
) {
    suspend operator fun invoke(parameters: SimulationParameters): DataResult<SimulationResult> {
        validate(parameters)?.let { return it }
        return repository.run(parameters)
    }

    private fun validate(p: SimulationParameters): DataResult.Failure? {
        if (p.numSimulations < MIN_SIMULATIONS) {
            return DataResult.Failure(
                AppError.Validation("numSimulations", "mínimo de $MIN_SIMULATIONS iterações")
            )
        }
        if (p.horizonMonths <= 0) {
            return DataResult.Failure(AppError.Validation("horizonMonths", "horizonte deve ser positivo"))
        }
        if (p.targetAmount <= 0.0) {
            return DataResult.Failure(AppError.Validation("targetAmount", "valor alvo deve ser positivo"))
        }
        if (p.volatilityAnnual < 0.0) {
            return DataResult.Failure(AppError.Validation("volatilityAnnual", "volatilidade não pode ser negativa"))
        }
        if (p.unemploymentProbAnnual !in 0.0..1.0) {
            return DataResult.Failure(
                AppError.Validation("unemploymentProbAnnual", "probabilidade entre 0 e 1")
            )
        }
        return null
    }

    companion object {
        /** Especificação técnica do TCC — Monte Carlo deve usar pelo menos 10k iterações. */
        const val MIN_SIMULATIONS = 10_000
    }
}

class GetSimulationUseCase @Inject constructor(
    private val repository: SimulationRepository,
) {
    suspend operator fun invoke(id: Long): DataResult<SimulationResult> =
        repository.getById(id)
}

class ObserveSimulationsByGoalUseCase @Inject constructor(
    private val repository: SimulationRepository,
) {
    operator fun invoke(goalId: Long): Flow<List<SimulationSummary>> =
        repository.observeByGoal(goalId)
}

class RefreshSimulationsByGoalUseCase @Inject constructor(
    private val repository: SimulationRepository,
) {
    suspend operator fun invoke(goalId: Long): DataResult<Unit> =
        repository.refreshByGoal(goalId)
}

class DeleteSimulationUseCase @Inject constructor(
    private val repository: SimulationRepository,
) {
    suspend operator fun invoke(id: Long): DataResult<Unit> = repository.delete(id)
}
