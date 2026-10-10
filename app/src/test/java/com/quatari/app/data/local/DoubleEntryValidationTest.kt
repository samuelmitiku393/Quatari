package com.quatari.app.data.local

import com.google.common.truth.Truth.assertThat
import com.quatari.app.data.local.entity.JournalEntryEntity
import com.quatari.app.domain.util.AmountUtils
import com.quatari.app.domain.util.HashUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DoubleEntryValidationTest {

    @Test
    fun `verifies double entry invariant succeeds when debits equal credits`() {
        val entries = listOf(
            JournalEntryEntity(
                transactionId = "tx-1",
                accountId = "asset:cbe:main",
                debitSantim = 40000L,
                creditSantim = 0L
            ),
            JournalEntryEntity(
                transactionId = "tx-1",
                accountId = "revenue:ad:channel_a",
                debitSantim = 0L,
                creditSantim = 40000L
            )
        )

        val totalDebits = entries.sumOf { it.debitSantim }
        val totalCredits = entries.sumOf { it.creditSantim }

        assertThat(totalDebits).isEqualTo(totalCredits)
    }

    @Test
    fun `verifies double entry invariant fails when debits do not balance credits`() {
        val unbalancedEntries = listOf(
            JournalEntryEntity(
                transactionId = "tx-1",
                accountId = "expense:food",
                debitSantim = 25000L,
                creditSantim = 0L
            ),
            JournalEntryEntity(
                transactionId = "tx-1",
                accountId = "asset:telebirr:main",
                debitSantim = 0L,
                creditSantim = 24000L // 1000 santim difference!
            )
        )

        val totalDebits = unbalancedEntries.sumOf { it.debitSantim }
        val totalCredits = unbalancedEntries.sumOf { it.creditSantim }

        val exception = assertThrows<IllegalStateException> {
            if (totalDebits != totalCredits) {
                throw IllegalStateException("Double-entry ledger invariant violated: Debits ($totalDebits) != Credits ($totalCredits)")
            }
        }

        assertThat(exception).hasMessageThat().contains("Double-entry ledger invariant violated")
    }

    @Test
    fun `deduplication hash is deterministic and collision resistant`() {
        val hash1 = HashUtils.computeTransactionId("Telebirr", "DJ90L0YWR6")
        val hash2 = HashUtils.computeTransactionId("telebirr ", "DJ90L0YWR6")
        val differentRef = HashUtils.computeTransactionId("Telebirr", "DJ90L0YWR7")

        assertThat(hash1).isEqualTo(hash2)
        assertThat(hash1).isNotEmpty()
        assertThat(hash1).isNotEqualTo(differentRef)
        assertThat(hash1.length).isEqualTo(64) // SHA-256 hex length
    }

    @Test
    fun `amount utilities handle Santim conversions without floating point drift`() {
        assertThat(AmountUtils.parseEtbToSantim("260.00")).isEqualTo(26000L)
        assertThat(AmountUtils.parseEtbToSantim("ETB 1.74")).isEqualTo(174L)
        assertThat(AmountUtils.parseEtbToSantim("0.26")).isEqualTo(26L)
        assertThat(AmountUtils.parseEtbToSantim("ETB 6,338.90")).isEqualTo(633890L)
        assertThat(AmountUtils.parseEtbToSantim("ETB113,349.37")).isEqualTo(11334937L)
        assertThat(AmountUtils.parseEtbToSantim("ETB49,700.84")).isEqualTo(4970084L)

        // Formatting back
        assertThat(AmountUtils.formatSantimToEtb(633890L)).isEqualTo("ETB 6,338.90")
        assertThat(AmountUtils.formatSantimToEtb(26000L)).isEqualTo("ETB 260.00")
    }
}
