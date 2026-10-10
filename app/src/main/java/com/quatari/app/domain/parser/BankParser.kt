package com.quatari.app.domain.parser

import com.quatari.app.domain.model.ParsedTransaction

/**
 * Deterministic parser interface for extracting financial telemetry from bank SMS PDUs.
 */
sealed interface BankParser {
    val bankName: String

    /**
     * Inspects whether this parser can handle the given sender/body tuple.
     */
    fun canParse(sender: String, body: String): Boolean

    /**
     * Parses the SMS text deterministically.
     * @param body The SMS message body.
     * @param receivedAt The epoch millis when the SMS was received.
     * @return [ParsedTransaction] if successfully parsed, null if parsing failed.
     */
    fun parse(body: String, receivedAt: Long): ParsedTransaction?
}
