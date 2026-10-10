package com.quatari.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quatari.app.data.local.entity.ProcessingStatus
import com.quatari.app.data.local.entity.RawSmsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RawSmsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sms: RawSmsEntity): Long

    @Update
    suspend fun update(sms: RawSmsEntity)

    @Query("SELECT * FROM raw_sms WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RawSmsEntity?

    @Query("SELECT * FROM raw_sms ORDER BY receivedAt DESC")
    fun getAllFlow(): Flow<List<RawSmsEntity>>

    @Query("SELECT * FROM raw_sms WHERE processingStatus = 'PENDING' ORDER BY receivedAt ASC")
    suspend fun getPending(): List<RawSmsEntity>

    @Query("UPDATE raw_sms SET processingStatus = :status, failureReason = :reason WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ProcessingStatus, reason: String? = null)
}
