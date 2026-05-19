package com.lifeforge.data.auth

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

/**
 * Verifica que o [AuthInterceptor]:
 * - Não injeta token nas rotas públicas (/auth/login, /auth/register).
 * - Injeta token nas demais rotas quando há token salvo.
 * - Deixa request original quando não há token.
 */
class AuthInterceptorTest {

    private val tokenStore: TokenStore = mockk()
    private val interceptor = AuthInterceptor(tokenStore)

    @Test
    fun `nao injeta token em auth login`() {
        val chain = mockChain("https://api.lifeforge.app/api/v1/auth/login")

        interceptor.intercept(chain)

        coVerify(exactly = 0) { tokenStore.getToken() }
        val sent = capturedRequest(chain)
        assertThat(sent.header("Authorization")).isNull()
    }

    @Test
    fun `nao injeta token em auth register`() {
        val chain = mockChain("https://api.lifeforge.app/api/v1/auth/register")

        interceptor.intercept(chain)

        coVerify(exactly = 0) { tokenStore.getToken() }
        val sent = capturedRequest(chain)
        assertThat(sent.header("Authorization")).isNull()
    }

    @Test
    fun `injeta Bearer token em rota privada quando token existe`() {
        coEvery { tokenStore.getToken() } returns "eyJhbGciOi..."
        val chain = mockChain("https://api.lifeforge.app/api/v1/goals")

        interceptor.intercept(chain)

        val sent = capturedRequest(chain)
        assertThat(sent.header("Authorization")).isEqualTo("Bearer eyJhbGciOi...")
    }

    @Test
    fun `nao injeta header quando token e null`() {
        coEvery { tokenStore.getToken() } returns null
        val chain = mockChain("https://api.lifeforge.app/api/v1/goals")

        interceptor.intercept(chain)

        val sent = capturedRequest(chain)
        assertThat(sent.header("Authorization")).isNull()
    }

    @Test
    fun `nao injeta header quando token e blank`() {
        coEvery { tokenStore.getToken() } returns "   "
        val chain = mockChain("https://api.lifeforge.app/api/v1/goals")

        interceptor.intercept(chain)

        val sent = capturedRequest(chain)
        assertThat(sent.header("Authorization")).isNull()
    }

    // ------------------------------------------------------------------------
    // Mocks compartilhados
    // ------------------------------------------------------------------------

    private val capturedRequestSlot = slot<Request>()

    private fun mockChain(url: String): Interceptor.Chain {
        val originalRequest = Request.Builder().url(url.toHttpUrl()).build()
        val mockResponse = Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody("application/json".toMediaType()))
            .build()

        return mockk<Interceptor.Chain>().apply {
            every { request() } returns originalRequest
            every { proceed(capture(capturedRequestSlot)) } returns mockResponse
        }
    }

    private fun capturedRequest(@Suppress("UNUSED_PARAMETER") chain: Interceptor.Chain): Request =
        capturedRequestSlot.captured
}
