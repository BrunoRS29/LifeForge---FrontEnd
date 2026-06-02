package com.lifeforge.data.util

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Cobre os caminhos de mapeamento de erros HTTP → [AppError] e o caminho
 * feliz. Não exercita Retrofit/OkHttp reais — usa [Response] sintéticos.
 */
class SafeApiCallTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ------------------------------------------------------------------------
    // Caminho feliz
    // ------------------------------------------------------------------------

    @Test
    fun `200 com body retorna Success`() = runTest {
        val result = safeApiCall(json) { Response.success("ok") }

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        assertThat((result as DataResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `204 sem body ainda assim retorna Success`() = runTest {
        // Endpoints DELETE retornam 204 — Response<Unit> com body null é OK
        // Especificamos explicitamente que o segundo parâmetro é o corpo (body)
        val response: Response<Unit> = Response.success(204, null as Unit?) 
        val result = safeApiCall(json) { response }

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
    }

    // ------------------------------------------------------------------------
    // Mapeamento de status HTTP
    // ------------------------------------------------------------------------

    @Test
    fun `400 com ErrorResponseDto retorna Validation com a mensagem`() = runTest {
        val errorJson = """{"error":"VALIDATION","message":"targetAmount deve ser > 0"}"""
        val response = errorResponse<String>(code = 400, body = errorJson)

        val result = safeApiCall(json) { response }

        assertThat(result).isInstanceOf(DataResult.Failure::class.java)
        val error = (result as DataResult.Failure).error
        assertThat(error).isInstanceOf(AppError.Validation::class.java)
        assertThat(error.message).isEqualTo("targetAmount deve ser > 0")
        assertThat((error as AppError.Validation).field).isEqualTo("VALIDATION")
    }

    @Test
    fun `422 com ErrorResponseDto retorna Validation com a mensagem`() = runTest {
        // O backend usa 422 (Unprocessable Entity) quando os dados do usuário
        // são insuficientes para a IA — ex.: histórico de renda < 6 registros.
        // Antes do fix, 422 caía no else -> Unknown -> "Algo deu errado",
        // escondendo a mensagem acionável que o backend já mandava.
        val errorJson =
            """{"error":"INSUFFICIENT_DATA","message":"Historico de renda precisa de >= 6 registros (atualmente 3)."}"""
        val response = errorResponse<String>(code = 422, body = errorJson)

        val result = safeApiCall(json) { response }

        assertThat(result).isInstanceOf(DataResult.Failure::class.java)
        val error = (result as DataResult.Failure).error
        assertThat(error).isInstanceOf(AppError.Validation::class.java)
        assertThat(error.message)
            .isEqualTo("Historico de renda precisa de >= 6 registros (atualmente 3).")
        assertThat((error as AppError.Validation).field).isEqualTo("INSUFFICIENT_DATA")
    }

    @Test
    fun `401 retorna Unauthorized`() = runTest {
        val response = errorResponse<String>(code = 401, body = """{"error":"UNAUTH","message":"token expirou"}""")
        val result = safeApiCall(json) { response }

        assertThat((result as DataResult.Failure).error).isInstanceOf(AppError.Unauthorized::class.java)
    }

    @Test
    fun `404 retorna NotFound`() = runTest {
        val response = errorResponse<String>(code = 404, body = """{"error":"NOT_FOUND","message":"meta nao encontrada"}""")
        val result = safeApiCall(json) { response }

        assertThat((result as DataResult.Failure).error).isInstanceOf(AppError.NotFound::class.java)
    }

    @Test
    fun `409 retorna Conflict`() = runTest {
        val response = errorResponse<String>(code = 409, body = """{"error":"EMAIL_TAKEN","message":"email ja existe"}""")
        val result = safeApiCall(json) { response }

        assertThat((result as DataResult.Failure).error).isInstanceOf(AppError.Conflict::class.java)
    }

    @Test
    fun `500 retorna Server com statusCode preservado`() = runTest {
        val response = errorResponse<String>(code = 500, body = """{"error":"INTERNAL","message":"db caiu"}""")
        val result = safeApiCall(json) { response }

        val error = (result as DataResult.Failure).error
        assertThat(error).isInstanceOf(AppError.Server::class.java)
        assertThat((error as AppError.Server).statusCode).isEqualTo(500)
    }

    // ------------------------------------------------------------------------
    // Body de erro malformado — não pode quebrar
    // ------------------------------------------------------------------------

    @Test
    fun `400 sem ErrorResponseDto valido ainda assim mapeia para Validation`() = runTest {
        val response = errorResponse<String>(code = 400, body = "<html>500 internal</html>")
        val result = safeApiCall(json) { response }

        assertThat((result as DataResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
    }

    // ------------------------------------------------------------------------
    // Exceções
    // ------------------------------------------------------------------------

    @Test
    fun `IOException retorna Network`() = runTest {
        val result = safeApiCall<String>(json) {
            throw IOException("dns fail")
        }

        val error = (result as DataResult.Failure).error
        assertThat(error).isInstanceOf(AppError.Network::class.java)
        assertThat(error.message).isEqualTo("dns fail")
    }

    @Test
    fun `Throwable generico retorna Unknown`() = runTest {
        val result = safeApiCall<String>(json) {
            throw IllegalStateException("boom")
        }

        assertThat((result as DataResult.Failure).error).isInstanceOf(AppError.Unknown::class.java)
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private fun <T> errorResponse(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))
}
