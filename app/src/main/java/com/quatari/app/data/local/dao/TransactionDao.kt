package com.quatari.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quatari.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT COUNT(*) > 0 FROM transactions WHERE id = :id")
    suspend fun exists(id: String): Boolean

    @Query("SELECT * FROM transactions WHERE isTriaged = 0 ORDER BY occurredAt DESC")
    fun getUntriagedFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isTriaged = 0 ORDER BY occurredAt DESC")
    suspend fun getUntriagedSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun getAllFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    suspend fun getAllSync(): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        WHERE (:bankFilter IS NULL OR bankName = :bankFilter)
          AND (:isTriaged IS NULL OR isTriaged = :isTriaged)
          AND (:startTime IS NULL OR occurredAt >= :startTime)
          AND (:endTime IS NULL OR occurredAt <= :endTime)
        ORDER BY occurredAt DESC
    """)
    fun getFilteredFlow(
        bankFilter: String?,
        isTriaged: Boolean?,
        startTime: Long?,
        endTime: Long?
    ): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET isTriaged = :isTriaged WHERE id = :id")
    suspend fun setTriagedStatus(id: String, isTriaged: Boolean)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String)
}
