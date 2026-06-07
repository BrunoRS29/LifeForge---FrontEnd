package com.lifeforge.presentation.screen.imports

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.imports.Bank
import com.lifeforge.domain.imports.BankTransaction
import com.lifeforge.domain.imports.ClassifiedTransaction
import com.lifeforge.domain.imports.ImportResult
import com.lifeforge.domain.imports.StatementClassifier
import com.lifeforge.domain.imports.StatementKind
import com.lifeforge.domain.imports.StatementParser
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.repository.ExpenseRepository
import com.lifeforge.domain.repository.IncomeRepository
import com.lifeforge.domain.usecase.ImportTransactionsUseCase
import com.lifeforge.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel do importador de extratos.
 *
 * Mantém uma SESSÃO de arquivos (possivelmente de bancos diferentes) para que
 * a detecção de transferências entre contas funcione sobre o conjunto inteiro.
 * O parsing/classificação é local; só o lote final (itens incluídos) sobe via
 * [ImportTransactionsUseCase]. Após importar, refresca os caches de
 * receita/despesa para os lançamentos aparecerem nas abas de Finanças.
 */
@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importTransactions: ImportTransactionsUseCase,
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    /** Transações brutas de extratos de conta, acumuladas na sessão. */
    private var rawTransactions: List<BankTransaction> = emptyList()

    /** Itens brutos de faturas de cartão (somente Nubank). */
    private var rawFaturas: List<BankTransaction> = emptyList()

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun onBankSelected(bank: Bank) = _state.update { it.copy(selectedBank = bank) }

    fun onUserNameChange(name: String) {
        _state.update { it.copy(userName = name) }
        reclassify()
    }

    /**
     * Liga/desliga a importação de faturas. Quando ligado, o "Pagamento de
     * fatura" do extrato é desativado (vira interno) e as despesas vêm dos
     * itens da fatura — evita contar o gasto do cartão duas vezes.
     */
    fun onImportInvoicesChange(enabled: Boolean) {
        _state.update { it.copy(importInvoices = enabled) }
        reclassify()
    }

    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            val bank = _state.value.selectedBank
            val newSources = mutableListOf<ImportedSource>()
            val newRaw = mutableListOf<BankTransaction>()

            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    val name = displayName(uri)
                    val content = runCatching { readText(uri) }.getOrNull()
                    val parsed = if (content.isNullOrBlank()) emptyList()
                    else StatementParser.parse(bank, content, name)
                    newRaw += parsed
                    newSources += ImportedSource(bank, name, parsed.size, StatementKind.ACCOUNT)
                }
            }

            rawTransactions = rawTransactions + newRaw
            _state.update { it.copy(sources = it.sources + newSources) }
            if (newRaw.isEmpty()) {
                _state.update { it.copy(error = "Nenhuma transação reconhecida — confira se o banco selecionado é o do arquivo.") }
            }
            reclassify()
        }
    }

    /** Adiciona arquivos de FATURA do Nubank (CSV `date,title,amount`). */
    fun addFaturaFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            val newSources = mutableListOf<ImportedSource>()
            val newRaw = mutableListOf<BankTransaction>()

            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    val name = displayName(uri)
                    val content = runCatching { readText(uri) }.getOrNull()
                    val parsed = if (content.isNullOrBlank()) emptyList()
                    else StatementParser.parseNubankFatura(content, name)
                    newRaw += parsed
                    newSources += ImportedSource(Bank.NUBANK, name, parsed.size, StatementKind.CARD_INVOICE)
                }
            }

            rawFaturas = rawFaturas + newRaw
            _state.update { it.copy(sources = it.sources + newSources) }
            if (newRaw.isEmpty()) {
                _state.update { it.copy(error = "Nenhum item de fatura reconhecido — confira se é o CSV de fatura do Nubank.") }
            }
            reclassify()
        }
    }

    fun toggleInclude(index: Int) {
        _state.update {
            val set = it.includedIndices.toMutableSet()
            if (!set.add(index)) set.remove(index)
            it.copy(includedIndices = set)
        }
    }

    fun clearAll() {
        rawTransactions = emptyList()
        rawFaturas = emptyList()
        _state.update {
            ImportUiState(
                selectedBank = it.selectedBank,
                userName = it.userName,
                importInvoices = it.importInvoices,
            )
        }
    }

    fun import() {
        val current = _state.value
        if (current.isImporting) return
        val included = current.includedIndices.sorted()
            .mapNotNull { current.classified.getOrNull(it) }

        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, error = null) }
            when (val r = importTransactions(included)) {
                is DataResult.Success -> {
                    // Best-effort: traz os novos lançamentos para o cache local.
                    incomeRepository.refresh()
                    expenseRepository.refresh()
                    _state.update { it.copy(isImporting = false, result = r.data) }
                }
                is DataResult.Failure -> _state.update {
                    it.copy(isImporting = false, error = r.error.toUserMessage())
                }
            }
        }
    }

    fun onErrorDismiss() = _state.update { it.copy(error = null) }
    fun onResultDismiss() = _state.update { it.copy(result = null) }

    private fun reclassify() {
        val s = _state.value
        val name = s.userName.trim().ifBlank { null }
        val statementClassified = StatementClassifier.classify(
            rawTransactions,
            userName = name,
            treatCardBillAsInternal = s.importInvoices,
        )
        val faturaClassified = StatementClassifier.classifyFatura(rawFaturas)
        val classified = statementClassified + faturaClassified
        val defaultIncluded = classified.indices
            .filter { classified[it].includedByDefault }
            .toSet()
        _state.update { it.copy(classified = classified, includedIndices = defaultIncluded) }
    }

    private fun readText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: ""

    private fun displayName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "extrato"
        runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx) ?: name
                }
            }
        }
        return name
    }
}

/** Um arquivo já carregado na sessão. */
data class ImportedSource(
    val bank: Bank,
    val fileName: String,
    val count: Int,
    val kind: StatementKind = StatementKind.ACCOUNT,
)

data class ImportUiState(
    val selectedBank: Bank = Bank.NUBANK,
    val userName: String = "",
    val importInvoices: Boolean = false,
    val sources: List<ImportedSource> = emptyList(),
    val classified: List<ClassifiedTransaction> = emptyList(),
    val includedIndices: Set<Int> = emptySet(),
    val isImporting: Boolean = false,
    val error: String? = null,
    val result: ImportResult? = null,
) {
    val includedCount: Int get() = includedIndices.size
    val ignoredCount: Int get() = classified.size - includedIndices.size
    val hasData: Boolean get() = classified.isNotEmpty()
}
