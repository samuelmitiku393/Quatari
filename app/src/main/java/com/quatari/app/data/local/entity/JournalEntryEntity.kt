package com.quatari.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Atomic double-entry ledger line item.
 * Every transaction consists of >= 2 journal entries where:
 * SUM(debitSantim) == SUM(creditSantim)
 */
@Entity(
    tableName = "journal_entries",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["accountId"])
    ]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val transactionId: String,
    val accountId: String,
    val debitSantim: Long = 0L,
    val creditSantim: Long = 0L
)
