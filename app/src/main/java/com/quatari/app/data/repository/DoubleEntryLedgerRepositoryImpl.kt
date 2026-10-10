package com.quatari.app.data.repository

import androidx.room.withTransaction
import com.quatari.app.data.local.HisabeDatabase
import com.quatari.app.data.local.entity.AccountEntity
import com.quatari.app.data.local.entity.AccountType
import com.quatari.app.data.local.entity.JournalEntryEntity
import com.quatari.app.data.local.entity.ProcessingStatus
import com.quatari.app.data.local.entity.RawSmsEntity
import com.quatari.app.data.local.entity.TransactionEntity
import com.quatari.app.domain.model.AccountWithBalance
import com.quatari.app.domain.model.CashFlowPoint
import com.quatari.app.domain.model.ChannelRevenueSummary
import com.quatari.app.domain.model.DashboardSummary
import com.quatari.app.domain.model.TransactionWithEntries
import com.quatari.app.domain.parser.TransactionParserEngine
import com.quatari.app.domain.util.HashUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DoubleEntryLedgerRepositoryImpl(
    private val database: HisabeDatabase,
    private val parserEngine: TransactionParserEngine = TransactionParserEngine()
) : DoubleEntryLedgerRepository {

    private val rawSmsDao = database.rawSmsDao()
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val journalEntryDao = database.journalEntryDao()

    override suspend fun saveRawSms(sender: String, body: String, receivedAt: Long): Long {
        val entity = RawSmsEntity(
            sender = sender,
            body = body,
            receivedAt = receivedAt,
            processingStatus = ProcessingStatus.PENDING
        )
        return rawSmsDao.insert(entity)
    }

    override suspend fun updateRawSmsStatus(id: Long, status: ProcessingStatus, reason: String?) {
        rawSmsDao.updateStatus(id, status, reason)
    }

    override suspend fun processIncomingSms(
        sender: String,
        body: String,
        receivedAt: Long
    ): ProcessingStatus {
        val rawSmsId = saveRawSms(sender, body, receivedAt)

        val parsed = parserEngine.parse(sender, body, receivedAt)
        if (parsed == null) {
            val isFinancial = parserEngine.isFinancialMessage(sender, body)
            val status = if (isFinancial) ProcessingStatus.FAILED else ProcessingStatus.FAILED
            val reason = if (isFinancial) "Deterministic parsing failed for bank SMS format" else "Non-banking sender or unsupported SMS"
            updateRawSmsStatus(rawSmsId, status, reason)
            return status
        }

        val txId = HashUtils.computeTransactionId(parsed.bankName, parsed.referenceNumber)

        return database.withTransaction {
            if (transactionDao.exists(txId)) {
                updateRawSmsStatus(rawSmsId, ProcessingStatus.DUPLICATE, "Duplicate reference: ${parsed.referenceNumber}")
                ProcessingStatus.DUPLICATE
            } else {
                val transaction = TransactionEntity(
                    id = txId,
                    bankName = parsed.bankName,
                    referenceNumber = parsed.referenceNumber,
                    rawAmount = parsed.rawAmount,
                    feeAmount = parsed.feeAmount,
                    reportedBalance = parsed.reportedBalance,
                    occurredAt = parsed.occurredAt,
                    rawSmsId = rawSmsId,
                    isTriaged = false,
                    notes = parsed.notes
                )
                transactionDao.insert(transaction)
                updateRawSmsStatus(rawSmsId, ProcessingStatus.PARSED)
                ProcessingStatus.PARSED
            }
        }
    }

    override fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllFlow()

    override suspend fun getAllAccountsSync(): List<AccountEntity> = accountDao.getAllSync()

    override suspend fun getAccountById(id: String): AccountEntity? = accountDao.getById(id)

    override suspend fun createAccount(account: AccountEntity) {
        accountDao.insert(account)
    }

    override fun getAccountsWithBalances(): Flow<List<AccountWithBalance>> {
        return combine(
            accountDao.getAllFlow(),
            journalEntryDao.getAccountBalanceAggregatesFlow()
        ) { accounts, aggregates ->
            val aggregateMap = aggregates.associateBy { it.accountId }
            accounts.map { account ->
                val agg = aggregateMap[account.id]
                val debit = agg?.totalDebit ?: 0L
                val credit = agg?.totalCredit ?: 0L
                val balance = when (account.type) {
                    AccountType.ASSET, AccountType.EXPENSE -> debit - credit
                    AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> credit - debit
                }
                AccountWithBalance(account, balance)
            }
        }
    }

    override fun getUntriagedTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getUntriagedFlow()

    override fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllFlow()

    override fun getFilteredTransactions(
        bankFilter: String?,
        isTriaged: Boolean?,
        startTime: Long?,
        endTime: Long?
    ): Flow<List<TransactionEntity>> {
        return transactionDao.getFilteredFlow(bankFilter, isTriaged, startTime, endTime)
    }

    override suspend fun getTransactionById(id: String): TransactionEntity? =
        transactionDao.getById(id)

    override suspend fun getTransactionWithEntries(id: String): TransactionWithEntries? {
        val tx = transactionDao.getById(id) ?: return null
        val entries = journalEntryDao.getByTransactionId(id)
        return TransactionWithEntries(tx, entries)
    }

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insert(transaction)
    }

    override suspend fun deleteTransaction(id: String) {
        database.withTransaction {
            journalEntryDao.deleteByTransactionId(id)
            transactionDao.delete(id)
        }
    }

    override suspend fun recordJournalEntries(
        transactionId: String,
        entries: List<JournalEntryEntity>
    ) {
        require(entries.isNotEmpty()) { "Cannot record empty journal entries" }

        val totalDebits = entries.sumOf { it.debitSantim }
        val totalCredits = entries.sumOf { it.creditSantim }

        if (totalDebits != totalCredits) {
            throw IllegalStateException(
                "Double-entry ledger invariant violated: Debits ($totalDebits Santim) != Credits ($totalCredits Santim)"
            )
        }

        database.withTransaction {
            journalEntryDao.deleteByTransactionId(transactionId)
            journalEntryDao.insertAll(entries)
            transactionDao.setTriagedStatus(transactionId, true)
        }
    }

    override suspend fun triageTransaction(
        transactionId: String,
        targetAccountId: String,
        sourceAssetAccountId: String?
    ) {
        val tx = transactionDao.getById(transactionId)
            ?: throw IllegalArgumentException("Transaction $transactionId not found")

        val targetAccount = accountDao.getById(targetAccountId)
            ?: throw IllegalArgumentException("Account $targetAccountId not found")

        val assetAccountId = sourceAssetAccountId ?: resolveDefaultAssetAccount(tx.bankName)

        val entries = mutableListOf<JournalEntryEntity>()

        when (targetAccount.type) {
            AccountType.REVENUE -> {
                // Incoming revenue: Debit Asset, Credit Revenue
                entries.add(
                    JournalEntryEntity(
                        transactionId = transactionId,
                        accountId = assetAccountId,
                        debitSantim = tx.rawAmount,
                        creditSantim = 0L
                    )
                )
                entries.add(
                    JournalEntryEntity(
                        transactionId = transactionId,
                        accountId = targetAccountId,
                        debitSantim = 0L,
                        creditSantim = tx.rawAmount
                    )
                )
            }
            AccountType.EXPENSE -> {
                // Outgoing expense: Debit Expense, Debit BankFee (if any), Credit Asset (rawAmount + fee)
                val totalOutflow = tx.rawAmount + tx.feeAmount
                entries.add(
                    JournalEntryEntity(
                        transactionId = transactionId,
                        accountId = targetAccountId,
                        debitSantim = tx.rawAmount,
                        creditSantim = 0L
                    )
                )
                if (tx.feeAmount > 0L) {
                    entries.add(
                        JournalEntryEntity(
                            transactionId = transactionId,
                            accountId = "expense:banking:fees",
                            debitSantim = tx.feeAmount,
                            creditSantim = 0L
                        )
                    )
                }
                entries.add(
                    JournalEntryEntity(
                        transactionId = transactionId,
                        accountId = assetAccountId,
                        debitSantim = 0L,
                        creditSantim = totalOutflow
                    )
                )
            }
            AccountType.ASSET -> {
                // Transfer between asset accounts
                val totalOutflow = tx.rawAmount + tx.feeAmount
                entries.add(
                    JournalEntryEntity(
                        transactionId = transactionId,
                        accountId = targetAccountId,
                        debitSantim = tx.rawAmount,
                        creditSantim = 0L
                    )
                )
                if (tx.feeAmount > 0L) {
                    entries.add(
                        JournalEntryEntity(
                            transactionId = transactionId,
                            accountId = "expense:banking:fees",
                            debitSantim = tx.feeAmount,
                            creditSantim = 0L
                        )
                    )
                }
                entries.add(
                    JournalEntryEntity(
                        transactionId = transactionId,
                        accountId = assetAccountId,
                        debitSantim = 0L,
                        creditSantim = totalOutflow
                    )
                )
            }
            AccountType.LIABILITY -> {
                // Paying down liability
                val totalOutflow = tx.rawAmount + tx.feeAmount
                entries.add(
                    JournalEntryEntity(
                        transactionId = transactionId,
                        accountId = targetAccountId,
                        debitSantim = tx.rawAmount,
                        creditSantim = 0L
                    )
                )
                if (tx.feeAmount > 0L) {
                    entries.add(
                        JournalEntryEntity(
                            transactionId = transactionId,
                            accountId = "expense:banking:fees",
                            debitSantim = tx.feeAmount,
                            creditSantim = 0L
                        )
                    )
                }
                entries.add(
                    JournalEntryEntity(
                        transactionId = transactionId,
                        accountId = assetAccountId,
                        debitSantim = 0L,
                        creditSantim = totalOutflow
                    )
                )
            }
            AccountType.EQUITY -> {
                // Owner distribution or equity adjustment
                entries.add(
                    JournalEntryEntity(
                        transactionId = transactionId,
                        accountId = targetAccountId,
                        debitSantim = tx.rawAmount,
                        creditSantim = 0L
                    )
                )
                entries.add(
                    JournalEntryEntity(
                        transactionId = transactionId,
                        accountId = assetAccountId,
                        debitSantim = 0L,
                        creditSantim = tx.rawAmount
                    )
                )
            }
        }

        recordJournalEntries(transactionId, entries)
    }

    override suspend fun createManualTransaction(
        bankOrSource: String,
        reference: String,
        amountSantim: Long,
        feeSantim: Long,
        occurredAt: Long,
        notes: String?,
        debitAccountId: String,
        creditAccountId: String
    ) {
        val txId = HashUtils.computeTransactionId(bankOrSource, reference)
        val transaction = TransactionEntity(
            id = txId,
            bankName = bankOrSource,
            referenceNumber = reference,
            rawAmount = amountSantim,
            feeAmount = feeSantim,
            reportedBalance = null,
            occurredAt = occurredAt,
            rawSmsId = null,
            isTriaged = true,
            notes = notes
        )

        val entries = mutableListOf<JournalEntryEntity>()
        entries.add(
            JournalEntryEntity(
                transactionId = txId,
                accountId = debitAccountId,
                debitSantim = amountSantim,
                creditSantim = 0L
            )
        )
        entries.add(
            JournalEntryEntity(
                transactionId = txId,
                accountId = creditAccountId,
                debitSantim = 0L,
                creditSantim = amountSantim
            )
        )

        database.withTransaction {
            transactionDao.insert(transaction)
            recordJournalEntries(txId, entries)
        }
    }

    override fun getDashboardSummary(): Flow<DashboardSummary> {
        return combine(
            accountDao.getAllFlow(),
            journalEntryDao.getAccountBalanceAggregatesFlow(),
            transactionDao.getAllFlow(),
            transactionDao.getUntriagedFlow()
        ) { accounts, aggregates, transactions, untriaged ->
            val accountMap = accounts.associateBy { it.id }
            val aggregateMap = aggregates.associateBy { it.accountId }

            var liquidNetWorth = 0L
            var cbeBalance = 0L
            var telebirrBalance = 0L
            var abyssiniaBalance = 0L
            var totalRevenue = 0L
            var totalExpense = 0L

            val channelRevenuesList = mutableListOf<ChannelRevenueSummary>()

            for (account in accounts) {
                val agg = aggregateMap[account.id]
                val debit = agg?.totalDebit ?: 0L
                val credit = agg?.totalCredit ?: 0L

                val balance = when (account.type) {
                    AccountType.ASSET, AccountType.EXPENSE -> debit - credit
                    AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> credit - debit
                }

                when (account.type) {
                    AccountType.ASSET -> {
                        liquidNetWorth += balance
                        when {
                            account.id.contains("cbe", ignoreCase = true) -> cbeBalance += balance
                            account.id.contains("telebirr", ignoreCase = true) -> telebirrBalance += balance
                            account.id.contains("abyssinia", ignoreCase = true) -> abyssiniaBalance += balance
                        }
                    }
                    AccountType.LIABILITY -> {
                        liquidNetWorth -= balance
                    }
                    AccountType.REVENUE -> {
                        totalRevenue += balance
                        if (balance > 0) {
                            channelRevenuesList.add(
                                ChannelRevenueSummary(
                                    accountId = account.id,
                                    channelName = account.name,
                                    totalRevenueSantim = balance,
                                    percentageOfTotal = 0f
                                )
                            )
                        }
                    }
                    AccountType.EXPENSE -> {
                        totalExpense += balance
                    }
                    AccountType.EQUITY -> { /* Stored in balance sheet */ }
                }
            }

            // Calculate channel percentages
            val finalChannelSummaries = channelRevenuesList.map {
                val pct = if (totalRevenue > 0) (it.totalRevenueSantim.toFloat() / totalRevenue) * 100f else 0f
                it.copy(percentageOfTotal = pct)
            }.sortedByDescending { it.totalRevenueSantim }

            // Group recent transactions into cashflow points (e.g. 7-day or recent history)
            val dayFormatter = SimpleDateFormat("MMM dd", Locale.US)
            val recentTx = transactions.take(30).reversed()
            val cashFlowPoints = recentTx.map { tx ->
                val dateLabel = dayFormatter.format(Date(tx.occurredAt))
                CashFlowPoint(
                    timestamp = tx.occurredAt,
                    label = dateLabel,
                    revenueSantim = if (tx.rawAmount > 0) tx.rawAmount else 0L,
                    expenseSantim = tx.feeAmount,
                    netIncomeSantim = tx.rawAmount - tx.feeAmount
                )
            }

            DashboardSummary(
                liquidNetWorthSantim = liquidNetWorth,
                cbeBalanceSantim = cbeBalance,
                telebirrBalanceSantim = telebirrBalance,
                abyssiniaBalanceSantim = abyssiniaBalance,
                totalRevenueSantim = totalRevenue,
                totalExpenseSantim = totalExpense,
                channelRevenues = finalChannelSummaries,
                cashFlowPoints = cashFlowPoints,
                pendingTriageCount = untriaged.size
            )
        }
    }

    private fun resolveDefaultAssetAccount(bankName: String): String {
        val lower = bankName.lowercase()
        return when {
            lower.contains("telebirr") -> "asset:telebirr:main"
            lower.contains("cbe") -> "asset:cbe:main"
            lower.contains("abyssinia") -> "asset:abyssinia:main"
            else -> "asset:cash:wallet"
        }
    }
}
