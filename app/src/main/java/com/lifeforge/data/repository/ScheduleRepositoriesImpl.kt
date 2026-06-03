package com.lifeforge.data.repository

import com.lifeforge.data.api.ExpenseApi
import com.lifeforge.data.api.IncomeApi
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.mapper.toRequestDto
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.ExpenseSchedule
import com.lifeforge.domain.model.ExpenseScheduleParams
import com.lifeforge.domain.model.IncomeSchedule
import com.lifeforge.domain.model.IncomeScheduleParams
import com.lifeforge.domain.model.ScheduleAffect
import com.lifeforge.domain.model.mapCatching
import com.lifeforge.domain.repository.ExpenseScheduleRepository
import com.lifeforge.domain.repository.IncomeScheduleRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositórios de schedule (Sprint 6). Network-only, mesmo padrão do
 * [com.lifeforge.data.repository.PredictionRepositoryImpl]: safeApiCall +
 * mapCatching, sem Room. Os registros materializados pelo backend chegam
 * pela lista normal de receitas/despesas (essa sim cacheada).
 */

@Singleton
class IncomeScheduleRepositoryImpl @Inject constructor(
    private val api: IncomeApi,
    private val json: Json,
) : IncomeScheduleRepository {

    override suspend fun list(): DataResult<List<IncomeSchedule>> =
        safeApiCall(json) { api.listSchedules() }
            .mapCatching { list -> list.map { it.toDomain() } }

    override suspend fun create(params: IncomeScheduleParams): DataResult<IncomeSchedule> =
        safeApiCall(json) { api.createSchedule(params.toRequestDto()) }
            .mapCatching { it.toDomain() }

    override suspend fun update(
        id: Long,
        params: IncomeScheduleParams,
        affect: ScheduleAffect,
    ): DataResult<IncomeSchedule> =
        safeApiCall(json) { api.updateSchedule(id, affect.name, params.toRequestDto()) }
            .mapCatching { it.toDomain() }

    override suspend fun delete(id: Long, affect: ScheduleAffect): DataResult<Unit> =
        safeApiCall(json) { api.deleteSchedule(id, affect.name) }
}

@Singleton
class ExpenseScheduleRepositoryImpl @Inject constructor(
    private val api: ExpenseApi,
    private val json: Json,
) : ExpenseScheduleRepository {

    override suspend fun list(): DataResult<List<ExpenseSchedule>> =
        safeApiCall(json) { api.listSchedules() }
            .mapCatching { list -> list.map { it.toDomain() } }

    override suspend fun create(params: ExpenseScheduleParams): DataResult<ExpenseSchedule> =
        safeApiCall(json) { api.createSchedule(params.toRequestDto()) }
            .mapCatching { it.toDomain() }

    override suspend fun update(
        id: Long,
        params: ExpenseScheduleParams,
        affect: ScheduleAffect,
    ): DataResult<ExpenseSchedule> =
        safeApiCall(json) { api.updateSchedule(id, affect.name, params.toRequestDto()) }
            .mapCatching { it.toDomain() }

    override suspend fun delete(id: Long, affect: ScheduleAffect): DataResult<Unit> =
        safeApiCall(json) { api.deleteSchedule(id, affect.name) }
}
