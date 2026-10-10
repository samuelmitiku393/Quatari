package com.quatari.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.quatari.app.QuatariApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * High-priority broadcast receiver intercepting incoming SMS PDUs.
 * Operates offline with zero external network connectivity.
 * Stitches multipart SMS, logs raw text, parses deterministically,
 * and routes to the double-entry triage queue.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val pendingResult = goAsync()

        // Stitch multipart concatenated messages grouped by originating address
        val groupedMessages = messages.groupBy { it.originatingAddress ?: "UNKNOWN" }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? QuatariApp
                val repository = app?.repository

                if (repository != null) {
                    for ((sender, parts) in groupedMessages) {
                        val fullBody = parts.joinToString(separator = "") { it.messageBody ?: "" }
                        val receivedAt = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

                        repository.processIncomingSms(
                            sender = sender,
                            body = fullBody,
                            receivedAt = receivedAt
                        )
                    }
                }
            } catch (_: Exception) {
                // Keep receiver resilient against exceptions
            } finally {
                pendingResult.finish()
            }
        }
    }
}
