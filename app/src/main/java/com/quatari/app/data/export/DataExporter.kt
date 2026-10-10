package com.quatari.app.data.export

import android.content.Context
import com.quatari.app.data.local.HisabeDatabase
import com.quatari.app.data.local.entity.AccountEntity
import com.quatari.app.data.local.entity.JournalEntryEntity
import com.quatari.app.data.local.entity.TransactionEntity
import com.quatari.app.domain.util.AmountUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * Exports the double-entry ledger into a standard CSV format.
     */
    suspend fun exportLedgerToCsv(
        outputStream: OutputStream,
        transactions: List<TransactionEntity>,
        journalEntries: List<JournalEntryEntity>,
        accounts: List<AccountEntity>
    ) = withContext(Dispatchers.IO) {
        val accountMap = accounts.associateBy { it.id }
        val entriesByTx = journalEntries.groupBy { it.transactionId }

        outputStream.bufferedWriter().use { writer ->
            // Header
            writer.write("Transaction_ID,Date_Time,Bank,Reference,Amount_ETB,Fee_ETB,Balance_ETB,Is_Triaged,Notes,Account_ID,Account_Name,Debit_ETB,Credit_ETB\n")

            for (tx in transactions) {
                val dateStr = dateFormat.format(Date(tx.occurredAt))
                val rawEtb = AmountUtils.formatSantimToEtb(tx.rawAmount, includeCurrency = false)
                val feeEtb = AmountUtils.formatSantimToEtb(tx.feeAmount, includeCurrency = false)
                val balEtb = tx.reportedBalance?.let { AmountUtils.formatSantimToEtb(it, includeCurrency = false) } ?: ""
                val notes = (tx.notes ?: "").replace("\"", "\"\"")

                val entries = entriesByTx[tx.id] ?: emptyList()
                if (entries.isEmpty()) {
                    // Untriaged or single line
                    writer.write("\"${tx.id}\",\"$dateStr\",\"${tx.bankName}\",\"${tx.referenceNumber}\",$rawEtb,$feeEtb,\"$balEtb\",${tx.isTriaged},\"$notes\",\"\",\"\",0.00,0.00\n")
                } else {
                    for (entry in entries) {
                        val accName = accountMap[entry.accountId]?.name ?: entry.accountId
                        val debitEtb = AmountUtils.formatSantimToEtb(entry.debitSantim, includeCurrency = false)
                        val creditEtb = AmountUtils.formatSantimToEtb(entry.creditSantim, includeCurrency = false)
                        writer.write("\"${tx.id}\",\"$dateStr\",\"${tx.bankName}\",\"${tx.referenceNumber}\",$rawEtb,$feeEtb,\"$balEtb\",${tx.isTriaged},\"$notes\",\"${entry.accountId}\",\"$accName\",$debitEtb,$creditEtb\n")
                    }
                }
            }
            writer.flush()
        }
    }

    /**
     * Performs an atomic SQLite database backup by reading the database file.
     */
    suspend fun exportDatabaseBackup(
        context: Context,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        // Trigger a checkpoint to merge WAL data into the main database file
        val db = HisabeDatabase.getInstance(context)
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

        val dbFile = context.getDatabasePath(HisabeDatabase.DATABASE_NAME)
        if (dbFile.exists()) {
            FileInputStream(dbFile).use { input ->
                input.copyTo(outputStream)
            }
            outputStream.flush()
        }
    }
}
