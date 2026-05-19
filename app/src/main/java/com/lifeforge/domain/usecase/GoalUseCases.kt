package com.lifeforge.domain.usecase

import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * UseCases do agregado Goal.
 *
 * `Create` e `Update` aplicam validação local antes de tocar a rede.
 * Falham rápido com [AppError.Validation] quando o usuário envia
 * dados claramente inválidos — economiza round-trip e simplifica a UI
 * (que pode mostrar a mensagem direto sem aguardar resposta HTTP).
 *
 * O [Clock] é injetado para permitir testes determinísticos da
 * validação "data alvo no futuro". Em produção o Hilt provê
 * `Clock.systemUTC()` (ver DispatcherModule).
 */

class ObserveGoalsUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    operator fun invoke(): Flow<List<Goal>> = repository.observeAll()
}

class ObserveGoalUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    operator fun invoke(id: Long): Flow<Goal?> = repository.observeById(id)
}

class RefreshGoalsUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(): DataResult<Unit> = repository.refresh()
}

class CreateGoalUseCase @Inject constructor(
    private val repository: GoalRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        name: String,
        category: GoalCategory,
        targetAmount: BigDecimal,
        targetDate: Instant,
        priority: Int,
    ): DataResult<Goal> {
        validateGoalFields(name, targetAmount, targetDate, priority, clock)
            ?.let { return it }
        return repository.create(name.trim(), category, targetAmount, targetDate, priority)
    }
}

class UpdateGoalUseCase @Inject constructor(
    private val repository: GoalRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        id: Long,
        name: String,
        category: GoalCategory,
        targetAmount: BigDecimal,
        targetDate: Instant,
        priority: Int,
    ): DataResult<Goal> {
        validateGoalFields(name, targetAmount, targetDate, priority, clock)
            ?.let { return it }
        return repository.update(id, name.trim(), category, targetAmount, targetDate, priority)
    }
}

class DeleteGoalUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(id: Long): DataResult<Unit> = repository.delete(id)
}

/**
 * Validações compartilhadas entre create e update. Retorna a primeira
 * falha encontrada ou `null` se tudo OK.
 */
private fun validateGoalFields(
    name: String,
    targetAmount: BigDecimal,
    targetDate: Instant,
    priority: Int,
    clock: Clock,
): DataResult.Failure? {
    if (name.isBlank()) {
        return DataResult.Failure(AppError.Validation("name", "nome da meta é obrigatório"))
    }
    if (targetAmount <= BigDecimal.ZERO) {
        return DataResult.Failure(AppError.Validation("targetAmount", "valor alvo deve ser positivo"))
    }
    if (!targetDate.isAfter(Instant.now(clock))) {
        return DataResult.Failure(AppError.Validation("targetDate", "data alvo deve estar no futuro"))
    }
    if (priority !in 1..10) {
        return DataResult.Failure(AppError.Validation("priority", "prioridade deve estar entre 1 e 10"))
    }
    return null
}
