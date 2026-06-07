package com.lifeforge.data.repository

import com.lifeforge.data.api.ProfileApi
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.mapper.toDto
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.UserProfile
import com.lifeforge.domain.model.mapCatching
import com.lifeforge.domain.repository.UserProfileRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Perfil estendido — pass-through para a API (sem cache Room; é um único
 * registro por usuário, lido na tela de Perfil).
 */
@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val api: ProfileApi,
    private val json: Json,
) : UserProfileRepository {

    override suspend fun getProfile(): DataResult<UserProfile> =
        safeApiCall(json) { api.getProfile() }.mapCatching { it.toDomain() }

    override suspend fun updateProfile(profile: UserProfile): DataResult<UserProfile> =
        safeApiCall(json) { api.updateProfile(profile.toDto()) }.mapCatching { it.toDomain() }
}
