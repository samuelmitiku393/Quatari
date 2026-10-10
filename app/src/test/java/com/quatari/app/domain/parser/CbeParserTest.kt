package com.quatari.app.domain.parser

import com.google.common.truth.Truth.assertThat
import com.quatari.app.domain.model.TransactionType
import org.junit.jupiter.api.Test

class CbeParserTest {

    private val parser = CbeParser()

    @Test
    fun `parses standard CBE transfer test vector with concatenated missing spaces and fee breakdown`() {
        val cbeSample = """
            Dear  Samuel Mitiku Eshetu You have successfully transferred ETB400.00 from account 1**4449 to account 1**7318 (Sisay Fiseha Haileselassie). Service charge of ETB 0.50 and VAT(15%) of ETB0.08 and Disaster Recovery(5%) of 0.03 with total of ETB400.61 .Your current balance is ETB49,700.84. Thanks for Banking with CBE. https://mbreciept.cbe.com.et/v2-hfHCxHyR90zec7I8nEPo
        """.trimIndent()

        val canParse = parser.canParse("CBE", cbeSample)
        assertThat(canParse).isTrue()

        val fallbackTimestamp = 1791535000000L
        val result = parser.parse(cbeSample, receivedAt = fallbackTimestamp)

        assertThat(result).isNotNull()
        assertThat(result!!.bankName).isEqualTo("CBE")
        assertThat(result.rawAmount).isEqualTo(40000L) // 400.00 ETB = 40000 Santim
        assertThat(result.feeAmount).isEqualTo(61L) // 400.61 - 400.00 = 0.61 ETB = 61 Santim
        assertThat(result.referenceNumber).isEqualTo("v2-hfHCxHyR90zec7I8nEPo")
        assertThat(result.reportedBalance).isEqualTo(4970084L) // 49,700.84 ETB = 4970084 Santim
        assertThat(result.accountNumber).isEqualTo("1**4449")
        assertThat(result.counterparty).isEqualTo("Sisay Fiseha Haileselassie")
        assertThat(result.transactionType).isEqualTo(TransactionType.DEBIT)
        assertThat(result.occurredAt).isEqualTo(fallbackTimestamp)
    }

    @Test
    fun `parses incoming CBE credited deposit`() {
        val creditedSample = """
            Dear Customer, Your account 1**4449 was credited with ETB 5,000.00. Your current balance is ETB54,700.84. Thanks for Banking with CBE. https://mbreciept.cbe.com.et/cbe-tx-999888
        """.trimIndent()

        val result = parser.parse(creditedSample, receivedAt = 5000L)
        assertThat(result).isNotNull()
        assertThat(result!!.rawAmount).isEqualTo(500000L) // 5000.00 ETB
        assertThat(result.feeAmount).isEqualTo(0L)
        assertThat(result.reportedBalance).isEqualTo(5470084L)
        assertThat(result.referenceNumber).isEqualTo("cbe-tx-999888")
        assertThat(result.transactionType).isEqualTo(TransactionType.CREDIT)
    }
}
