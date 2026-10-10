package com.quatari.app.ui.triage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quatari.app.data.local.entity.AccountEntity
import com.quatari.app.data.local.entity.AccountType
import com.quatari.app.data.local.entity.TransactionEntity
import com.quatari.app.domain.util.AmountUtils
import com.quatari.app.ui.theme.Emerald400
import com.quatari.app.ui.theme.Slate700
import com.quatari.app.ui.theme.Slate800
import com.quatari.app.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCategorizeSheet(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    onSelectAccount: (accountId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Slate900,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Classify Transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${transaction.bankName} · ${AmountUtils.formatSantimToEtb(transaction.rawAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Emerald400
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Slate700)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select Chart-of-Accounts Node:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Group accounts by type
            val groupedAccounts = accounts.groupBy { it.type }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                groupedAccounts.forEach { (type, accList) ->
                    item {
                        Text(
                            text = type.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(accList) { account ->
                        AccountItemRow(account = account) {
                            onSelectAccount(account.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountItemRow(
    account: AccountEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = account.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate700,
                    fontSize = 11.sp
                )
            }

            Text(
                text = account.type.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
