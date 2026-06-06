package com.lifeforge.domain.imports

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.IncomeType
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Testes do parsing e da classificação dos extratos (Nubank/Itaú).
 * Usa os mesmos padrões dos arquivos reais fornecidos pelo usuário.
 */
class StatementImportTest {

    // ------------------------------------------------------------------------
    // Parsers
    // ------------------------------------------------------------------------

    @Test
    fun `nubank - parseia linhas e ignora o cabecalho`() {
        val csv = """
            Data,Valor,Identificador,Descrição
            10/01/2026,100.00,abc-123,Transferência Recebida - Fabiana
            12/01/2026,-4707.54,def-456,Pagamento de fatura
        """.trimIndent()

        val txns = StatementParser.parseNubank(csv, "jan.csv")

        assertThat(txns).hasSize(2)
        assertThat(txns[0].date).isEqualTo(LocalDate.of(2026, 1, 10))
        assertThat(txns[0].amount).isEqualTo(BigDecimal("100.00"))
        assertThat(txns[0].externalId).isEqualTo("abc-123")
        assertThat(txns[1].amount.signum()).isEqualTo(-1)
        assertThat(txns[1].bank).isEqualTo(Bank.NUBANK)
    }

    @Test
    fun `nubank - descricao com virgula nao quebra o parsing`() {
        val csv = "10/01/2026,100.00,id-1,Compra, com virgula, no meio"
        val txns = StatementParser.parseNubank(csv, "x.csv")
        assertThat(txns).hasSize(1)
        assertThat(txns[0].description).isEqualTo("Compra, com virgula, no meio")
    }

    @Test
    fun `itau - entende formato BR (virgula decimal) e sinal`() {
        val txt = """
            24/02/2023;REMUNERACAO/SALARIO;1700,00
            10/02/2023;PIX TRANSF  GABRIEL10/02;-528,00
            01/01/2023;COMPRA;1.234,56
        """.trimIndent()

        val txns = StatementParser.parseItau(txt, "fev.txt")

        assertThat(txns).hasSize(3)
        assertThat(txns[0].amount).isEqualTo(BigDecimal("1700.00"))
        assertThat(txns[1].amount).isEqualTo(BigDecimal("-528.00"))
        assertThat(txns[2].amount).isEqualTo(BigDecimal("1234.56"))
        assertThat(txns[0].bank).isEqualTo(Bank.ITAU)
        assertThat(txns[0].externalId).isNull()
    }

    // ------------------------------------------------------------------------
    // Classificador
    // ------------------------------------------------------------------------

    private fun txn(
        amount: String,
        desc: String,
        bank: Bank = Bank.NUBANK,
        date: LocalDate = LocalDate.of(2026, 1, 10),
    ) = BankTransaction(date, BigDecimal(amount), desc, null, bank, "f")

    @Test
    fun `RDB vira interno desmarcado e fatura vira despesa`() {
        val res = StatementClassifier.classify(
            listOf(
                txn("100.00", "Resgate RDB"),
                txn("-1030.00", "Aplicação RDB"),
                txn("-4707.54", "Pagamento de fatura"),
            )
        )
        assertThat(res[0].kind).isEqualTo(TxnKind.INTERNAL)   // Resgate RDB
        assertThat(res[1].kind).isEqualTo(TxnKind.INTERNAL)   // Aplicação RDB
        // Fatura de cartão conta como despesa (extrato não tem compras unitárias).
        assertThat(res[2].kind).isEqualTo(TxnKind.EXPENSE)
        assertThat(res[2].includedByDefault).isTrue()
    }

    @Test
    fun `despesas recebem categoria por heuristica`() {
        val res = StatementClassifier.classify(
            listOf(
                txn("-131.50", "Compra no débito - DELTA SUPERMERCADOS"),
                txn("-63.98", "Pagamento de boleto efetuado - SEMAE PIRACICABA"),
                txn("-1944.00", "Pagamento de boleto efetuado - DAS-SIMPLES NACIONAL"),
            )
        )
        assertThat(res[0].category).isEqualTo(ExpenseCategory.FOOD)      // supermercado
        assertThat(res[1].category).isEqualTo(ExpenseCategory.HOUSING)   // semae (água)
        assertThat(res[2].category).isEqualTo(ExpenseCategory.OTHER)     // imposto
        assertThat(res.all { it.kind == TxnKind.EXPENSE }).isTrue()
    }

    @Test
    fun `salario vira renda do tipo SALARY`() {
        val res = StatementClassifier.classify(
            listOf(txn("1700.00", "REMUNERACAO/SALARIO", Bank.ITAU)),
        )
        assertThat(res[0].kind).isEqualTo(TxnKind.INCOME)
        assertThat(res[0].incomeType).isEqualTo(IncomeType.SALARY)
    }

    @Test
    fun `transferencia para si mesmo (nome na descricao) vira interna`() {
        val res = StatementClassifier.classify(
            listOf(txn("-528.00", "PIX TRANSF  GABRIEL10/02", Bank.ITAU)),
            userName = "Gabriel Innocencio",
        )
        assertThat(res[0].kind).isEqualTo(TxnKind.INTERNAL)
        assertThat(res[0].internalReason).isEqualTo(InternalReason.SELF_TRANSFER)
    }

    @Test
    fun `transferencia entre contas (saida e entrada de mesmo valor) e pareada`() {
        val res = StatementClassifier.classify(
            listOf(
                txn("-700.00", "Transferência enviada pelo Pix", Bank.NUBANK, LocalDate.of(2026, 1, 10)),
                txn("700.00", "Transferência Recebida pelo Pix", Bank.ITAU, LocalDate.of(2026, 1, 11)),
            )
        )
        assertThat(res.all { it.kind == TxnKind.INTERNAL }).isTrue()
        assertThat(res.all { it.internalReason == InternalReason.CROSS_ACCOUNT }).isTrue()
    }

    @Test
    fun `renda e despesa de mesmo valor que NAO sao transferencia nao se pareiam`() {
        val res = StatementClassifier.classify(
            listOf(
                txn("1700.00", "REMUNERACAO/SALARIO", Bank.ITAU, LocalDate.of(2023, 1, 24)),
                txn("-1700.00", "Pagamento de boleto efetuado - DAS", Bank.ITAU, LocalDate.of(2023, 1, 26)),
            )
        )
        assertThat(res[0].kind).isEqualTo(TxnKind.INCOME)
        assertThat(res[1].kind).isEqualTo(TxnKind.EXPENSE)
    }
}
