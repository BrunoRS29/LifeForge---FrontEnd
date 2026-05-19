package com.lifeforge.data.util

import com.lifeforge.data.model.dto.ErrorResponseDto
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

/**
 * Encapsula uma chamada Retrofit e devolve [DataResult] tipado, traduzindo
 * exceções e códigos HTTP em [AppError] sem vazar detalhes de framework
 * para a camada de domínio.
 *
 * Padrão de uso:
 *
 * ```
 * override suspend fun create(...): DataResult<Goal> = safeApiCall(json) {
 *     api.create(GoalRequestDto(...))
 * }.map { it.toDomain() }
 * ```
 *
 * Trata explicitamente:
 * - [CancellationException]: re-lança (cancelamento de coroutine não é erro).
 * - [java.io.IOException]: falha de rede (timeout, DNS, conexão recusada).
 * - [HttpException]: status >= 400 (mapeado por código).
 * - [SerializationException]: parsing JSON inválido.
 * - Qualquer outra: [AppError.Unknown].
 *
 * @param json Json injetado para parsear o ErrorResponseDto do backend.
 * @param block lambda suspend que executa a chamada Retrofit.
 */
suspend inline fun <T> safeApiCall(
    json: Json,
    crossinline block: suspend () -> Response<T>,
): DataResult<T> = try {
    val response = block()
    when {
        response.isSuccessful -> {
            val body = response.body()
            if (body == null && response.code() != 204) {
                DataResult.Failure(
                    AppError.Unknown(
                        IllegalStateException("Body vazio em resposta ${response.code()}")
                    )
                )
            } else {
                @Suppress("UNCHECKED_CAST")
                DataResult.Success(body ?: Unit as T)
            }
        }
        else -> DataResult.Failure(parseHttpError(response, json))
    }
} catch (ce: CancellationException) {
    throw ce
} catch (io: IOException) {
    Timber.w(io, "Falha de rede")
    DataResult.Failure(AppError.Network(io.message))
} catch (http: HttpException) {
    // Em geral safeApiCall recebe Response<T>, então HttpException não chega aqui;
    // mantemos por defesa caso alguém migre para call adapter que lance.
    DataResult.Failure(AppError.Server(http.code(), http.message()))
} catch (se: SerializationException) {
    Timber.e(se, "JSON inválido")
    DataResult.Failure(AppError.Unknown(se))
} catch (t: Throwable) {
    Timber.e(t, "Erro inesperado em chamada de rede")
    DataResult.Failure(AppError.Unknown(t))
}

/**
 * Converte um Response não-OK em [AppError] semanticamente correto,
 * tentando extrair o [ErrorResponseDto] do corpo se estiver presente.
 */
@PublishedApi
internal fun parseHttpError(response: Response<*>, json: Json): AppError {
    val errorBody = response.errorBody()?.string().orEmpty()
    val parsed: ErrorResponseDto? = if (errorBody.isNotBlank()) {
        runCatching { json.decodeFromString<ErrorResponseDto>(errorBody) }.getOrNull()
    } else null

    val message = parsed?.message ?: response.message().ifBlank { "HTTP ${response.code()}" }
    val errorCode = parsed?.error

    return when (response.code()) {
        400 -> AppError.Validation(field = errorCode, message = message)
        401 -> AppError.Unauthorized(message)
        403 -> AppError.Unauthorized(message)
        404 -> AppError.NotFound(message)
        409 -> AppError.Conflict(message)
        in 500..599 -> AppError.Server(response.code(), message)
        else -> AppError.Unknown(IllegalStateException(message))
    }
}
