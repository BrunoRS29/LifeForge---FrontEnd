package com.lifeforge.domain.imports

import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.IncomeType
import java.text.Normalizer
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Classifica as transações de um extrato em RECEITA / DESPESA / INTERNA.
 *
 * Movimentos internos (não são renda nem despesa) são detectados e excluídos
 * por padrão:
 *  - investimentos (Resgate/Aplicação RDB, aplicação automática do Itaú);
 *  - pagamento de fatura de cartão;
 *  - transferências para você mesmo (nome do usuário na descrição);
 *  - transferências ENTRE SUAS CONTAS: pareamento de uma saída em um arquivo
 *    com uma entrada de mesmo valor (±3 dias) em outro — detectável quando
 *    vários extratos são carregados juntos.
 *
 * Despesas recebem categoria por heurística de palavra-chave; o resto cai em
 * OUTROS. Tudo é normalizado (minúsculas + sem acento) antes do match.
 */
object StatementClassifier {

    private const val CROSS_ACCOUNT_DAY_WINDOW = 3L

    fun classify(
        transactions: List<BankTransaction>,
        userName: String? = null,
    ): List<ClassifiedTransaction> {
        val nameTokens = userName
            ?.let { normalize(it) }
            ?.split(" ", ".", "-", "/")
            ?.map { it.trim() }
            ?.filter { it.length >= 3 }
            ?: emptyList()

        val initial = transactions.map { classifyOne(it, nameTokens) }
        return detectCrossAccountTransfers(initial)
    }

    // ------------------------------------------------------------------------
    // Passagem 1: classificação individual
    // ------------------------------------------------------------------------

    private fun classifyOne(
        txn: BankTransaction,
        nameTokens: List<String>,
    ): ClassifiedTransaction {
        val d = normalize(txn.description)

        if (containsAny(d, INVESTMENT_KEYWORDS)) {
            return internal(txn, InternalReason.INVESTMENT)
        }
        // NB: "Pagamento de fatura" NÃO é filtrado — como o extrato não traz as
        // compras unitárias do cartão, o pagamento da fatura representa o gasto
        // do mês e deve contar como despesa.
        if (nameTokens.isNotEmpty() && isTransferLike(d) && nameTokens.any { d.contains(it) }) {
            return internal(txn, InternalReason.SELF_TRANSFER)
        }

        return if (txn.amount.signum() > 0) {
            ClassifiedTransaction(
                txn = txn,
                kind = TxnKind.INCOME,
                incomeType = detectIncomeType(d),
                includedByDefault = true,
            )
        } else {
            ClassifiedTransaction(
                txn = txn,
                kind = TxnKind.EXPENSE,
                category = detectCategory(d),
                includedByDefault = true,
            )
        }
    }

    // ------------------------------------------------------------------------
    // Passagem 2: transferências entre contas (par saída/entrada)
    // ------------------------------------------------------------------------

    private fun detectCrossAccountTransfers(
        items: List<ClassifiedTransaction>,
    ): List<ClassifiedTransaction> {
        val result = items.toMutableList()
        val used = BooleanArray(result.size)

        for (i in result.indices) {
            val out = result[i]
            if (used[i] || out.kind == TxnKind.INTERNAL) continue
            if (out.txn.amount.signum() >= 0) continue            // procura SAÍDA
            if (!isTransferLike(normalize(out.txn.description))) continue

            for (j in result.indices) {
                if (j == i || used[j] || result[j].kind == TxnKind.INTERNAL) continue
                val inc = result[j]
                if (inc.txn.amount.signum() <= 0) continue        // ...e ENTRADA
                if (!isTransferLike(normalize(inc.txn.description))) continue

                val sameAmount = out.txn.amount.abs().compareTo(inc.txn.amount.abs()) == 0
                val closeDate = abs(
                    ChronoUnit.DAYS.between(out.txn.date, inc.txn.date)
                ) <= CROSS_ACCOUNT_DAY_WINDOW

                if (sameAmount && closeDate) {
                    result[i] = internal(out.txn, InternalReason.CROSS_ACCOUNT)
                    result[j] = internal(inc.txn, InternalReason.CROSS_ACCOUNT)
                    used[i] = true
                    used[j] = true
                    break
                }
            }
        }
        return result
    }

    // ------------------------------------------------------------------------
    // Heurísticas
    // ------------------------------------------------------------------------

    private fun detectIncomeType(d: String): IncomeType = when {
        d.contains("salario") || d.contains("remunera") ||
            d.contains("pro labore") || d.contains("prolabore") -> IncomeType.SALARY
        d.contains("dividend") -> IncomeType.DIVIDEND
        d.contains("aluguel") -> IncomeType.RENT
        d.contains("bonus") -> IncomeType.BONUS
        else -> IncomeType.OTHER
    }

    private fun detectCategory(d: String): ExpenseCategory = when {
        containsAny(d, FOOD) -> ExpenseCategory.FOOD
        containsAny(d, HOUSING) -> ExpenseCategory.HOUSING
        containsAny(d, TRANSPORT) -> ExpenseCategory.TRANSPORT
        containsAny(d, HEALTH) -> ExpenseCategory.HEALTH
        containsAny(d, EDUCATION) -> ExpenseCategory.EDUCATION
        containsAny(d, LEISURE) -> ExpenseCategory.LEISURE
        else -> ExpenseCategory.OTHER
    }

    private fun internal(txn: BankTransaction, reason: InternalReason) =
        ClassifiedTransaction(
            txn = txn,
            kind = TxnKind.INTERNAL,
            internalReason = reason,
            includedByDefault = false,
        )

    private fun isTransferLike(d: String) = containsAny(d, TRANSFER_KEYWORDS)
    private fun containsAny(d: String, keys: List<String>) = keys.any { d.contains(it) }

    /** minúsculas + remoção de acentos para casar palavras-chave sem acento. */
    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

    // Palavras-chave (sempre sem acento, pois a descrição é normalizada).
    private val INVESTMENT_KEYWORDS =
        listOf("rdb", "cdb", "tesouro direto", "int itau", "aplicacao", "resgate")
    private val TRANSFER_KEYWORDS = listOf("pix", " ted", " doc", "transfer")
    private val FOOD =
        listOf("mercado", "supermerc", "atacad", "hortifruti", "padaria", "acougue", "ifood", "milk", "restaurante", "lanchonete")
    private val HOUSING =
        listOf("cpfl", "semae", "sabesp", "enel", "energia", "agua", "vivo", "claro", "telefonica", "telesp", "internet", "aluguel", "condominio", "forca luz")
    private val TRANSPORT =
        listOf("uber", "99app", "posto", "combustivel", "ipva", "estacionamento", "pedagio", "shell", "ipiranga")
    private val HEALTH =
        listOf("farmacia", "drogaria", "drogasil", "unimed", "hospital", "clinica", "saude")
    private val EDUCATION =
        listOf("escola", "faculdade", "curso", "ensino", "universidade", "udemy", "alura", "colegio")
    private val LEISURE =
        listOf("netflix", "spotify", "cinema", "disney", "steam", "hbo", "prime video")
}
