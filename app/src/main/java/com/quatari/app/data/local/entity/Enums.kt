package com.quatari.app.data.local.entity

/**
 * Lifecycle status of an intercepted SMS message.
 */
enum class ProcessingStatus {
    PENDING,
    PARSED,
    FAILED,
    DUPLICATE
}

/**
 * Standard double-entry bookkeeping account classifications.
 * Assets & Expenses increase with Debits.
 * Liabilities, Equity, & Revenues increase with Credits.
 */
enum class AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    REVENUE,
    EXPENSE
}
