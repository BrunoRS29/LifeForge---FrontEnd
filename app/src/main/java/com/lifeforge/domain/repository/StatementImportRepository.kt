package com.lifeforge.domain.repository

import com.lifeforge.domain.imports.ClassifiedTransaction
import com.lifeforge.domain.imports.ImportResult
import com.lifeforge.domain.model.DataResult

/**
 * Envia as transações selecionadas (após preview) para o backend em lote.
 * Network-only, como [PredictionRepository] — não há cache local de import.
 */
interface StatementImportRepository {

    /** Importa os lançamentos incluídos (mapeados para receita/despesa por sinal). */
    suspend fun import(included: List<ClassifiedTransaction>): DataResult<ImportResult>
}
