package com.quatari.app.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quatari.app.data.local.entity.AccountEntity
import com.quatari.app.domain.util.AmountUtils
import com.quatari.app.ui.theme.Emerald500
import com.quatari.app.ui.theme.Slate700
import com.quatari.app.ui.theme.Slate800
import com.quatari.app.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTransactionDialog(
    accounts: List<AccountEntity>,
    onConfirm: (
        bank: String,
        ref: String,
        amountSantim: Long,
        occurredAt: Long,
        notes: String?,
        debitAccountId: String,
        creditAccountId: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var sourceName by remember { mutableStateOf("Cash") }
    var reference by remember { mutableStateOf("MANUAL-${System.currentTimeMillis() % 100000}") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var selectedDebitAccount by remember {
        mutableStateOf(accounts.firstOrNull { it.id == "expense:personal" } ?: accounts.firstOrNull())
    }
    var selectedCreditAccount by remember {
        mutableStateOf(accounts.firstOrNull { it.id == "asset:cash:wallet" } ?: accounts.firstOrNull())
    }

    var debitExpanded by remember { mutableStateOf(false) }
    var creditExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "Manual Cash Flow Entry",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = sourceName,
                    onValueChange = { sourceName = it },
                    label = { Text("Source / Bank") },
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (ETB)") },
                    placeholder = { Text("e.g. 150.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Debit Account Selector
                ExposedDropdownMenuBox(
                    expanded = debitExpanded,
                    onExpandedChange = { debitExpanded = !debitExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDebitAccount?.name ?: "Select Debit Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Debit Account (+ Asset/Expense)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = debitExpanded) },
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = debitExpanded,
                        onDismissRequest = { debitExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.name} (${account.type.name})") },
                                onClick = {
                                    selectedDebitAccount = account
                                    debitExpanded = false
                                }
                            )
                        }
                    }
                }

                // Credit Account Selector
                ExposedDropdownMenuBox(
                    expanded = creditExpanded,
                    onExpandedChange = { creditExpanded = !creditExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCreditAccount?.name ?: "Select Credit Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Credit Account (- Asset / + Revenue)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = creditExpanded) },
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = creditExpanded,
                        onDismissRequest = { creditExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.name} (${account.type.name})") },
                                onClick = {
                                    selectedCreditAccount = account
                                    creditExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Description") },
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val santim = AmountUtils.parseEtbToSantim(amountText)
                    if (santim > 0 && selectedDebitAccount != null && selectedCreditAccount != null) {
                        onConfirm(
                            sourceName,
                            reference,
                            santim,
                            System.currentTimeMillis(),
                            notes.ifBlank { null },
                            selectedDebitAccount!!.id,
                            selectedCreditAccount!!.id
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                enabled = amountText.isNotBlank() && selectedDebitAccount != null && selectedCreditAccount != null
            ) {
                Text("Record Entry", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Emerald500,
    unfocusedBorderColor = Slate700,
    focusedLabelColor = Emerald500,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
