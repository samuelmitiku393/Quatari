package com.quatari.app.domain.parser

import com.quatari.app.domain.model.ParsedTransaction
import com.quatari.app.domain.model.TransactionType
import com.quatari.app.domain.util.AmountUtils
import java.util.regex.Pattern

class AbyssiniaParser : BankParser {

    override val bankName: String = "Abyssinia"

    private val accountPattern = Pattern.compile(
        """account\s+([0-9\*]+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val amountPattern = Pattern.compile(
        """with ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val balancePattern = Pattern.compile(
        """Available Balance:\s*ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val receiptTrxPattern = Pattern.compile(
        """(?:slip/\?trx=|trx=)([a-zA-Z0-9]+)""",
        Pattern.CASE_INSENSITIVE
    )

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.lowercase()
        val b = body.lowercase()
        return s.contains("abyssinia") ||
                s.contains("boa") ||
                b.contains("bank of abyssinia") ||
                b.contains("cs.bankofabyssinia.com")
    }

    override fun parse(body: String, receivedAt: Long): ParsedTransaction? {
        val trxMatcher = receiptTrxPattern.matcher(body)
        val reference = if (trxMatcher.find()) trxMatcher.group(1)!!.trim() else return null

        val amountMatcher = amountPattern.matcher(body)
        val rawAmount = if (amountMatcher.find()) {
            AmountUtils.parseEtbToSantim(amountMatcher.group(1)!!)
        } else {
            return null
        }

        var balance: Long? = null
        val balMatcher = balancePattern.matcher(body)
        if (balMatcher.find()) {
            balance = AmountUtils.parseEtbToSantim(balMatcher.group(1)!!)
        }

        var accountNumber: String? = null
        val accMatcher = accountPattern.matcher(body)
        if (accMatcher.find()) {
            accountNumber = accMatcher.group(1)!!.trim()
        }

        val isDebit = body.contains("was debited", ignoreCase = true)
        val isCredit = body.contains("was credited", ignoreCase = true)
        val transactionType = when {
            isDebit -> TransactionType.DEBIT
            isCredit -> TransactionType.CREDIT
            else -> TransactionType.UNKNOWN
        }

        val note = when {
            accountNumber != null -> "BoA ($accountNumber)"
            else -> "Bank of Abyssinia"
        }

        return ParsedTransaction(
            bankName = bankName,
            referenceNumber = reference,
            rawAmount = rawAmount,
            feeAmount = 0L,
            reportedBalance = balance,
            occurredAt = receivedAt,
            transactionType = transactionType,
            counterparty = null,
            accountNumber = accountNumber,
            notes = note
        )
    }
}
