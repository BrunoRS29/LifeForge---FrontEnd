package com.lifeforge.domain.repository

import com.lifeforge.domain.model.Asset
import com.lifeforge.domain.model.AssetType
import com.lifeforge.domain.model.AuthSession
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Expense
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.domain.model.ExpenseSchedule
import com.lifeforge.domain.model.ExpenseScheduleParams
import com.lifeforge.domain.model.Income
import com.lifeforge.domain.model.IncomeSchedule
import com.lifeforge.domain.model.IncomeScheduleParams
import com.lifeforge.domain.model.IncomeType
import com.lifeforge.domain.model.ScheduleAffect
import com.lifeforge.domain.model.OptimizationResult
import com.lifeforge.domain.model.RebalanceResult
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.model.SimulationParameters
import com.lifeforge.domain.model.SimulationResult
import com.lifeforge.domain.model.SimulationSummary
import com.lifeforge.domain.model.User
import com.lifeforge.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.Instant

/**
 * Contratos de repositório expostos para a camada `domain` e `presentation`.
 *
 * **Padrão offline-first:**
 * - Métodos `observeXxx()` retornam Flow do Room (single source of truth).
 *   A UI sempre observa essa fonte; nunca espera por uma chamada de rede.
 * - Métodos `refreshXxx()` disparam sync com a API e atualizam o Room;
 *   o Flow auto-emite o novo estado.
 * - Mutações (create/update/delete) são otimistas: gravam no Room
 *   imediatamente e tentam replicar no servidor; em caso de falha de
 *   rede, ficam marcadas como `pending_sync` (Fase 4.1b).
 */

// ===========================================================================
// Auth — não tem cache local
// ===========================================================================

interface AuthRepository {
    suspend fun register(
        email: String,
        name: String,
        password: String,
        riskProfile: RiskProfile?,
    ): DataResult<AuthSession>

    suspend fun login(email: String, password: String): DataResult<AuthSession>

    suspend fun logout()

    /** Sessão atual observável — emite null quando não há token válido. */
    fun observeSession(): Flow<AuthSession?>
}

// ===========================================================================
// User — perfil do usuário logado
// ===========================================================================

interface UserRepository {
    fun observeCurrentUser(): Flow<User?>
    suspend fun refreshCurrentUser(): DataResult<User>

    suspend fun updateRiskProfile(profile: RiskProfile): DataResult<User>

    /** Troca o nome de exibição do usuário (PATCH /users/me/name). */
    suspend fun updateName(name: String): DataResult<User>
}

// ===========================================================================
// User profile — parâmetros estendidos para projeções (network-only)
// ===========================================================================

interface UserProfileRepository {
    suspend fun getProfile(): DataResult<UserProfile>
    suspend fun updateProfile(profile: UserProfile): DataResult<UserProfile>
}

// ===========================================================================
// Goals — CRUD com cache offline
// ===========================================================================

interface GoalRepository {
    fun observeAll(): Flow<List<Goal>>
    fun observeById(id: Long): Flow<Goal?>

    suspend fun refresh(): DataResult<Unit>

    suspend fun create(
        name: String,
        category: GoalCategory,
        targetAmount: BigDecimal,
        targetDate: Instant,
        priority: Int,
    ): DataResult<Goal>

    suspend fun update(
        id: Long,
        name: String,
        category: GoalCategory,
        targetAmount: BigDecimal,
        targetDate: Instant,
        priority: Int,
    ): DataResult<Goal>

    suspend fun delete(id: Long): DataResult<Unit>
}

// ===========================================================================
// Incomes
// ===========================================================================

interface IncomeRepository {
    fun observeAll(): Flow<List<Income>>
    suspend fun refresh(): DataResult<Unit>

    suspend fun create(
        source: String,
        amount: BigDecimal,
        incomeType: IncomeType,
        recurring: Boolean,
        receivedAt: Instant,
    ): DataResult<Income>

    suspend fun update(
        id: Long,
        source: String,
        amount: BigDecimal,
        incomeType: IncomeType,
        recurring: Boolean,
        receivedAt: Instant,
    ): DataResult<Income>

    suspend fun delete(id: Long): DataResult<Unit>

    /** Remove TODAS as receitas do usuário (backend + cache local). */
    suspend fun deleteAll(): DataResult<Unit>
}

// ===========================================================================
// Expenses
// ===========================================================================

interface ExpenseRepository {
    fun observeAll(): Flow<List<Expense>>
    suspend fun refresh(): DataResult<Unit>

    suspend fun create(
        description: String,
        amount: BigDecimal,
        category: ExpenseCategory,
        recurring: Boolean,
        spentAt: Instant,
    ): DataResult<Expense>

    suspend fun update(
        id: Long,
        description: String,
        amount: BigDecimal,
        category: ExpenseCategory,
        recurring: Boolean,
        spentAt: Instant,
    ): DataResult<Expense>

    suspend fun delete(id: Long): DataResult<Unit>

    /** Remove TODAS as despesas do usuário (backend + cache local). */
    suspend fun deleteAll(): DataResult<Unit>
}

// ===========================================================================
// Income / Expense schedules (Sprint 6) — network-only (sem cache Room)
// ===========================================================================

interface IncomeScheduleRepository {
    suspend fun list(): DataResult<List<IncomeSchedule>>
    suspend fun create(params: IncomeScheduleParams): DataResult<IncomeSchedule>
    suspend fun update(id: Long, params: IncomeScheduleParams, affect: ScheduleAffect): DataResult<IncomeSchedule>
    suspend fun delete(id: Long, affect: ScheduleAffect): DataResult<Unit>
}

interface ExpenseScheduleRepository {
    suspend fun list(): DataResult<List<ExpenseSchedule>>
    suspend fun create(params: ExpenseScheduleParams): DataResult<ExpenseSchedule>
    suspend fun update(id: Long, params: ExpenseScheduleParams, affect: ScheduleAffect): DataResult<ExpenseSchedule>
    suspend fun delete(id: Long, affect: ScheduleAffect): DataResult<Unit>
}

// ===========================================================================
// Assets
// ===========================================================================

interface AssetRepository {
    fun observeAll(): Flow<List<Asset>>
    suspend fun refresh(): DataResult<Unit>

    suspend fun create(
        name: String,
        assetType: AssetType,
        currentValue: BigDecimal,
        expectedReturn: BigDecimal,
        volatility: BigDecimal,
    ): DataResult<Asset>

    suspend fun update(
        id: Long,
        name: String,
        assetType: AssetType,
        currentValue: BigDecimal,
        expectedReturn: BigDecimal,
        volatility: BigDecimal,
    ): DataResult<Asset>

    suspend fun delete(id: Long): DataResult<Unit>
}

// ===========================================================================
// Simulation — write goes to network (compute pesado), result é cacheado
// ===========================================================================

interface SimulationRepository {
    /** Executa Monte Carlo no backend e persiste o resultado em cache. */
    suspend fun run(parameters: SimulationParameters): DataResult<SimulationResult>

    /** Recupera resultado completo do cache (e refresca se ausente). */
    suspend fun getById(id: Long): DataResult<SimulationResult>

    /** Lista resumida das simulações de uma meta. */
    fun observeByGoal(goalId: Long): Flow<List<SimulationSummary>>

    suspend fun refreshByGoal(goalId: Long): DataResult<Unit>

    suspend fun delete(id: Long): DataResult<Unit>
}

// ===========================================================================
// Optimization — não persiste, sempre roda no servidor
// ===========================================================================

interface OptimizationRepository {
    /** Aporte ideal para atingir alvo com probabilidade [targetSuccessProbability]. */
    suspend fun optimizeContribution(
        goalId: Long? = null,
        initialCapital: Double,
        expectedReturnAnnual: Double,
        volatilityAnnual: Double,
        targetAmount: Double,
        horizonMonths: Int,
        targetSuccessProbability: Double = 0.80,
        seed: Long? = null,
    ): DataResult<OptimizationResult>

    /** Horizonte ideal dado um aporte fixo. */
    suspend fun optimizeHorizon(
        goalId: Long? = null,
        initialCapital: Double,
        expectedReturnAnnual: Double,
        volatilityAnnual: Double,
        targetAmount: Double,
        monthlyContribution: Double,
        targetSuccessProbability: Double = 0.80,
        seed: Long? = null,
    ): DataResult<OptimizationResult>

    /** Sugestão de alocação por classe de ativo. */
    suspend fun rebalance(
        riskProfile: RiskProfile,
        currentCapital: Double,
        targetAmount: Double,
        monthsToGoal: Int,
    ): DataResult<RebalanceResult>
}
