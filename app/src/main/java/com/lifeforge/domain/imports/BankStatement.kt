package com.lifeforge.domain.imports

import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.IncomeType
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Modelos do importador de extratos bancários.
 *
 * O parsing acontece no dispositivo (cada banco tem seu formato) e o
 * resultado é classificado em receita / despesa / movimento interno antes de
 * subir para o backend via importação em lote.
 */

/** Banco de origem do extrato — define qual parser usar. */
enum class Bank(val label: String) {
    NUBANK("Nubank"),
    ITAU("Itaú"),
}

/** Uma transação bruta extraída de uma linha do extrato (valor COM sinal). */
data class BankTransaction(
    val date: LocalDate,
    val amount: BigDecimal,   // + entrada, - saída
    val description: String,
    val externalId: String?,  // UUID do Nubank; null no Itaú
    val bank: Bank,
    val sourceFile: String,
)

/** Natureza da transação após classificação. */
enum class TxnKind { INCOME, EXPENSE, INTERNAL }

/** Por que uma transação foi marcada como movimento interno (não é renda/despesa). */
enum class InternalReason(val label: String) {
    INVESTMENT("Investimento (RDB/aplicação)"),
    CARD_BILL("Fatura de cartão"),
    SELF_TRANSFER("Transferência sua"),
    CROSS_ACCOUNT("Transferência entre suas contas"),
}

/**
 * Transação já classificada. `includedByDefault` é false para movimentos
 * internos — na pré-visualização eles vêm desmarcados, mas o usuário pode
 * sobrescrever.
 */
data class ClassifiedTransaction(
    val txn: BankTransaction,
    val kind: TxnKind,
    val internalReason: InternalReason? = null,
    val incomeType: IncomeType? = null,
    val category: ExpenseCategory? = null,
    val includedByDefault: Boolean,
)

/** Resultado da importação em lote (contagens devolvidas pelo backend). */
data class ImportResult(
    val incomesCreated: Int,
    val expensesCreated: Int,
    val skipped: Int,
) {
    val total: Int get() = incomesCreated + expensesCreated
}
