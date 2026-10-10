package com.quatari.app.domain.parser

import com.quatari.app.domain.model.ParsedTransaction

/**
 * Deterministic engine routing raw incoming SMS messages to appropriate bank state-machines.
 */
class TransactionParserEngine(
    private val parsers: List<BankParser> = listOf(
        TelebirrParser(),
        CbeParser(),
        AbyssiniaParser()
    )
) {
    /**
     * Attempts to parse the SMS message.
     * @return [ParsedTransaction] if successfully parsed by any bank parser, null otherwise.
     */
    fun parse(sender: String, body: String, receivedAt: Long): ParsedTransaction? {
        val matchingParser = parsers.firstOrNull { it.canParse(sender, body) }
        return matchingParser?.parse(body, receivedAt)
    }

    /**
     * Determines whether the sender or message indicates a supported financial institution.
     */
    fun isFinancialMessage(sender: String, body: String): Boolean {
        return parsers.any { it.canParse(sender, body) }
    }
}
