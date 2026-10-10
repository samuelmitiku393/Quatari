package com.quatari.app.domain.util

import java.math.BigDecimal
import java.text.DecimalFormat

object AmountUtils {

    /**
     * Parse an ETB currency string into Santim (integer cents: 1 ETB = 100 Santim).
     * Strips currency symbols, commas, and trailing whitespaces.
     * Uses BigDecimal for deterministic, loss-free financial arithmetic.
     */
    fun parseEtbToSantim(amountStr: String): Long {
        val cleaned = amountStr
            .replace("ETB", "", ignoreCase = true)
            .replace("Br", "", ignoreCase = true)
            .replace(",", "")
            .trim()

        if (cleaned.isEmpty()) return 0L

        return try {
            val bd = BigDecimal(cleaned)
            bd.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLongExact()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Formats integer Santim into human-readable ETB format.
     * Example: 633890L -> "ETB 6,338.90"
     */
    fun formatSantimToEtb(santim: Long, includeCurrency: Boolean = true): String {
        val isNegative = santim < 0
        val absSantim = kotlin.math.abs(santim)
        val whole = absSantim / 100
        val cents = absSantim % 100

        val formatter = DecimalFormat("#,##0")
        val formattedWhole = formatter.format(whole)
        val formattedCents = String.format("%02d", cents)

        val prefix = if (isNegative) "-" else ""
        val currency = if (includeCurrency) "ETB " else ""

        return "$prefix$currency$formattedWhole.$formattedCents"
    }
}
