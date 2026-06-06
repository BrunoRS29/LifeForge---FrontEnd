package com.lifeforge.data.repository

import com.lifeforge.data.api.FinanceImportApi
import com.lifeforge.data.model.dto.ExpenseRequestDto
import com.lifeforge.data.model.dto.ImportRequestDto
import com.lifeforge.data.model.dto.IncomeRequestDto
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.imports.ClassifiedTransaction
import com.lifeforge.domain.imports.ImportResult
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.IncomeType
import com.lifeforge.domain.model.mapCatching
import com.lifeforge.domain.repository.StatementImportRepository
import kotlinx.serialization.json.Json
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapeia as transações incluídas para receitas/despesas (pelo sinal) e envia
 * em lote. A data (LocalDate) vira Instant ao meio-dia no fuso de São Paulo,
 * mesma convenção das telas de Finanças — evita que o dia "escorregue" no UTC.
 */
@Singleton
class StatementImportRepositoryImpl @Inject constructor(
    private val api: FinanceImportApi,
    private val json: Json,
) : StatementImportRepository {

    override suspend fun import(
        included: List<ClassifiedTransaction>,
    ): DataResult<ImportResult> {
        val incomes = mutableListOf<IncomeRequestDto>()
        val expenses = mutableListOf<ExpenseRequestDto>()

        for (item in included) {
            val t = item.txn
            val iso = t.date.atTime(12, 0).atZone(SP_ZONE).toInstant().toString()
            val amount = t.amount.abs().toPlainString()
            val label = t.description.trim().ifBlank { t.bank.label }.take(MAX_LABEL)

            if (t.amount.signum() >= 0) {
                incomes += IncomeRequestDto(
                    source = label,
                    amount = amount,
                    incomeType = (item.incomeType ?: IncomeType.OTHER).name,
                    recurring = false,
                    receivedAt = iso,
                )
            } else {
                expenses += ExpenseRequestDto(
                    description = label,
                    amount = amount,
                    category = (item.category ?: ExpenseCategory.OTHER).name,
                    recurring = false,
                    spentAt = iso,
                )
            }
        }

        return safeApiCall(json) {
            api.importTransactions(ImportRequestDto(incomes = incomes, expenses = expenses))
        }.mapCatching { ImportResult(it.incomesCreated, it.expensesCreated, it.skipped) }
    }

    private companion object {
        val SP_ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")
        const val MAX_LABEL = 200
    }
}
