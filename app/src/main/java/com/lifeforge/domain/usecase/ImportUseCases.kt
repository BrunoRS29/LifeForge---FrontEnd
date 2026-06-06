package com.lifeforge.domain.usecase

import com.lifeforge.domain.imports.ClassifiedTransaction
import com.lifeforge.domain.imports.ImportResult
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.repository.StatementImportRepository
import javax.inject.Inject

/**
 * Importa as transações selecionadas na pré-visualização. O parsing e a
 * classificação são feitos no ViewModel (precisam do ContentResolver para ler
 * os arquivos); aqui fica só a regra de negócio + a chamada de rede.
 */
class ImportTransactionsUseCase @Inject constructor(
    private val repository: StatementImportRepository,
) {
    suspend operator fun invoke(
        included: List<ClassifiedTransaction>,
    ): DataResult<ImportResult> {
        if (included.isEmpty()) {
            return DataResult.Failure(
                AppError.Validation("transactions", "Nenhum lançamento selecionado para importar")
            )
        }
        return repository.import(included)
    }
}
