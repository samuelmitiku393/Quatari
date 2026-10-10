package com.quatari.app.domain.model

import com.quatari.app.data.local.entity.AccountEntity
import com.quatari.app.data.local.entity.JournalEntryEntity
import com.quatari.app.data.local.entity.TransactionEntity

/**
 * Combined view of a transaction along with its balanced double-entry line items.
 */
data class TransactionWithEntries(
    val transaction: TransactionEntity,
    val entries: List<JournalEntryEntity> = emptyList()
)

/**
 * Represents an account alongside its computed double-entry balance in Santim.
 */
data class AccountWithBalance(
    val account: AccountEntity,
    val balanceSantim: Long
)

/**
 * Revenue summary for a specific channel (e.g. Telegram Channel A).
 */
data class ChannelRevenueSummary(
    val accountId: String,
    val channelName: String,
    val totalRevenueSantim: Long,
    val percentageOfTotal: Float
)

/**
 * Historical cashflow data point for charting (Net income curve).
 */
data class CashFlowPoint(
    val timestamp: Long,
    val label: String,
    val revenueSantim: Long,
    val expenseSantim: Long,
    val netIncomeSantim: Long
)

/**
 * Aggregated dashboard financial intelligence.
 */
data class DashboardSummary(
    val liquidNetWorthSantim: Long,
    val cbeBalanceSantim: Long,
    val telebirrBalanceSantim: Long,
    val abyssiniaBalanceSantim: Long,
    val totalRevenueSantim: Long,
    val totalExpenseSantim: Long,
    val channelRevenues: List<ChannelRevenueSummary>,
    val cashFlowPoints: List<CashFlowPoint>,
    val pendingTriageCount: Int
)
