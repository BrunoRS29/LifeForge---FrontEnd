package com.lifeforge.domain.imports

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Parsers dos formatos de extrato suportados. Puros (sem I/O) e testáveis:
 * recebem o conteúdo do arquivo já lido como String.
 *
 *  - Nubank: CSV `Data,Valor,Identificador,Descrição` — ponto decimal, com
 *    sinal, UUID por transação. A descrição é o último campo (pode conter
 *    vírgulas), por isso usamos split com limite 4.
 *  - Itaú: TXT sem cabeçalho `data;descrição;valor` — valor em formato BR
 *    (vírgula decimal, ponto de milhar), com sinal, sem identificador.
 *  - Fatura Nubank: CSV `date,title,amount` — data ISO (aaaa-mm-dd), valor BR
 *    entre aspas ("10,68", "1.128,11"). Compras são POSITIVAS; créditos
 *    (pagamento recebido, estorno) vêm negativos ("- 5.925,06"). Campos podem
 *    conter vírgula/aspas (ex.: `"IOF de ""Loja"""`), então usamos um split
 *    de CSV que respeita aspas.
 */
object StatementParser {

    private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun parse(bank: Bank, content: String, sourceFile: String): List<BankTransaction> =
        when (bank) {
            Bank.NUBANK -> parseNubank(content, sourceFile)
            Bank.ITAU -> parseItau(content, sourceFile)
        }

    fun parseNubank(content: String, sourceFile: String): List<BankTransaction> =
        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line -> parseNubankLine(line, sourceFile) }
            .toList()

    fun parseItau(content: String, sourceFile: String): List<BankTransaction> =
        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line -> parseItauLine(line, sourceFile) }
            .toList()

    /**
     * Fatura do Nubank (CSV `date,title,amount`). Devolve transações na
     * convenção interna (despesa = negativo): compra do cartão (positiva na
     * fatura) vira saída; crédito (pagamento/estorno, negativo na fatura) vira
     * positivo — a classificação trata esses como movimento interno.
     */
    fun parseNubankFatura(content: String, sourceFile: String): List<BankTransaction> =
        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line -> parseNubankFaturaLine(line, sourceFile) }
            .toList()

    private fun parseNubankFaturaLine(line: String, sourceFile: String): BankTransaction? {
        val parts = splitCsv(line)
        if (parts.size < 3) return null
        // Cabeçalho ("date,title,amount") cai fora porque a data não faz parse.
        val date = runCatching { LocalDate.parse(parts[0].trim()) }.getOrNull() ?: return null
        val title = parts[1].trim()
        val faturaAmount = parseFaturaAmount(parts[2]) ?: return null
        if (faturaAmount.signum() == 0) return null
        return BankTransaction(date, faturaAmount.negate(), title, null, Bank.NUBANK, sourceFile)
    }

    private fun parseNubankLine(line: String, sourceFile: String): BankTransaction? {
        val parts = line.split(",", limit = 4)
        if (parts.size < 4) return null
        // Cabeçalho ("Data,...") cai fora porque a data não faz parse.
        val date = runCatching { LocalDate.parse(parts[0].trim(), DATE) }.getOrNull() ?: return null
        val amount = parts[1].trim().toBigDecimalOrNull() ?: return null
        if (amount.signum() == 0) return null
        val externalId = parts[2].trim().ifBlank { null }
        val description = parts[3].trim()
        return BankTransaction(date, amount, description, externalId, Bank.NUBANK, sourceFile)
    }

    private fun parseItauLine(line: String, sourceFile: String): BankTransaction? {
        val parts = line.split(";", limit = 3)
        if (parts.size < 3) return null
        val date = runCatching { LocalDate.parse(parts[0].trim(), DATE) }.getOrNull() ?: return null
        val description = parts[1].trim()
        val amount = parseBrDecimal(parts[2].trim()) ?: return null
        if (amount.signum() == 0) return null
        return BankTransaction(date, amount, description, null, Bank.ITAU, sourceFile)
    }

    /** "1.700,00" -> 1700.00 ; "528,00" -> 528.00 ; "-1055,40" -> -1055.40 */
    private fun parseBrDecimal(raw: String): BigDecimal? {
        val cleaned = raw.replace(".", "").replace(",", ".").replace(" ", "")
        return cleaned.toBigDecimalOrNull()
    }

    /**
     * Valor BR da fatura, que pode vir negativo com espaço: "10,68",
     * "1.128,11", "- 5.925,06". O sinal é tratado à parte do separador de
     * milhar/decimal.
     */
    private fun parseFaturaAmount(raw: String): BigDecimal? {
        var s = raw.trim()
        val negative = s.startsWith("-")
        if (negative) s = s.removePrefix("-").trim()
        val value = parseBrDecimal(s) ?: return null
        return if (negative) value.negate() else value
    }

    /**
     * Split de uma linha CSV respeitando aspas duplas (RFC 4180): vírgulas
     * dentro de aspas não separam, e `""` vira uma aspa literal. Necessário
     * para faturas como `2026-05-07,"IOF de ""Loja""","39,48"`.
     */
    private fun splitCsv(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { fields.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        fields.add(sb.toString())
        return fields
    }
}
