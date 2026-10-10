package com.quatari.app.ui.ledger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quatari.app.data.local.entity.TransactionEntity
import com.quatari.app.domain.util.AmountUtils
import com.quatari.app.ui.theme.Emerald400
import com.quatari.app.ui.theme.Emerald500
import com.quatari.app.ui.theme.Rose500
import com.quatari.app.ui.theme.Slate700
import com.quatari.app.ui.theme.Slate800
import com.quatari.app.ui.theme.Slate900
import com.quatari.app.ui.theme.Slate950
import com.quatari.app.ui.triage.BankBadge
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val selectedBank by viewModel.selectedBankFilter.collectAsState()
    val selectedTriage by viewModel.selectedTriageFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showManualDialog by viewModel.showManualDialog.collectAsState()
    val selectedDetailTx by viewModel.selectedTransaction.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // SAF launchers for CSV export and SQLite backup
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv(context, uri)
        }
    }

    val dbExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LedgerUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is LedgerUiEvent.ShowError -> snackbarHostState.showSnackbar("Error: ${event.error}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historic Ledger",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { csvExportLauncher.launch("quatari_ledger_${System.currentTimeMillis()}.csv") }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export CSV",
                            tint = Emerald400
                        )
                    }
                    IconButton(onClick = { dbExportLauncher.launch("hisabe_backup_${System.currentTimeMillis()}.db") }) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Export DB Backup",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openManualDialog() },
                containerColor = Emerald500,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Manual Entry")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Slate950,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                placeholder = { Text("Search reference, counterparty, notes...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald500,
                    unfocusedBorderColor = Slate700,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Bank Filters Row
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedBank == null,
                        onClick = { viewModel.selectedBankFilter.value = null },
                        label = { Text("All Banks") },
                        colors = filterChipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedBank == "Telebirr",
                        onClick = { viewModel.selectedBankFilter.value = if (selectedBank == "Telebirr") null else "Telebirr" },
                        label = { Text("Telebirr") },
                        colors = filterChipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedBank == "CBE",
                        onClick = { viewModel.selectedBankFilter.value = if (selectedBank == "CBE") null else "CBE" },
                        label = { Text("CBE") },
                        colors = filterChipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedBank == "Abyssinia",
                        onClick = { viewModel.selectedBankFilter.value = if (selectedBank == "Abyssinia") null else "Abyssinia" },
                        label = { Text("Abyssinia") },
                        colors = filterChipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedBank == "Cash",
                        onClick = { viewModel.selectedBankFilter.value = if (selectedBank == "Cash") null else "Cash" },
                        label = { Text("Cash") },
                        colors = filterChipColors()
                    )
                }
            }

            // Triage Status Filters Row
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTriage == null,
                        onClick = { viewModel.selectedTriageFilter.value = null },
                        label = { Text("All Status") },
                        colors = filterChipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTriage == true,
                        onClick = { viewModel.selectedTriageFilter.value = if (selectedTriage == true) null else true },
                        label = { Text("Classified") },
                        colors = filterChipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTriage == false,
                        onClick = { viewModel.selectedTriageFilter.value = if (selectedTriage == false) null else false },
                        label = { Text("Pending") },
                        colors = filterChipColors()
                    )
                }
            }

            // Transactions List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No ledger entries match the active filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = transactions,
                        key = { it.id }
                    ) { tx ->
                        LedgerItemCard(
                            transaction = tx,
                            onClick = { viewModel.selectTransaction(tx.id) }
                        )
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualTransactionDialog(
            accounts = accounts,
            onConfirm = { bank, ref, amount, date, notes, debitAcc, creditAcc ->
                viewModel.createManualTransaction(bank, ref, amount, date, notes, debitAcc, creditAcc)
            },
            onDismiss = { viewModel.dismissManualDialog() }
        )
    }

    selectedDetailTx?.let { detail ->
        TransactionDetailDialog(
            item = detail,
            accounts = accounts,
            onDismiss = { viewModel.dismissDetailDialog() }
        )
    }
}

@Composable
fun LedgerItemCard(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BankBadge(bankName = transaction.bankName)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ref: ${transaction.referenceNumber.take(12)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = transaction.notes ?: "Transaction",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1
                )

                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(transaction.occurredAt))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate700,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = AmountUtils.formatSantimToEtb(transaction.rawAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (transaction.isTriaged) Emerald500.copy(alpha = 0.15f) else Rose500.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (transaction.isTriaged) "BALANCED" else "PENDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.isTriaged) Emerald400 else Rose500
                    )
                }
            }
        }
    }
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Slate900,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = Emerald500,
    selectedLabelColor = Color.White
)
