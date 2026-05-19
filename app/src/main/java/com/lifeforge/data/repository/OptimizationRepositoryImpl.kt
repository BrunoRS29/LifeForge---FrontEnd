package com.lifeforge.data.repository

import com.lifeforge.data.api.OptimizationApi
import com.lifeforge.data.mapper.optimizeContributionRequestDto
import com.lifeforge.data.mapper.optimizeHorizonRequestDto
import com.lifeforge.data.mapper.rebalanceRequestDto
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.OptimizationResult
import com.lifeforge.domain.model.RebalanceResult
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.model.mapCatching
import com.lifeforge.domain.repository.OptimizationRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação de [OptimizationRepository] — pass-through para a API
 * sem cache local.
 *
 * Decisão de não cachear: otimização é análise sob demanda (alinhado com
 * o backend que documenta `"otimização e análise sob demanda, não persiste"`).
 * Os parâmetros mudam constantemente conforme o usuário ajusta sliders,
 * então cachear teria pouca utilidade prática.
 */
@Singleton
class OptimizationRepositoryImpl @Inject constructor(
    private val api: OptimizationApi,
    private val json: Json,
) : OptimizationRepository {

    override suspend fun optimizeContribution(
        goalId: Long?,
        initialCapital: Double,
        expectedReturnAnnual: Double,
        volatilityAnnual: Double,
        targetAmount: Double,
        horizonMonths: Int,
        targetSuccessProbability: Double,
        seed: Long?,
    ): DataResult<OptimizationResult> = safeApiCall(json) {
        api.optimizeContribution(
            optimizeContributionRequestDto(
                goalId = goalId,
                initialCapital = initialCapital,
                expectedReturnAnnual = expectedReturnAnnual,
                volatilityAnnual = volatilityAnnual,
                targetAmount = targetAmount,
                horizonMonths = horizonMonths,
                targetSuccessProbability = targetSuccessProbability,
                seed = seed,
            )
        )
    }.mapCatching { it.toDomain() }

    override suspend fun optimizeHorizon(
        goalId: Long?,
        initialCapital: Double,
        expectedReturnAnnual: Double,
        volatilityAnnual: Double,
        targetAmount: Double,
        monthlyContribution: Double,
        targetSuccessProbability: Double,
        seed: Long?,
    ): DataResult<OptimizationResult> = safeApiCall(json) {
        api.optimizeHorizon(
            optimizeHorizonRequestDto(
                goalId = goalId,
                initialCapital = initialCapital,
                expectedReturnAnnual = expectedReturnAnnual,
                volatilityAnnual = volatilityAnnual,
                targetAmount = targetAmount,
                monthlyContribution = monthlyContribution,
                targetSuccessProbability = targetSuccessProbability,
                seed = seed,
            )
        )
    }.mapCatching { it.toDomain() }

    override suspend fun rebalance(
        riskProfile: RiskProfile,
        currentCapital: Double,
        targetAmount: Double,
        monthsToGoal: Int,
    ): DataResult<RebalanceResult> = safeApiCall(json) {
        api.rebalance(
            rebalanceRequestDto(
                riskProfile = riskProfile,
                currentCapital = currentCapital,
                targetAmount = targetAmount,
                monthsToGoal = monthsToGoal,
            )
        )
    }.mapCatching { it.toDomain() }
}
