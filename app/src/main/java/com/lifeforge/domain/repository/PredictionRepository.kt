package com.lifeforge.domain.repository

import com.lifeforge.domain.model.CalibratedSimulation
import com.lifeforge.domain.model.CalibratedSimulationParameters
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.ExpensePrediction
import com.lifeforge.domain.model.IncomePrediction
import com.lifeforge.domain.model.PredictionSummary

/**
 * Contrato do repositorio de IA preditiva (Sprint 5).
 *
 * Diferente dos CRUD repositories (Sprint 1), este NAO usa Room. Toda
 * operacao bate na API:
 *  - Predicoes sao geradas sob demanda (cada `predict*` treina um modelo
 *    fresco no microsservico Python). Cachear localmente daria poucos
 *    ganhos e introduziria sincronizacao com o cache do backend.
 *  - Diferente da SimulationRepository (que cacheia `SimulationResult`
 *    pesado com histograma), o uso tipico aqui eh "rodou simulacao
 *    calibrada uma vez, ja viu o resultado". Re-visitar e raro.
 *
 * Em sprints futuras, podemos adicionar `observeRecentPredictions`
 * baseado em Room se houver demanda. Por ora, `listRecent` eh request
 * direto.
 */
interface PredictionRepository {

    /**
     * Gera predicao de renda para o horizonte solicitado (1..60 meses).
     * Custo: 1-3s no servidor (treino + predict on-the-fly).
     */
    suspend fun predictIncome(horizonMonths: Int): DataResult<IncomePrediction>

    /**
     * Gera predicao de despesas categorizadas para o proximo periodo
     * (1..12 meses). Custo similar a [predictIncome].
     */
    suspend fun predictExpenses(horizonMonths: Int = 1): DataResult<ExpensePrediction>

    /**
     * Lista predicoes recentes do usuario (auditoria). Default 50.
     */
    suspend fun listRecent(limit: Int = 50): DataResult<List<PredictionSummary>>

    /**
     * Roda simulacao calibrada: backend faz predict_income + predict_expenses
     * + calibracao + 10k iteracoes de Monte Carlo. Custo total: 3-6s.
     *
     * Este eh o endpoint MAIS PESADO do app - timeout do OkHttp (60s)
     * deve estar generoso.
     */
    suspend fun runCalibrated(
        parameters: CalibratedSimulationParameters,
    ): DataResult<CalibratedSimulation>
}
