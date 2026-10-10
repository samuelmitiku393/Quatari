package com.quatari.app.data.repository

import com.quatari.app.data.local.entity.AccountEntity
import com.quatari.app.data.local.entity.AccountType
import com.quatari.app.data.local.entity.JournalEntryEntity
import com.quatari.app.data.local.entity.ProcessingStatus
import com.quatari.app.data.local.entity.RawSmsEntity
import com.quatari.app.data.local.entity.TransactionEntity
import com.quatari.app.domain.model.AccountWithBalance
import com.quatari.app.domain.model.DashboardSummary
import com.quatari.app.domain.model.ParsedTransaction
import com.quatari.app.domain.model.TransactionWithEntries
import kotlinx.coroutines.flow.Flow

interface DoubleEntryLedgerRepository {

    // Raw SMS & Ingestion
    suspend fun saveRawSms(sender: String, body: String, receivedAt: Long): Long
    suspend fun updateRawSmsStatus(id: Long, status: ProcessingStatus, reason: String? = null)
    suspend fun processIncomingSms(sender: String, body: String, receivedAt: Long): ProcessingStatus

    // Accounts
    fun getAllAccounts(): Flow<List<AccountEntity>>
    suspend fun getAllAccountsSync(): List<AccountEntity>
    suspend fun getAccountById(id: String): AccountEntity?
    suspend fun createAccount(account: AccountEntity)
    fun getAccountsWithBalances(): Flow<List<AccountWithBalance>>

    // Transactions
    fun getUntriagedTransactions(): Flow<List<TransactionEntity>>
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getFilteredTransactions(
        bankFilter: String? = null,
        isTriaged: Boolean? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Flow<List<TransactionEntity>>
    suspend fun getTransactionById(id: String): TransactionEntity?
    suspend fun getTransactionWithEntries(id: String): TransactionWithEntries?
    suspend fun insertTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(id: String)

    // Double-entry balancing & Triage
    /**
     * Atomically creates balancing journal entries for a transaction and marks it triaged.
     * Throws [IllegalStateException] if debits and credits do not balance mathematically.
     */
    suspend fun recordJournalEntries(
        transactionId: String,
        entries: List<JournalEntryEntity>
    )

    /**
     * Helper for quick-triaging an unclassified transaction into standard ledger accounts.
     * Ensures perfect debit = credit balance automatically.
     */
    suspend fun triageTransaction(
        transactionId: String,
        targetAccountId: String,
        sourceAssetAccountId: String? = null
    )

    // Manual Entry
    suspend fun createManualTransaction(
        bankOrSource: String,
        reference: String,
        amountSantim: Long,
        feeSantim: Long = 0L,
        occurredAt: Long,
        notes: String?,
        debitAccountId: String,
        creditAccountId: String
    )

    // Financial Intelligence & Aggregation
    fun getDashboardSummary(): Flow<DashboardSummary>
}
