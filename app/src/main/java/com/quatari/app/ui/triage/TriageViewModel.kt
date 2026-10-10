package com.quatari.app.ui.triage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quatari.app.data.local.entity.AccountEntity
import com.quatari.app.data.local.entity.TransactionEntity
import com.quatari.app.data.repository.DoubleEntryLedgerRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TriageUiEvent {
    data class ShowMessage(val message: String) : TriageUiEvent()
    data class ShowError(val error: String) : TriageUiEvent()
}

class TriageViewModel(
    private val repository: DoubleEntryLedgerRepository
) : ViewModel() {

    val untriagedTransactions: StateFlow<List<TransactionEntity>> =
        repository.getUntriagedTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> =
        repository.getAllAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<TriageUiEvent>()
    val events: SharedFlow<TriageUiEvent> = _events.asSharedFlow()

    private val _selectedTransactionForModal = MutableStateFlow<TransactionEntity?>(null)
    val selectedTransactionForModal: StateFlow<TransactionEntity?> = _selectedTransactionForModal.asStateFlow()

    fun openCategorizeModal(transaction: TransactionEntity) {
        _selectedTransactionForModal.value = transaction
    }

    fun dismissCategorizeModal() {
        _selectedTransactionForModal.value = null
    }

    fun quickTag(
        transactionId: String,
        targetAccountId: String,
        sourceAssetAccountId: String? = null
    ) {
        viewModelScope.launch {
            try {
                repository.triageTransaction(
                    transactionId = transactionId,
                    targetAccountId = targetAccountId,
                    sourceAssetAccountId = sourceAssetAccountId
                )
                _events.emit(TriageUiEvent.ShowMessage("Transaction categorized into balanced ledger!"))
            } catch (e: Exception) {
                _events.emit(TriageUiEvent.ShowError(e.message ?: "Failed to categorize transaction"))
            }
        }
    }

    fun swipeRightQuickTag(transactionId: String) {
        quickTag(transactionId, "revenue:ad:channel_a")
    }

    fun swipeLeftQuickTag(transactionId: String) {
        quickTag(transactionId, "expense:personal")
    }

    fun injectTestVectors() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()

                val telebirrSample = """
                    Dear Samuel 
                    You have transferred ETB 260.00 to SELAM AWULACHEW (2519****9626) on 09/10/2026 08:07:05. Your transaction number is DJ90L0YWR6. The service fee is  ETB 1.74 and  15% VAT on the service fee is ETB 0.26. Your current E-Money Account  balance is ETB 6,338.90. To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DJ90L0YWR6.
                """.trimIndent()

                val boaSample = """
                    Dear Samuel, your account 2*93 was debited with ETB 300.00. Available Balance: ETB 113,349.37.
                    Receipt: https://cs.bankofabyssinia.com/slip/?trx=FT26272208DB83593
                    Feedback: https://cs.bankofabyssinia.com/cs/?trx=DFT26272208DB
                """.trimIndent()

                val cbeSample = """
                    Dear  Samuel Mitiku Eshetu You have successfully transferred ETB400.00 from account 1**4449 to account 1**7318 (Sisay Fiseha Haileselassie). Service charge of ETB 0.50 and VAT(15%) of ETB0.08 and Disaster Recovery(5%) of 0.03 with total of ETB400.61 .Your current balance is ETB49,700.84. Thanks for Banking with CBE. https://mbreciept.cbe.com.et/v2-hfHCxHyR90zec7I8nEPo
                """.trimIndent()

                repository.processIncomingSms("telebirr", telebirrSample, now - 3600000)
                repository.processIncomingSms("Abyssinia", boaSample, now - 7200000)
                repository.processIncomingSms("CBE", cbeSample, now - 10800000)

                _events.emit(TriageUiEvent.ShowMessage("Injected 3 realistic bank test SMS messages!"))
            } catch (e: Exception) {
                _events.emit(TriageUiEvent.ShowError(e.message ?: "Failed to inject test vectors"))
            }
        }
    }

    class Factory(private val repository: DoubleEntryLedgerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TriageViewModel(repository) as T
        }
    }
}
