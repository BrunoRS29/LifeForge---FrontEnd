package com.lifeforge.data.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor que injeta `Authorization: Bearer <token>` em todas as
 * chamadas de rede, exceto nas rotas públicas de autenticação.
 *
 * - `runBlocking` é aceitável aqui: OkHttp já chama o interceptor numa
 *   thread de IO, e a leitura do DataStore é uma única operação rápida
 *   sobre um arquivo pequeno (preferences).
 * - Se o token estiver ausente para uma rota privada, deixamos o request
 *   ir mesmo assim — o servidor responderá 401, que é mapeado para
 *   [com.lifeforge.domain.model.AppError.Unauthorized] na camada de
 *   repositório.
 * - Se uma rota privada responde 401 a um token enviado, o token expirou (ou
 *   foi invalidado): ele é removido, a sessão observada vira nula e a UI volta
 *   ao login. O banco local NÃO é apagado aqui — o cache e as alterações ainda
 *   não sincronizadas sobrevivem e sobem quando o mesmo usuário entrar de novo.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Rotas que NÃO devem carregar o token (login/register).
        // Match por sufixo do path para ser robusto contra mudanças
        // no base URL (debug vs release).
        val pathSegments = original.url.encodedPathSegments
        val isPublicAuth = pathSegments.size >= 2 &&
            pathSegments[pathSegments.size - 2] == "auth" &&
            pathSegments.last() in PUBLIC_AUTH_ENDPOINTS

        if (isPublicAuth) {
            return chain.proceed(original)
        }

        val token = runBlocking { tokenStore.getToken() }

        if (token.isNullOrBlank()) {
            return chain.proceed(original)
        }

        val request = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        val response = chain.proceed(request)

        if (response.code == HTTP_UNAUTHORIZED) {
            runBlocking { tokenStore.clearIfCurrent(token) }
        }
        return response
    }

    private companion object {
        val PUBLIC_AUTH_ENDPOINTS = setOf("login", "register")
        const val HTTP_UNAUTHORIZED = 401
    }
}
