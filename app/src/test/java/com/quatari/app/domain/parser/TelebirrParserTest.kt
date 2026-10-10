package com.quatari.app.domain.parser

import com.google.common.truth.Truth.assertThat
import com.quatari.app.domain.model.TransactionType
import org.junit.jupiter.api.Test

class TelebirrParserTest {

    private val parser = TelebirrParser()

    @Test
    fun `parses standard Telebirr transfer test vector with exact santim precision`() {
        val telebirrSample = """
            Dear Samuel 
            You have transferred ETB 260.00 to SELAM AWULACHEW (2519****9626) on 09/10/2026 08:07:05. Your transaction number is DJ90L0YWR6. The service fee is  ETB 1.74 and  15% VAT on the service fee is ETB 0.26. Your current E-Money Account  balance is ETB 6,338.90. To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DJ90L0YWR6.
        """.trimIndent()

        val canParse = parser.canParse("telebirr", telebirrSample)
        assertThat(canParse).isTrue()

        val result = parser.parse(telebirrSample, receivedAt = 1791533225000L)
        assertThat(result).isNotNull()
        assertThat(result!!.bankName).isEqualTo("Telebirr")
        assertThat(result.rawAmount).isEqualTo(26000L) // 260.00 ETB = 26000 Santim
        assertThat(result.feeAmount).isEqualTo(200L) // 1.74 + 0.26 = 2.00 ETB = 200 Santim
        assertThat(result.referenceNumber).isEqualTo("DJ90L0YWR6")
        assertThat(result.reportedBalance).isEqualTo(633890L) // 6,338.90 ETB = 633890 Santim
        assertThat(result.transactionType).isEqualTo(TransactionType.DEBIT)
        assertThat(result.counterparty).isEqualTo("SELAM AWULACHEW")
    }

    @Test
    fun `parses incoming Telebirr deposit or received payment`() {
        val receivedSample = """
            You have received ETB 1,500.00 from KEBEDE TESFA (2519****1234) on 08/10/2026 14:20:00. Your transaction number is AB12C3D4E5. Your current E-Money Account balance is ETB 7,838.90.
        """.trimIndent()

        val result = parser.parse(receivedSample, receivedAt = 1000L)
        assertThat(result).isNotNull()
        assertThat(result!!.rawAmount).isEqualTo(150000L) // 1500.00 ETB
        assertThat(result.referenceNumber).isEqualTo("AB12C3D4E5")
        assertThat(result.reportedBalance).isEqualTo(783890L)
        assertThat(result.transactionType).isEqualTo(TransactionType.CREDIT)
        assertThat(result.counterparty).isEqualTo("KEBEDE TESFA")
    }
}
