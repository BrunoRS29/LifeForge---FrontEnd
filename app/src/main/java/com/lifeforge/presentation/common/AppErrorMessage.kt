package com.lifeforge.presentation.common

import com.lifeforge.domain.model.AppError

/**
 * Converte [AppError] para uma mensagem user-friendly em PT-BR.
 *
 * Mensagens deliberadamente curtas e acionáveis — quando há ação
 * sugerida (ex.: "verifique sua conexão"), ela aparece. Para erros
 * genéricos do servidor, evitamos expor detalhes técnicos.
 *
 * Casos específicos por tela (ex.: 401 no login = "email ou senha
 * incorretos") devem ser tratados antes de cair aqui — esta função
 * é o fallback genérico.
 */
fun AppError.toUserMessage(): String = when (this) {
    is AppError.Network -> "Sem conexão. Verifique sua internet e tente de novo."
    is AppError.Unauthorized -> "Sua sessão expirou. Faça login novamente."
    is AppError.NotFound -> "Recurso não encontrado."
    is AppError.Validation -> message ?: "Dados inválidos."
    is AppError.Conflict -> message ?: "Conflito com dados existentes."
    is AppError.Server -> "Erro no servidor. Tente novamente em alguns instantes."
    is AppError.Unknown -> "Algo deu errado. Tente novamente."
}
