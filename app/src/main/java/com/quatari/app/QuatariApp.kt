package com.quatari.app

import android.app.Application
import com.quatari.app.data.local.HisabeDatabase
import com.quatari.app.data.repository.DoubleEntryLedgerRepository
import com.quatari.app.data.repository.DoubleEntryLedgerRepositoryImpl
import com.quatari.app.domain.parser.TransactionParserEngine

class QuatariApp : Application() {

    lateinit var database: HisabeDatabase
        private set

    lateinit var repository: DoubleEntryLedgerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = HisabeDatabase.getInstance(this)
        repository = DoubleEntryLedgerRepositoryImpl(
            database = database,
            parserEngine = TransactionParserEngine()
        )
    }
}
