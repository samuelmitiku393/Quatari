package com.quatari.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.quatari.app.data.local.dao.AccountDao
import com.quatari.app.data.local.dao.JournalEntryDao
import com.quatari.app.data.local.dao.RawSmsDao
import com.quatari.app.data.local.dao.TransactionDao
import com.quatari.app.data.local.entity.AccountEntity
import com.quatari.app.data.local.entity.AccountType
import com.quatari.app.data.local.entity.JournalEntryEntity
import com.quatari.app.data.local.entity.RawSmsEntity
import com.quatari.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        RawSmsEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        JournalEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HisabeDatabase : RoomDatabase() {

    abstract fun rawSmsDao(): RawSmsDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        const val DATABASE_NAME = "hisabe_database"

        @Volatile
        private var INSTANCE: HisabeDatabase? = null

        fun getInstance(context: Context): HisabeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HisabeDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_ACCOUNTS = listOf(
            // Assets
            AccountEntity("asset:cbe:main", "CBE Account", AccountType.ASSET),
            AccountEntity("asset:telebirr:main", "Telebirr Wallet", AccountType.ASSET),
            AccountEntity("asset:abyssinia:main", "Bank of Abyssinia", AccountType.ASSET),
            AccountEntity("asset:cash:wallet", "Cash on Hand", AccountType.ASSET),

            // Revenues (e.g. Telegram Channels)
            AccountEntity("revenue:ad:channel_a", "Channel A Ad Revenue", AccountType.REVENUE),
            AccountEntity("revenue:ad:channel_b", "Channel B Ad Revenue", AccountType.REVENUE),
            AccountEntity("revenue:general", "General Income", AccountType.REVENUE),

            // Expenses
            AccountEntity("expense:personal", "Personal Spending", AccountType.EXPENSE),
            AccountEntity("expense:operating", "Business Operations", AccountType.EXPENSE),
            AccountEntity("expense:banking:fees", "Bank & Service Fees", AccountType.EXPENSE),
            AccountEntity("expense:food", "Food & Dining", AccountType.EXPENSE),
            AccountEntity("expense:transport", "Transport & Fuel", AccountType.EXPENSE),
            AccountEntity("expense:utilities", "Utilities & Phone", AccountType.EXPENSE),

            // Equity & Liabilities
            AccountEntity("equity:opening_balance", "Opening Equity", AccountType.EQUITY),
            AccountEntity("liability:payables", "Accounts Payable", AccountType.LIABILITY)
        )

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed default chart of accounts
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        database.accountDao().insertAll(DEFAULT_ACCOUNTS)
                    }
                }
            }
        }
    }
}
