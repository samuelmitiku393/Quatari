package com.quatari.app.domain.util

import java.security.MessageDigest

object HashUtils {

    /**
     * Computes the deterministic SHA-256 fingerprint for deduplication.
     * Formula: SHA-256(normalized_bank + ":" + normalized_ref)
     */
    fun computeTransactionId(bank: String, reference: String): String {
        val normalizedBank = bank.trim().lowercase()
        val normalizedRef = reference.trim()
        val rawKey = "$normalizedBank:$normalizedRef"

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawKey.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
