package com.lifeforge.data.api

import com.lifeforge.data.model.dto.AssetDto
import com.lifeforge.data.model.dto.AssetRequestDto
import com.lifeforge.data.model.dto.ExpenseDto
import com.lifeforge.data.model.dto.ExpenseRequestDto
import com.lifeforge.data.model.dto.ExpenseScheduleDto
import com.lifeforge.data.model.dto.ExpenseScheduleRequestDto
import com.lifeforge.data.model.dto.IncomeDto
import com.lifeforge.data.model.dto.IncomeRequestDto
import com.lifeforge.data.model.dto.IncomeScheduleDto
import com.lifeforge.data.model.dto.IncomeScheduleRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * APIs CRUD agrupadas — Income, Expense e Asset têm shape muito similar.
 * Mantê-las num arquivo evita 3 arquivos quase vazios.
 *
 * Sprint 6: Income/Expense ganham subrotas /schedules (templates recorrentes).
 * Schedules são network-only no app — os registros gerados chegam pela lista
 * normal (já cacheada em Room).
 */

interface IncomeApi {

    @GET("incomes")
    suspend fun list(): Response<List<IncomeDto>>

    @GET("incomes/{id}")
    suspend fun getById(@Path("id") id: Long): Response<IncomeDto>

    @POST("incomes")
    suspend fun create(@Body body: IncomeRequestDto): Response<IncomeDto>

    @PUT("incomes/{id}")
    suspend fun update(
        @Path("id") id: Long,
        @Body body: IncomeRequestDto,
    ): Response<IncomeDto>

    @DELETE("incomes/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>

    @DELETE("incomes")
    suspend fun deleteAll(): Response<Unit>

    // ---- Schedules recorrentes ----

    @GET("incomes/schedules")
    suspend fun listSchedules(): Response<List<IncomeScheduleDto>>

    @POST("incomes/schedules")
    suspend fun createSchedule(@Body body: IncomeScheduleRequestDto): Response<IncomeScheduleDto>

    @PUT("incomes/schedules/{id}")
    suspend fun updateSchedule(
        @Path("id") id: Long,
        @Query("affect") affect: String,
        @Body body: IncomeScheduleRequestDto,
    ): Response<IncomeScheduleDto>

    @DELETE("incomes/schedules/{id}")
    suspend fun deleteSchedule(
        @Path("id") id: Long,
        @Query("affect") affect: String,
    ): Response<Unit>
}

interface ExpenseApi {

    @GET("expenses")
    suspend fun list(): Response<List<ExpenseDto>>

    @GET("expenses/{id}")
    suspend fun getById(@Path("id") id: Long): Response<ExpenseDto>

    @POST("expenses")
    suspend fun create(@Body body: ExpenseRequestDto): Response<ExpenseDto>

    @PUT("expenses/{id}")
    suspend fun update(
        @Path("id") id: Long,
        @Body body: ExpenseRequestDto,
    ): Response<ExpenseDto>

    @DELETE("expenses/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>

    @DELETE("expenses")
    suspend fun deleteAll(): Response<Unit>

    // ---- Schedules recorrentes ----

    @GET("expenses/schedules")
    suspend fun listSchedules(): Response<List<ExpenseScheduleDto>>

    @POST("expenses/schedules")
    suspend fun createSchedule(@Body body: ExpenseScheduleRequestDto): Response<ExpenseScheduleDto>

    @PUT("expenses/schedules/{id}")
    suspend fun updateSchedule(
        @Path("id") id: Long,
        @Query("affect") affect: String,
        @Body body: ExpenseScheduleRequestDto,
    ): Response<ExpenseScheduleDto>

    @DELETE("expenses/schedules/{id}")
    suspend fun deleteSchedule(
        @Path("id") id: Long,
        @Query("affect") affect: String,
    ): Response<Unit>
}

interface AssetApi {

    @GET("assets")
    suspend fun list(): Response<List<AssetDto>>

    @GET("assets/{id}")
    suspend fun getById(@Path("id") id: Long): Response<AssetDto>

    @POST("assets")
    suspend fun create(@Body body: AssetRequestDto): Response<AssetDto>

    @PUT("assets/{id}")
    suspend fun update(
        @Path("id") id: Long,
        @Body body: AssetRequestDto,
    ): Response<AssetDto>

    @DELETE("assets/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>
}
