package com.lifeforge.presentation.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import java.math.BigDecimal

/**
 * Campo de input monetário/numérico em PT-BR.
 *
 * Aceita apenas dígitos, vírgula e ponto (filtragem feita pelo
 * ViewModel via [sanitizeCurrencyInput]). O parse de vírgula/ponto
 * usa [parseCurrencyInput] que normaliza para o formato esperado
 * pelo construtor de [BigDecimal].
 *
 * Genérico o suficiente para `targetAmount` (Goal), `amount`
 * (Income/Expense), `currentValue` (Asset), `expectedReturn`,
 * `volatility`, e os parâmetros Double da Optimization.
 */
@Composable
fun CurrencyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction,
        ),
        singleLine = true,
        enabled = enabled,
        modifier = modifier,
    )
}

/**
 * Filtra a entrada do usuário aceitando apenas dígitos, vírgula e
 * ponto. Chamado pelo ViewModel no callback `onValueChange` antes de
 * atualizar o state — evita o usuário inserir letras por engano.
 */
fun sanitizeCurrencyInput(input: String): String =
    input.filter { it.isDigit() || it == ',' || it == '.' }

/**
 * Normaliza um input PT-BR ("1.500,00") para o formato aceito pelo
 * `BigDecimal` ("1500.00"). Retorna `null` se a string não puder ser
 * parseada — o ViewModel mapeia null para erro de validação no campo.
 *
 * Estratégia: assume que vírgula sempre é o decimal (padrão PT-BR).
 * Pontos são tratados como separador de milhar e removidos antes do
 * parse. Se o usuário digitar `1500.50` (formato US, sem vírgula),
 * funciona também — o ponto sobrevive porque não há outro ponto que
 * indique que é milhar.
 */
fun parseCurrencyInput(input: String): BigDecimal? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null

    val normalized = if (trimmed.contains(',')) {
        // Vírgula presente: PT-BR. Pontos são milhar, vírgula é decimal.
        trimmed.replace(".", "").replace(",", ".")
    } else {
        // Sem vírgula: pode ser US (1500.50) ou inteiro com milhar (1.500).
        // Heurística: se há mais de um ponto, ou se o último ponto tem mais
        // de 2 dígitos depois, são separadores de milhar.
        val lastDot = trimmed.lastIndexOf('.')
        if (lastDot >= 0 && trimmed.length - lastDot - 1 > 2) {
            trimmed.replace(".", "")
        } else {
            trimmed
        }
    }

    return try {
        BigDecimal(normalized)
    } catch (e: NumberFormatException) {
        null
    }
}

/** Versão Double — para campos da Optimization (taxa anual, volatilidade, etc.). */
fun parseCurrencyInputAsDouble(input: String): Double? =
    parseCurrencyInput(input)?.toDouble()
