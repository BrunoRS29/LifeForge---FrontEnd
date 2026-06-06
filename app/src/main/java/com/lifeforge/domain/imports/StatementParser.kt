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
}
