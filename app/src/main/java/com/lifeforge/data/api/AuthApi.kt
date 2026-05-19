package com.lifeforge.data.api

import com.lifeforge.data.model.dto.AuthResponseDto
import com.lifeforge.data.model.dto.LoginRequestDto
import com.lifeforge.data.model.dto.RegisterRequestDto
import com.lifeforge.data.model.dto.UpdateRiskProfileRequestDto
import com.lifeforge.data.model.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

/**
 * Endpoints públicos de autenticação. Sem JWT no header
 * (o [com.lifeforge.data.auth.AuthInterceptor] sabe disso).
 *
 * - 201 Created → AuthResponseDto
 * - 400 BadRequest, 409 Conflict → ErrorResponseDto via errorBody
 */
interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<AuthResponseDto>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<AuthResponseDto>
}

/**
 * Endpoints autenticados sobre o usuário corrente.
 * O AuthInterceptor injeta `Authorization: Bearer <token>` automaticamente.
 */
interface UserApi {

    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @PATCH("users/me/risk-profile")
    suspend fun updateRiskProfile(
        @Body body: UpdateRiskProfileRequestDto,
    ): Response<UserDto>
}
