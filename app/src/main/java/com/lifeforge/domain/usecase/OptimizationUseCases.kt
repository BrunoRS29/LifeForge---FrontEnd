package com.lifeforge.domain.usecase

import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.OptimizationResult
import com.lifeforge.domain.model.RebalanceResult
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.repository.OptimizationRepository
import javax.inject.Inject

/**
 * UseCases de otimização financeira.
 *
 * Validação fina dos parâmetros (compatibilidade entre
 * `targetSuccessProbability` e horizonte etc.) acontece no backend —
 * aqui só checagens triviais para evitar requisições absurdas.
 */

class OptimizeContributionUseCase @Inject constructor(
    private val repository: OptimizationRepository,
) {
    suspend operator fun invoke(
        goalId: Long? = null,
        initialCapital: Double,
        expectedReturnAnnual: Double,
        volatilityAnnual: Double,
        targetAmount: Double,
        horizonMonths: Int,
        targetSuccessProbability: Double,
        seed: Long? = null,
    ): DataResult<OptimizationResult> {
        validateCommon(targetAmount, horizonMonths, targetSuccessProbability, volatilityAnnual)
            ?.let { return it }
        return repository.optimizeContribution(
            goalId, initialCapital, expectedReturnAnnual, volatilityAnnual,
            targetAmount, horizonMonths, targetSuccessProbability, seed,
        )
    }
}

class OptimizeHorizonUseCase @Inject constructor(
    private val repository: OptimizationRepository,
) {
    suspend operator fun invoke(
        goalId: Long? = null,
        initialCapital: Double,
        expectedReturnAnnual: Double,
        volatilityAnnual: Double,
        targetAmount: Double,
        monthlyContribution: Double,
        targetSuccessProbability: Double,
        seed: Long? = null,
    ): DataResult<OptimizationResult> {
        if (monthlyContribution < 0.0) {
            return DataResult.Failure(
                AppError.Validation("monthlyContribution", "aporte não pode ser negativo")
            )
        }
        if (targetAmount <= 0.0) {
            return DataResult.Failure(AppError.Validation("targetAmount", "valor alvo deve ser positivo"))
        }
        if (targetSuccessProbability !in 0.0..1.0) {
            return DataResult.Failure(
                AppError.Validation("targetSuccessProbability", "probabilidade entre 0 e 1")
            )
        }
        if (volatilityAnnual < 0.0) {
            return DataResult.Failure(AppError.Validation("volatilityAnnual", "volatilidade não pode ser negativa"))
        }
        return repository.optimizeHorizon(
            goalId, initialCapital, expectedReturnAnnual, volatilityAnnual,
            targetAmount, monthlyContribution, targetSuccessProbability, seed,
        )
    }
}

class RebalanceUseCase @Inject constructor(
    private val repository: OptimizationRepository,
) {
    suspend operator fun invoke(
        riskProfile: RiskProfile,
        currentCapital: Double,
        targetAmount: Double,
        monthsToGoal: Int,
    ): DataResult<RebalanceResult> {
        if (targetAmount <= 0.0) {
            return DataResult.Failure(AppError.Validation("targetAmount", "valor alvo deve ser positivo"))
        }
        if (monthsToGoal <= 0) {
            return DataResult.Failure(AppError.Validation("monthsToGoal", "horizonte deve ser positivo"))
        }
        if (currentCapital < 0.0) {
            return DataResult.Failure(AppError.Validation("currentCapital", "capital não pode ser negativo"))
        }
        return repository.rebalance(riskProfile, currentCapital, targetAmount, monthsToGoal)
    }
}

private fun validateCommon(
    targetAmount: Double,
    horizonMonths: Int,
    targetSuccessProbability: Double,
    volatilityAnnual: Double,
): DataResult.Failure? {
    if (targetAmount <= 0.0) {
        return DataResult.Failure(AppError.Validation("targetAmount", "valor alvo deve ser positivo"))
    }
    if (horizonMonths <= 0) {
        return DataResult.Failure(AppError.Validation("horizonMonths", "horizonte deve ser positivo"))
    }
    if (targetSuccessProbability !in 0.0..1.0) {
        return DataResult.Failure(
            AppError.Validation("targetSuccessProbability", "probabilidade entre 0 e 1")
        )
    }
    if (volatilityAnnual < 0.0) {
        return DataResult.Failure(AppError.Validation("volatilityAnnual", "volatilidade não pode ser negativa"))
    }
    return null
}
