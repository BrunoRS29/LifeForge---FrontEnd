package com.lifeforge.presentation.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.math.BigDecimal
import java.math.RoundingMode

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
 * `volatility`, e os parâmetros Double da Optimization. [prefix] e [suffix]
 * ficam fixos ao lado do número (ex.: "R$", "%"), e [supportingText] explica
 * o campo quando não há erro.
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
    prefix: String? = null,
    suffix: String? = null,
    supportingText: String? = null,
    groupThousands: Boolean = false,
) {
    val supporting = error ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (groupThousands) ThousandsSeparatorTransformation else VisualTransformation.None,
        prefix = prefix?.let { { Text("$it\u00A0") } },
        suffix = suffix?.let { { Text("\u00A0$it") } },
        isError = error != null,
        supportingText = supporting?.let { { Text(it) } },
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
 * Campo de valor em reais: o "R$" fica fixo à esquerda do número e os milhares
 * aparecem separados enquanto se digita ("2.500.000,00").
 */
@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    supportingText: String? = null,
) = CurrencyField(
    value = value,
    onValueChange = onValueChange,
    label = label,
    modifier = modifier,
    error = error,
    imeAction = imeAction,
    enabled = enabled,
    prefix = "R$",
    supportingText = supportingText,
    groupThousands = true,
)

/**
 * Campo de taxa em porcentagem ("8,25 %"): o usuário digita a porcentagem e o
 * ViewModel converte para fração com [parsePercentInput].
 */
@Composable
fun PercentField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    supportingText: String? = null,
    suffix: String = "%",
) = CurrencyField(
    value = value,
    onValueChange = onValueChange,
    label = label,
    modifier = modifier,
    error = error,
    imeAction = imeAction,
    enabled = enabled,
    suffix = suffix,
    supportingText = supportingText,
)

/** Valor do domínio → texto de um campo de R$ em PT-BR: 403.27 → "403,27". */
fun BigDecimal.toMoneyInput(): String = toPlainString().replace('.', ',')

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

/**
 * Fração → texto para um campo de porcentagem: 0.0825 → "8,25"; 0.15 → "15".
 * Até 4 casas, sem zeros à direita (evita ruído de ponto flutuante na tela).
 */
fun fractionToPercentInput(fraction: BigDecimal): String =
    fraction.movePointRight(2)
        .setScale(4, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
        .replace('.', ',')

fun fractionToPercentInput(fraction: Double): String = fractionToPercentInput(BigDecimal.valueOf(fraction))

/** Porcentagem digitada → fração: "8,25" → 0.0825. `null` se não for um número. */
fun parsePercentInput(input: String): BigDecimal? = parseCurrencyInput(input)?.movePointLeft(2)

/** Versão Double de [parsePercentInput] — para os parâmetros do motor estatístico. */
fun parsePercentInputAsDouble(input: String): Double? = parsePercentInput(input)?.toDouble()

/**
 * Agrupa os milhares da parte inteira de um número digitado em PT-BR
 * ("2500000,5" → "2.500.000,5"). Texto com ponto fica como está: o usuário já
 * escolheu a pontuação (milhar ou decimal no formato americano).
 */
fun groupThousands(raw: String): String = ThousandsGrouping.of(raw).text

/**
 * O texto agrupado e as tabelas de posição do cursor entre o texto guardado
 * (sem pontos) e o exibido (com pontos).
 */
internal class ThousandsGrouping private constructor(
    val text: String,
    private val originalToShown: IntArray,
    private val shownToOriginal: IntArray,
) {
    fun toShown(offset: Int): Int = originalToShown[offset.coerceIn(0, originalToShown.lastIndex)]
    fun toOriginal(offset: Int): Int = shownToOriginal[offset.coerceIn(0, shownToOriginal.lastIndex)]

    companion object {
        fun of(raw: String): ThousandsGrouping {
            if (raw.isEmpty() || raw.contains('.')) {
                val identity = IntArray(raw.length + 1) { it }
                return ThousandsGrouping(raw, identity, identity)
            }
            val integerEnd = raw.indexOf(',').let { if (it < 0) raw.length else it }
            val out = StringBuilder()
            val originalToShown = IntArray(raw.length + 1)
            for (i in raw.indices) {
                if (i in 1 until integerEnd && (integerEnd - i) % 3 == 0) out.append('.')
                originalToShown[i] = out.length
                out.append(raw[i])
            }
            originalToShown[raw.length] = out.length
            val shownToOriginal = IntArray(out.length + 1)
            var count = 0
            for (t in out.indices) {
                shownToOriginal[t] = count
                if (out[t] != '.') count++
            }
            shownToOriginal[out.length] = count
            return ThousandsGrouping(out.toString(), originalToShown, shownToOriginal)
        }
    }
}

/** Exibe os milhares agrupados sem alterar o valor guardado no estado. */
private object ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val grouping = ThousandsGrouping.of(text.text)
        return TransformedText(
            AnnotatedString(grouping.text),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = grouping.toShown(offset)
                override fun transformedToOriginal(offset: Int): Int = grouping.toOriginal(offset)
            },
        )
    }
}
