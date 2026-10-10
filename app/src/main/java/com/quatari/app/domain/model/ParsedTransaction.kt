package com.quatari.app.domain.model

enum class TransactionType {
    DEBIT,      // Outgoing money (expense/transfer out)
    CREDIT,     // Incoming money (income/deposit/transfer in)
    UNKNOWN
}

/**
 * Deterministically parsed representation of a financial SMS message.
 * All monetary amounts are normalized to integer Santim (1 ETB = 100 Santim).
 */
data class ParsedTransaction(
    val bankName: String,
    val referenceNumber: String,
    val rawAmount: Long, // Santim (1 ETB = 100 Santim)
    val feeAmount: Long = 0L, // Santim
    val reportedBalance: Long? = null, // Santim
    val occurredAt: Long,
    val transactionType: TransactionType = TransactionType.UNKNOWN,
    val counterparty: String? = null, // e.g. recipient or sender name
    val accountNumber: String? = null, // e.g. "1**4449" or "2*93"
    val notes: String? = null
)
