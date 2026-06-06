package com.lifeforge.domain.usecase

import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.CalibratedSimulation
import com.lifeforge.domain.model.CalibratedSimulationParameters
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.ExpensePrediction
import com.lifeforge.domain.model.IncomePrediction
import com.lifeforge.domain.model.PredictionSummary
import com.lifeforge.domain.model.WealthPrediction
import com.lifeforge.domain.repository.PredictionRepository
import javax.inject.Inject

/**
 * UseCases do subsistema de IA preditiva (Sprint 5).
 *
 * Validacao em duas camadas:
 *  1. Aqui (use case): regras de negocio (faixas validas, minimos do TCC).
 *  2. Backend Ktor: regras finais (precisa ter historico suficiente
 *     no banco do usuario, etc.) - retorna 422 com codigo estruturado.
 *
 * Por que validar aqui se o backend ja valida? Para evitar uma round-trip
 * de 3s quando o usuario digita um valor obviamente invalido. UX > rede.
 */

// ============================================================================
// Predict Income
// ============================================================================

class PredictIncomeUseCase @Inject constructor(
    private val repository: PredictionRepository,
) {
    suspend operator fun invoke(horizonMonths: Int): DataResult<IncomePrediction> {
        if (horizonMonths !in MIN_HORIZON..MAX_INCOME_HORIZON) {
            return DataResult.Failure(
                AppError.Validation(
                    "horizonMonths",
                    "horizonte deve estar entre $MIN_HORIZON e $MAX_INCOME_HORIZON meses",
                )
            )
        }
        return repository.predictIncome(horizonMonths)
    }

    companion object {
        const val MIN_HORIZON = 1
        const val MAX_INCOME_HORIZON = 60
    }
}

// ============================================================================
// Predict Expenses
// ============================================================================

class PredictExpensesUseCase @Inject constructor(
    private val repository: PredictionRepository,
) {
    suspend operator fun invoke(horizonMonths: Int = 1): DataResult<ExpensePrediction> {
        if (horizonMonths !in MIN_HORIZON..MAX_EXPENSE_HORIZON) {
            return DataResult.Failure(
                AppError.Validation(
                    "horizonMonths",
                    "horizonte deve estar entre $MIN_HORIZON e $MAX_EXPENSE_HORIZON meses",
                )
            )
        }
        return repository.predictExpenses(horizonMonths)
    }

    companion object {
        const val MIN_HORIZON = 1
        // Random Forest extrapola mal alem de 12 meses - limite documentado
        // no proprio microsservico Python.
        const val MAX_EXPENSE_HORIZON = 12
    }
}

// ============================================================================
// Predict Wealth (serie temporal de patrimonio)
// ============================================================================

class PredictWealthUseCase @Inject constructor(
    private val repository: PredictionRepository,
) {
    suspend operator fun invoke(horizonMonths: Int = 12): DataResult<WealthPrediction> {
        if (horizonMonths !in MIN_HORIZON..MAX_WEALTH_HORIZON) {
            return DataResult.Failure(
                AppError.Validation(
                    "horizonMonths",
                    "horizonte deve estar entre $MIN_HORIZON e $MAX_WEALTH_HORIZON meses",
                )
            )
        }
        return repository.predictWealth(horizonMonths)
    }

    companion object {
        const val MIN_HORIZON = 1
        const val MAX_WEALTH_HORIZON = 60
    }
}

// ============================================================================
// List Recent Predictions (auditoria)
// ============================================================================

class ListRecentPredictionsUseCase @Inject constructor(
    private val repository: PredictionRepository,
) {
    suspend operator fun invoke(limit: Int = 50): DataResult<List<PredictionSummary>> {
        val safeLimit = limit.coerceIn(1, 200)
        return repository.listRecent(safeLimit)
    }
}

// ============================================================================
// Run Calibrated Simulation
// ============================================================================

class RunCalibratedSimulationUseCase @Inject constructor(
    private val repository: PredictionRepository,
) {
    suspend operator fun invoke(
        parameters: CalibratedSimulationParameters,
    ): DataResult<CalibratedSimulation> {
        validate(parameters)?.let { return it }
        return repository.runCalibrated(parameters)
    }

    private fun validate(p: CalibratedSimulationParameters): DataResult.Failure? {
        // Replica as mesmas validacoes do RunSimulationUseCase (Sprint 2)
        // que ainda sao aplicaveis - sem `monthlyContribution` (derivado).
        if (p.numSimulations < MIN_SIMULATIONS) {
            return DataResult.Failure(
                AppError.Validation("numSimulations", "minimo de $MIN_SIMULATIONS iteracoes")
            )
        }
        if (p.horizonMonths <= 0) {
            return DataResult.Failure(
                AppError.Validation("horizonMonths", "horizonte deve ser positivo")
            )
        }
        if (p.targetAmount <= 0.0) {
            return DataResult.Failure(
                AppError.Validation("targetAmount", "valor alvo deve ser positivo")
            )
        }
        if (p.volatilityAnnual < 0.0) {
            return DataResult.Failure(
                AppError.Validation("volatilityAnnual", "volatilidade nao pode ser negativa")
            )
        }
        if (p.unemploymentProbAnnual !in 0.0..1.0) {
            return DataResult.Failure(
                AppError.Validation("unemploymentProbAnnual", "probabilidade entre 0 e 1")
            )
        }
        if (p.incomeHorizonMonths !in 1..60) {
            return DataResult.Failure(
                AppError.Validation("incomeHorizonMonths", "entre 1 e 60 meses")
            )
        }
        return null
    }

    companion object {
        /** Especificacao do TCC - Monte Carlo deve usar pelo menos 10k iteracoes. */
        const val MIN_SIMULATIONS = 10_000
    }
}
