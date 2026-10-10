package com.quatari.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Economic transaction entity representing an ingested or manually recorded financial event.
 * Primary key is deterministically generated via SHA-256(bank + normalized_ref) to ensure
 * duplicate SMS broadcasts never create duplicate transactions.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = RawSmsEntity::class,
            parentColumns = ["id"],
            childColumns = ["rawSmsId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["rawSmsId"]),
        Index(value = ["bankName"]),
        Index(value = ["referenceNumber"]),
        Index(value = ["isTriaged"]),
        Index(value = ["occurredAt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val bankName: String,
    val referenceNumber: String,
    val rawAmount: Long, // Measured strictly in Santim (1 ETB = 100 Santim)
    val feeAmount: Long = 0L, // Measured in Santim
    val reportedBalance: Long? = null, // Measured in Santim
    val occurredAt: Long,
    val rawSmsId: Long? = null,
    val isTriaged: Boolean = false,
    val notes: String? = null
)
