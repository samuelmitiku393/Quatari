package com.quatari.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quatari.app.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

data class AccountBalanceAggregate(
    val accountId: String,
    val totalDebit: Long,
    val totalCredit: Long
)

data class ChannelRevenueAggregate(
    val accountId: String,
    val accountName: String,
    val totalRevenueSantim: Long
)

@Dao
interface JournalEntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: JournalEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entries: List<JournalEntryEntity>)

    @Query("DELETE FROM journal_entries WHERE transactionId = :transactionId")
    suspend fun deleteByTransactionId(transactionId: String)

    @Query("SELECT * FROM journal_entries WHERE transactionId = :transactionId ORDER BY id ASC")
    suspend fun getByTransactionId(transactionId: String): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries WHERE transactionId = :transactionId ORDER BY id ASC")
    fun getByTransactionIdFlow(transactionId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries ORDER BY id ASC")
    fun getAllFlow(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries ORDER BY id ASC")
    suspend fun getAllSync(): List<JournalEntryEntity>

    @Query("""
        SELECT 
            accountId, 
            COALESCE(SUM(debitSantim), 0) AS totalDebit, 
            COALESCE(SUM(creditSantim), 0) AS totalCredit 
        FROM journal_entries 
        GROUP BY accountId
    """)
    fun getAccountBalanceAggregatesFlow(): Flow<List<AccountBalanceAggregate>>

    @Query("""
        SELECT 
            accountId, 
            COALESCE(SUM(debitSantim), 0) AS totalDebit, 
            COALESCE(SUM(creditSantim), 0) AS totalCredit 
        FROM journal_entries 
        GROUP BY accountId
    """)
    suspend fun getAccountBalanceAggregatesSync(): List<AccountBalanceAggregate>

    @Query("""
        SELECT 
            je.accountId AS accountId,
            a.name AS accountName,
            COALESCE(SUM(je.creditSantim - je.debitSantim), 0) AS totalRevenueSantim
        FROM journal_entries je
        INNER JOIN accounts a ON je.accountId = a.id
        INNER JOIN transactions t ON je.transactionId = t.id
        WHERE a.type = 'REVENUE'
          AND (:startTime IS NULL OR t.occurredAt >= :startTime)
          AND (:endTime IS NULL OR t.occurredAt <= :endTime)
        GROUP BY je.accountId, a.name
        ORDER BY totalRevenueSantim DESC
    """)
    fun getRevenueByChannelFlow(
        startTime: Long?,
        endTime: Long?
    ): Flow<List<ChannelRevenueAggregate>>
}
