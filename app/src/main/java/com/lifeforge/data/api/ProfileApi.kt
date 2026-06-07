package com.lifeforge.data.api

import com.lifeforge.data.model.dto.UserProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/**
 * Perfil estendido do usuário autenticado. O AuthInterceptor injeta o Bearer.
 *  GET /profile  -> perfil salvo (ou objeto vazio)
 *  PUT /profile  -> substitui o perfil
 */
interface ProfileApi {

    @GET("profile")
    suspend fun getProfile(): Response<UserProfileDto>

    @PUT("profile")
    suspend fun updateProfile(@Body body: UserProfileDto): Response<UserProfileDto>
}
