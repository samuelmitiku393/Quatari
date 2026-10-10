package com.quatari.app.ui.triage

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quatari.app.data.local.entity.TransactionEntity
import com.quatari.app.domain.util.AmountUtils
import com.quatari.app.ui.theme.AbyssiniaAmber
import com.quatari.app.ui.theme.AbyssiniaYellow
import com.quatari.app.ui.theme.CbeGold
import com.quatari.app.ui.theme.CbePurple
import com.quatari.app.ui.theme.Emerald400
import com.quatari.app.ui.theme.Emerald500
import com.quatari.app.ui.theme.Rose500
import com.quatari.app.ui.theme.Slate700
import com.quatari.app.ui.theme.Slate800
import com.quatari.app.ui.theme.Slate900
import com.quatari.app.ui.theme.TelebirrPlum
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TriageCard(
    transaction: TransactionEntity,
    onQuickTag: (targetAccountId: String) -> Unit,
    onOpenModal: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipeRight()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeLeft()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Emerald500.copy(alpha = 0.85f)
                    SwipeToDismissBoxValue.EndToStart -> Rose500.copy(alpha = 0.85f)
                    SwipeToDismissBoxValue.Settled -> Slate800
                },
                label = "swipeBgColor"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Quick Ad Revenue",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ad Revenue (Swipe Right)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Personal Expense (Swipe Left)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Quick Expense",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Slate900
            ),
            border = borderStrokeForBank(transaction.bankName),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header: Bank badge & Occurred Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BankBadge(bankName = transaction.bankName)

                    val dateStr = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.US)
                        .format(Date(transaction.occurredAt))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount & Reference row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = AmountUtils.formatSantimToEtb(transaction.rawAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (transaction.feeAmount > 0L) {
                            Text(
                                text = "Fee: ${AmountUtils.formatSantimToEtb(transaction.feeAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (transaction.reportedBalance != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate800)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Bal: ${AmountUtils.formatSantimToEtb(transaction.reportedBalance)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald400,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Note & Reference info
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.notes ?: "Unclassified transaction",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )

                    Text(
                        text = "Ref: ${transaction.referenceNumber.take(14)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Tag Action Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PresetChip(
                        label = "Channel A Ad",
                        onClick = { onQuickTag("revenue:ad:channel_a") }
                    )
                    PresetChip(
                        label = "Channel B Ad",
                        onClick = { onQuickTag("revenue:ad:channel_b") }
                    )
                    PresetChip(
                        label = "Personal",
                        onClick = { onQuickTag("expense:personal") }
                    )
                    PresetChip(
                        label = "Transfer",
                        onClick = { onQuickTag("asset:cash:wallet") }
                    )
                    SuggestionChip(
                        onClick = onOpenModal,
                        label = { Text("More...", fontSize = 12.sp) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More categories",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Slate800,
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun BankBadge(bankName: String) {
    val (badgeBg, badgeText, badgeColor) = when {
        bankName.contains("telebirr", ignoreCase = true) ->
            Triple(TelebirrPlum.copy(alpha = 0.2f), "Telebirr", TelebirrPlum)
        bankName.contains("cbe", ignoreCase = true) ->
            Triple(CbePurple.copy(alpha = 0.2f), "CBE", CbeGold)
        bankName.contains("abyssinia", ignoreCase = true) ->
            Triple(AbyssiniaAmber.copy(alpha = 0.2f), "Bank of Abyssinia", AbyssiniaYellow)
        else ->
            Triple(Emerald500.copy(alpha = 0.2f), bankName, Emerald400)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = badgeText,
            color = badgeColor,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Slate800,
            labelColor = Color.White
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = Slate700
        )
    )
}

private fun borderStrokeForBank(bankName: String): androidx.compose.foundation.BorderStroke {
    val color = when {
        bankName.contains("telebirr", ignoreCase = true) -> TelebirrPlum.copy(alpha = 0.5f)
        bankName.contains("cbe", ignoreCase = true) -> CbePurple.copy(alpha = 0.5f)
        bankName.contains("abyssinia", ignoreCase = true) -> AbyssiniaAmber.copy(alpha = 0.5f)
        else -> Slate700
    }
    return androidx.compose.foundation.BorderStroke(1.dp, color)
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0A0F1D)
@Composable
private fun TriageCardPreview() {
    com.quatari.app.ui.theme.QuatariTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TriageCard(
                transaction = TransactionEntity(
                    id = "preview-1",
                    bankName = "Telebirr",
                    referenceNumber = "DJ90L0YWR6",
                    rawAmount = 26000L,
                    feeAmount = 200L,
                    reportedBalance = 633890L,
                    occurredAt = System.currentTimeMillis(),
                    notes = "Telebirr: SELAM AWULACHEW"
                ),
                onQuickTag = {},
                onOpenModal = {},
                onSwipeRight = {},
                onSwipeLeft = {}
            )

            TriageCard(
                transaction = TransactionEntity(
                    id = "preview-2",
                    bankName = "CBE",
                    referenceNumber = "v2-hfHCxHyR90",
                    rawAmount = 40000L,
                    feeAmount = 61L,
                    reportedBalance = 4970084L,
                    occurredAt = System.currentTimeMillis() - 3600000,
                    notes = "CBE: Sisay Fiseha Haileselassie"
                ),
                onQuickTag = {},
                onOpenModal = {},
                onSwipeRight = {},
                onSwipeLeft = {}
            )
        }
    }
}

