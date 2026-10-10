package com.quatari.app.domain.parser

import com.quatari.app.domain.model.ParsedTransaction
import com.quatari.app.domain.model.TransactionType
import com.quatari.app.domain.util.AmountUtils
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

class TelebirrParser : BankParser {

    override val bankName: String = "Telebirr"

    private val amountPattern = Pattern.compile(
        """(?:transferred|deposited|received|paid)\s+ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val recipientPattern = Pattern.compile(
        """to\s+([^(]+?)\s*\(""",
        Pattern.CASE_INSENSITIVE
    )

    private val senderPattern = Pattern.compile(
        """from\s+([^(]+?)\s*\(""",
        Pattern.CASE_INSENSITIVE
    )

    private val datePattern = Pattern.compile(
        """on\s+(\d{2}/\d{2}/\d{4}\s+\d{2}:\d{2}:\d{2})""",
        Pattern.CASE_INSENSITIVE
    )

    private val refPattern = Pattern.compile(
        """(?:transaction number is|receipt/)\s*([A-Za-z0-9]+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val serviceFeePattern = Pattern.compile(
        """service fee is\s+ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val vatFeePattern = Pattern.compile(
        """VAT\s*(?:on the service fee\s*)?is\s*ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val balancePattern = Pattern.compile(
        """(?:balance is|balance\s*ETB)\s*ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.lowercase()
        val b = body.lowercase()
        return s.contains("telebirr") || b.contains("telebirr") || b.contains("e-money account")
    }

    override fun parse(body: String, receivedAt: Long): ParsedTransaction? {
        val refMatcher = refPattern.matcher(body)
        val reference = if (refMatcher.find()) refMatcher.group(1)!!.trim() else return null

        val amountMatcher = amountPattern.matcher(body)
        val rawAmount = if (amountMatcher.find()) {
            AmountUtils.parseEtbToSantim(amountMatcher.group(1)!!)
        } else {
            return null
        }

        // Calculate total fees: Service Fee + VAT
        var totalFee = 0L
        val feeMatcher = serviceFeePattern.matcher(body)
        if (feeMatcher.find()) {
            totalFee += AmountUtils.parseEtbToSantim(feeMatcher.group(1)!!)
        }
        val vatMatcher = vatFeePattern.matcher(body)
        if (vatMatcher.find()) {
            totalFee += AmountUtils.parseEtbToSantim(vatMatcher.group(1)!!)
        }

        // Reported balance
        var balance: Long? = null
        val balMatcher = balancePattern.matcher(body)
        if (balMatcher.find()) {
            balance = AmountUtils.parseEtbToSantim(balMatcher.group(1)!!)
        }

        // Timestamp parsing (dd/MM/yyyy HH:mm:ss)
        var occurredAt = receivedAt
        val dMatcher = datePattern.matcher(body)
        if (dMatcher.find()) {
            val dateStr = dMatcher.group(1)!!.trim()
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("GMT+3")
                }
                sdf.parse(dateStr)?.let { occurredAt = it.time }
            } catch (_: Exception) {
                // Fallback to receivedAt
            }
        }

        // Counterparty
        val counterparty = when {
            recipientPattern.matcher(body).find() -> {
                val m = recipientPattern.matcher(body)
                m.find()
                m.group(1)?.trim()
            }
            senderPattern.matcher(body).find() -> {
                val m = senderPattern.matcher(body)
                m.find()
                m.group(1)?.trim()
            }
            else -> null
        }

        val transactionType = when {
            body.contains("transferred", ignoreCase = true) || body.contains("paid", ignoreCase = true) ->
                TransactionType.DEBIT
            body.contains("received", ignoreCase = true) || body.contains("deposited", ignoreCase = true) ->
                TransactionType.CREDIT
            else -> TransactionType.UNKNOWN
        }

        val note = when {
            counterparty != null -> "Telebirr: $counterparty"
            else -> "Telebirr transfer"
        }

        return ParsedTransaction(
            bankName = bankName,
            referenceNumber = reference,
            rawAmount = rawAmount,
            feeAmount = totalFee,
            reportedBalance = balance,
            occurredAt = occurredAt,
            transactionType = transactionType,
            counterparty = counterparty,
            notes = note
        )
    }
}
