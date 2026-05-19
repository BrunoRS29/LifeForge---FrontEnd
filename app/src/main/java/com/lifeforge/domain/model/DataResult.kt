package com.lifeforge.domain.model

/**
 * Wrapper de resultado de operação que pode falhar (rede, validação,
 * autorização). É a contrapartida tipada do try/catch — força a UI a
 * lidar explicitamente com cada caso.
 *
 * Usado em todos os métodos `suspend` dos repositórios e use cases.
 *
 * Padrão de consumo na UI:
 *
 * ```
 * when (val outcome = useCase()) {
 *     is DataResult.Success -> render(outcome.data)
 *     is DataResult.Failure -> showError(outcome.error)
 * }
 * ```
 *
 * Preferido sobre `kotlin.Result` porque (a) carrega informação semântica
 * de erro e (b) é serializável/comparável trivialmente para testes.
 */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Failure(val error: AppError) : DataResult<Nothing>
}

/**
 * Categoriza erros de forma que a UI consiga decidir reação apropriada
 * sem inspecionar mensagens.
 */
sealed class AppError(open val message: String?) {

    /** Sem conexão, timeout, DNS — geralmente transitório. */
    data class Network(override val message: String? = null) : AppError(message)

    /** 401: token expirado/inválido. UI deve redirecionar para login. */
    data class Unauthorized(override val message: String? = null) : AppError(message)

    /** 404: recurso não existe (ou pertence a outro usuário). */
    data class NotFound(override val message: String? = null) : AppError(message)

    /** 400: validação no backend. `field` opcional para destacar input. */
    data class Validation(
        val field: String? = null,
        override val message: String? = null,
    ) : AppError(message)

    /** 409: conflito (ex: e-mail já cadastrado no register). */
    data class Conflict(override val message: String? = null) : AppError(message)

    /** 5xx: erro interno do servidor. */
    data class Server(val statusCode: Int, override val message: String? = null) : AppError(message)

    /** Falha de parsing ou erro inesperado. Ideal capturar em logging. */
    data class Unknown(val cause: Throwable? = null) : AppError(cause?.message)
}

/** Helpers para encadeamento funcional. */
inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(data))
    is DataResult.Failure -> this
}

/**
 * Igual a [map], mas captura exceções da transformação e devolve
 * [AppError.Unknown]. Usado em pipelines de repositório onde a etapa
 * pós-rede (parse de DTO → domain, escrita em Room) pode falhar.
 *
 * `CancellationException` é re-lançada para preservar a estrutura de
 * cooperative cancellation das coroutines.
 */

suspend inline fun <T, R> DataResult<T>.mapCatching(
    crossinline transform: suspend (T) -> R,
): DataResult<R> = when (this) {
    is DataResult.Success -> try {
        DataResult.Success(transform(data))
    } catch (ce: kotlinx.coroutines.CancellationException) {
        throw ce
    } catch (t: Throwable) {
        DataResult.Failure(AppError.Unknown(t))
    }
    is DataResult.Failure -> this
}

inline fun <T> DataResult<T>.onSuccess(action: (T) -> Unit): DataResult<T> {
    if (this is DataResult.Success) action(data)
    return this
}

inline fun <T> DataResult<T>.onFailure(action: (AppError) -> Unit): DataResult<T> {
    if (this is DataResult.Failure) action(error)
    return this
}
