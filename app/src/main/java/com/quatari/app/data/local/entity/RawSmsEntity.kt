package com.quatari.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Raw SMS PDU record captured directly from the telephony broadcast.
 * Stored locally for tamper-proof auditing and deterministic reprocessing.
 */
@Entity(
    tableName = "raw_sms",
    indices = [
        Index(value = ["sender"]),
        Index(value = ["receivedAt"]),
        Index(value = ["processingStatus"])
    ]
)
data class RawSmsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val processingStatus: ProcessingStatus = ProcessingStatus.PENDING,
    val failureReason: String? = null
)
