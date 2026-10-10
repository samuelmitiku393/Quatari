package com.quatari.app.ui.ledger

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quatari.app.data.export.DataExporter
import com.quatari.app.data.local.entity.AccountEntity
import com.quatari.app.data.local.entity.JournalEntryEntity
import com.quatari.app.data.local.entity.TransactionEntity
import com.quatari.app.data.repository.DoubleEntryLedgerRepository
import com.quatari.app.domain.model.TransactionWithEntries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class LedgerUiEvent {
    data class ShowMessage(val message: String) : LedgerUiEvent()
    data class ShowError(val error: String) : LedgerUiEvent()
}

class LedgerViewModel(
    private val repository: DoubleEntryLedgerRepository
) : ViewModel() {

    val selectedBankFilter = MutableStateFlow<String?>(null)
    val selectedTriageFilter = MutableStateFlow<Boolean?>(null)
    val searchQuery = MutableStateFlow("")

    val accounts: StateFlow<List<AccountEntity>> =
        repository.getAllAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionEntity>> =
        combine(
            selectedBankFilter,
            selectedTriageFilter
        ) { bank, triaged ->
            Pair(bank, triaged)
        }.flatMapLatest { (bank, triaged) ->
            repository.getFilteredTransactions(bankFilter = bank, isTriaged = triaged)
        }.combine(searchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter { tx ->
                    tx.referenceNumber.contains(query, ignoreCase = true) ||
                            tx.bankName.contains(query, ignoreCase = true) ||
                            (tx.notes ?: "").contains(query, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTransaction = MutableStateFlow<TransactionWithEntries?>(null)
    val selectedTransaction: StateFlow<TransactionWithEntries?> = _selectedTransaction.asStateFlow()

    private val _showManualDialog = MutableStateFlow(false)
    val showManualDialog: StateFlow<Boolean> = _showManualDialog.asStateFlow()

    private val _events = MutableSharedFlow<LedgerUiEvent>()
    val events: SharedFlow<LedgerUiEvent> = _events.asSharedFlow()

    fun openManualDialog() {
        _showManualDialog.value = true
    }

    fun dismissManualDialog() {
        _showManualDialog.value = false
    }

    fun selectTransaction(transactionId: String) {
        viewModelScope.launch {
            val txWithEntries = repository.getTransactionWithEntries(transactionId)
            _selectedTransaction.value = txWithEntries
        }
    }

    fun dismissDetailDialog() {
        _selectedTransaction.value = null
    }

    fun createManualTransaction(
        bankOrSource: String,
        reference: String,
        amountSantim: Long,
        occurredAt: Long,
        notes: String?,
        debitAccountId: String,
        creditAccountId: String
    ) {
        viewModelScope.launch {
            try {
                repository.createManualTransaction(
                    bankOrSource = bankOrSource,
                    reference = reference,
                    amountSantim = amountSantim,
                    feeSantim = 0L,
                    occurredAt = occurredAt,
                    notes = notes,
                    debitAccountId = debitAccountId,
                    creditAccountId = creditAccountId
                )
                _showManualDialog.value = false
                _events.emit(LedgerUiEvent.ShowMessage("Manual transaction recorded & balanced!"))
            } catch (e: Exception) {
                _events.emit(LedgerUiEvent.ShowError(e.message ?: "Failed to record transaction"))
            }
        }
    }

    fun exportCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val txs = transactions.value
                val allAccounts = accounts.value
                // Load all entries
                val entriesList = mutableListOf<JournalEntryEntity>()
                for (tx in txs) {
                    val res = repository.getTransactionWithEntries(tx.id)
                    res?.entries?.let { entriesList.addAll(it) }
                }

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    DataExporter.exportLedgerToCsv(stream, txs, entriesList, allAccounts)
                }
                _events.emit(LedgerUiEvent.ShowMessage("CSV Ledger successfully exported!"))
            } catch (e: Exception) {
                _events.emit(LedgerUiEvent.ShowError("Export error: ${e.message}"))
            }
        }
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    DataExporter.exportDatabaseBackup(context, stream)
                }
                _events.emit(LedgerUiEvent.ShowMessage("Database backup exported successfully!"))
            } catch (e: Exception) {
                _events.emit(LedgerUiEvent.ShowError("Backup error: ${e.message}"))
            }
        }
    }

    class Factory(private val repository: DoubleEntryLedgerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LedgerViewModel(repository) as T
        }
    }
}
