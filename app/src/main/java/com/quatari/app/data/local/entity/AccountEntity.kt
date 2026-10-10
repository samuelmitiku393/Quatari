package com.quatari.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Chart-of-Accounts entity representing financial nodes in the double-entry ledger.
 * Examples:
 * - asset:cbe:4449
 * - asset:telebirr:main
 * - asset:abyssinia:2x93
 * - asset:cash:wallet
 * - revenue:ad:channel_a
 * - revenue:ad:channel_b
 * - expense:operating:food
 * - expense:banking:fee
 */
@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["type"]),
        Index(value = ["isArchived"])
    ]
)
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: AccountType,
    val isArchived: Boolean = false
)
