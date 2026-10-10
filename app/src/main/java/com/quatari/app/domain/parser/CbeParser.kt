package com.quatari.app.domain.parser

import com.quatari.app.domain.model.ParsedTransaction
import com.quatari.app.domain.model.TransactionType
import com.quatari.app.domain.util.AmountUtils
import java.util.regex.Pattern

class CbeParser : BankParser {

    override val bankName: String = "CBE"

    private val amountTransferredPattern = Pattern.compile(
        """(?:transferred|paid|debited)\s*ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val amountCreditedPattern = Pattern.compile(
        """(?:credited\s*(?:with)?|deposited|received)\s*ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val fromAccountPattern = Pattern.compile(
        """from account\s*([0-9\*]+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val toAccountRecipientPattern = Pattern.compile(
        """to account\s*([0-9\*]+)(?:\s*\(([^)]+)\))?""",
        Pattern.CASE_INSENSITIVE
    )

    private val totalDebitedPattern = Pattern.compile(
        """with total of\s*ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val serviceChargePattern = Pattern.compile(
        """Service charge\s*(?:of)?\s*ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val vatChargePattern = Pattern.compile(
        """VAT\s*(?:\([^)]*\))?\s*(?:of)?\s*ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val disasterChargePattern = Pattern.compile(
        """Disaster Recovery\s*(?:\([^)]*\))?\s*(?:of)?\s*(?:ETB\s*)?([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val balancePattern = Pattern.compile(
        """current balance is\s*ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val receiptSlugPattern = Pattern.compile(
        """mbreciept\.cbe\.com\.et/([a-zA-Z0-9_\-]+)""",
        Pattern.CASE_INSENSITIVE
    )

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.lowercase()
        val b = body.lowercase()
        return s.contains("cbe") ||
                b.contains("thanks for banking with cbe") ||
                b.contains("mbreciept.cbe.com.et") ||
                b.contains("commercial bank of ethiopia")
    }

    override fun parse(body: String, receivedAt: Long): ParsedTransaction? {
        val refMatcher = receiptSlugPattern.matcher(body)
        val reference = if (refMatcher.find()) refMatcher.group(1)!!.trim() else return null

        var rawAmount = 0L
        var transactionType = TransactionType.UNKNOWN

        val transferMatcher = amountTransferredPattern.matcher(body)
        if (transferMatcher.find()) {
            rawAmount = AmountUtils.parseEtbToSantim(transferMatcher.group(1)!!)
            transactionType = TransactionType.DEBIT
        } else {
            val creditMatcher = amountCreditedPattern.matcher(body)
            if (creditMatcher.find()) {
                rawAmount = AmountUtils.parseEtbToSantim(creditMatcher.group(1)!!)
                transactionType = TransactionType.CREDIT
            } else {
                return null
            }
        }

        // Calculate Fee
        var feeAmount = 0L
        val totalMatcher = totalDebitedPattern.matcher(body)
        if (totalMatcher.find()) {
            val totalDebited = AmountUtils.parseEtbToSantim(totalMatcher.group(1)!!)
            if (totalDebited >= rawAmount) {
                feeAmount = totalDebited - rawAmount
            }
        } else {
            val scMatcher = serviceChargePattern.matcher(body)
            if (scMatcher.find()) {
                feeAmount += AmountUtils.parseEtbToSantim(scMatcher.group(1)!!)
            }
            val vatMatcher = vatChargePattern.matcher(body)
            if (vatMatcher.find()) {
                feeAmount += AmountUtils.parseEtbToSantim(vatMatcher.group(1)!!)
            }
            val drMatcher = disasterChargePattern.matcher(body)
            if (drMatcher.find()) {
                feeAmount += AmountUtils.parseEtbToSantim(drMatcher.group(1)!!)
            }
        }

        // Reported Balance
        var reportedBalance: Long? = null
        val balMatcher = balancePattern.matcher(body)
        if (balMatcher.find()) {
            reportedBalance = AmountUtils.parseEtbToSantim(balMatcher.group(1)!!)
        }

        // Accounts & Recipient
        var fromAccount: String? = null
        val fromMatcher = fromAccountPattern.matcher(body)
        if (fromMatcher.find()) {
            fromAccount = fromMatcher.group(1)!!.trim()
        }

        var counterparty: String? = null
        val toMatcher = toAccountRecipientPattern.matcher(body)
        if (toMatcher.find()) {
            counterparty = toMatcher.group(2)?.trim()
        }

        val note = when {
            counterparty != null -> "CBE: $counterparty"
            fromAccount != null -> "CBE ($fromAccount)"
            else -> "CBE transfer"
        }

        return ParsedTransaction(
            bankName = bankName,
            referenceNumber = reference,
            rawAmount = rawAmount,
            feeAmount = feeAmount,
            reportedBalance = reportedBalance,
            occurredAt = receivedAt, // SMS date absent from CBE message text, fallback to receivedAt
            transactionType = transactionType,
            counterparty = counterparty,
            accountNumber = fromAccount,
            notes = note
        )
    }
}
