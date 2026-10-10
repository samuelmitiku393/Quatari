package com.quatari.app.domain.parser

import com.google.common.truth.Truth.assertThat
import com.quatari.app.domain.model.TransactionType
import org.junit.jupiter.api.Test

class AbyssiniaParserTest {

    private val parser = AbyssiniaParser()

    @Test
    fun `parses standard Bank of Abyssinia debit test vector with exact parameters`() {
        val boaSample = """
            Dear Samuel, your account 2*93 was debited with ETB 300.00. Available Balance: ETB 113,349.37.
            Receipt: https://cs.bankofabyssinia.com/slip/?trx=FT26272208DB83593
            Feedback: https://cs.bankofabyssinia.com/cs/?trx=DFT26272208DB
        """.trimIndent()

        val canParse = parser.canParse("Abyssinia", boaSample)
        assertThat(canParse).isTrue()

        val result = parser.parse(boaSample, receivedAt = 1791534000000L)
        assertThat(result).isNotNull()
        assertThat(result!!.bankName).isEqualTo("Abyssinia")
        assertThat(result.accountNumber).isEqualTo("2*93")
        assertThat(result.transactionType).isEqualTo(TransactionType.DEBIT)
        assertThat(result.rawAmount).isEqualTo(30000L) // 300.00 ETB = 30000 Santim
        assertThat(result.feeAmount).isEqualTo(0L)
        assertThat(result.referenceNumber).isEqualTo("FT26272208DB83593")
        assertThat(result.reportedBalance).isEqualTo(11334937L) // 113,349.37 ETB = 11334937 Santim
    }

    @Test
    fun `parses Bank of Abyssinia credit transaction`() {
        val creditedSample = """
            Dear Customer, your account 2*93 was credited with ETB 10,000.00. Available Balance: ETB 123,349.37.
            Receipt: https://cs.bankofabyssinia.com/slip/?trx=FT99887766AA
        """.trimIndent()

        val result = parser.parse(creditedSample, receivedAt = 2000L)
        assertThat(result).isNotNull()
        assertThat(result!!.transactionType).isEqualTo(TransactionType.CREDIT)
        assertThat(result.rawAmount).isEqualTo(1000000L)
        assertThat(result.referenceNumber).isEqualTo("FT99887766AA")
        assertThat(result.reportedBalance).isEqualTo(12334937L)
    }
}
