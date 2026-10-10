package com.quatari.app.domain.parser

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class TransactionParserEngineTest {

    private val engine = TransactionParserEngine()

    @Test
    fun `routes each bank message to appropriate parser`() {
        val telebirrSms = "Dear user You have transferred ETB 50.00 to ABEBE (2519111111) on 01/01/2026 10:00:00. Your transaction number is TB123. The service fee is ETB 0.50. Your current E-Money Account balance is ETB 100.00."
        val parsedTb = engine.parse("telebirr", telebirrSms, 1000L)
        assertThat(parsedTb).isNotNull()
        assertThat(parsedTb!!.bankName).isEqualTo("Telebirr")

        val boaSms = "Dear user, your account 1*23 was debited with ETB 150.00. Available Balance: ETB 500.00. Receipt: https://cs.bankofabyssinia.com/slip/?trx=BOA456"
        val parsedBoa = engine.parse("BankOfAbyssinia", boaSms, 2000L)
        assertThat(parsedBoa).isNotNull()
        assertThat(parsedBoa!!.bankName).isEqualTo("Abyssinia")

        val cbeSms = "Dear user You have successfully transferred ETB200.00 from account 1**1 to account 1**2 (Chala). with total of ETB200.50 .Your current balance is ETB1,000.00. Thanks for Banking with CBE. https://mbreciept.cbe.com.et/cbe789"
        val parsedCbe = engine.parse("CBE", cbeSms, 3000L)
        assertThat(parsedCbe).isNotNull()
        assertThat(parsedCbe!!.bankName).isEqualTo("CBE")
    }

    @Test
    fun `returns null on non-financial SMS`() {
        val spamSms = "Win a free phone by sending WIN to 8888! Terms and conditions apply."
        val result = engine.parse("8888", spamSms, 1000L)
        assertThat(result).isNull()
        assertThat(engine.isFinancialMessage("8888", spamSms)).isFalse()
    }
}
