package com.quatari.app.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quatari.app.data.local.entity.AccountEntity
import com.quatari.app.domain.model.TransactionWithEntries
import com.quatari.app.domain.util.AmountUtils
import com.quatari.app.ui.theme.Emerald400
import com.quatari.app.ui.theme.Rose500
import com.quatari.app.ui.theme.Slate700
import com.quatari.app.ui.theme.Slate800
import com.quatari.app.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailDialog(
    item: TransactionWithEntries,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit
) {
    val tx = item.transaction
    val accountMap = accounts.associateBy { it.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        shape = RoundedCornerShape(18.dp),
        title = {
            Column {
                Text(
                    text = "Transaction Audit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Reference: ${tx.referenceNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Info block
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DetailRow("Institution", tx.bankName)
                        DetailRow("Amount", AmountUtils.formatSantimToEtb(tx.rawAmount))
                        if (tx.feeAmount > 0) {
                            DetailRow("Fee", AmountUtils.formatSantimToEtb(tx.feeAmount))
                        }
                        if (tx.reportedBalance != null) {
                            DetailRow("Balance", AmountUtils.formatSantimToEtb(tx.reportedBalance))
                        }
                        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(tx.occurredAt))
                        DetailRow("Date", dateStr)
                        DetailRow("Status", if (tx.isTriaged) "Classified & Balanced" else "Pending Triage")
                        tx.notes?.let { DetailRow("Notes", it) }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Double-Entry Journal Entries:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (item.entries.isEmpty()) {
                    Text(
                        text = "No balancing journal entries yet. This item is still in Triage Inbox.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (entry in item.entries) {
                            val accName = accountMap[entry.accountId]?.name ?: entry.accountId
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Slate800),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = accName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = entry.accountId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate700,
                                            fontSize = 10.sp
                                        )
                                    }

                                    if (entry.debitSantim > 0) {
                                        Text(
                                            text = "DR ${AmountUtils.formatSantimToEtb(entry.debitSantim)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald400
                                        )
                                    } else {
                                        Text(
                                            text = "CR ${AmountUtils.formatSantimToEtb(entry.creditSantim)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Rose500
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Emerald400)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
