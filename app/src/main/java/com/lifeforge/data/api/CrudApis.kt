package com.lifeforge.data.api

import com.lifeforge.data.model.dto.AssetDto
import com.lifeforge.data.model.dto.AssetRequestDto
import com.lifeforge.data.model.dto.ExpenseDto
import com.lifeforge.data.model.dto.ExpenseRequestDto
import com.lifeforge.data.model.dto.IncomeDto
import com.lifeforge.data.model.dto.IncomeRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * APIs CRUD agrupadas — Income, Expense e Asset têm shape muito similar.
 * Mantê-las num arquivo evita 3 arquivos quase vazios.
 */

interface IncomeApi {

    @GET("incomes")
    suspend fun list(): Response<List<IncomeDto>>

    @GET("incomes/{id}")
    suspend fun getById(@Path("id") id: Long): Response<IncomeDto>

    @POST("incomes")
    suspend fun create(@Body body: IncomeRequestDto): Response<IncomeDto>

    @DELETE("incomes/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>
}

interface ExpenseApi {

    @GET("expenses")
    suspend fun list(): Response<List<ExpenseDto>>

    @GET("expenses/{id}")
    suspend fun getById(@Path("id") id: Long): Response<ExpenseDto>

    @POST("expenses")
    suspend fun create(@Body body: ExpenseRequestDto): Response<ExpenseDto>

    @DELETE("expenses/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>
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
