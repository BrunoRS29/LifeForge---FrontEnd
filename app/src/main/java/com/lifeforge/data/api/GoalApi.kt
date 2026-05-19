package com.lifeforge.data.api

import com.lifeforge.data.model.dto.GoalDto
import com.lifeforge.data.model.dto.GoalRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface GoalApi {

    @GET("goals")
    suspend fun list(): Response<List<GoalDto>>

    @GET("goals/{id}")
    suspend fun getById(@Path("id") id: Long): Response<GoalDto>

    @POST("goals")
    suspend fun create(@Body body: GoalRequestDto): Response<GoalDto>

    @PUT("goals/{id}")
    suspend fun update(
        @Path("id") id: Long,
        @Body body: GoalRequestDto,
    ): Response<GoalDto>

    @DELETE("goals/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>
}
