package com.quatari.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Triage : Screen("triage", "Triage Inbox", Icons.Default.Inbox)
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.BarChart)
    data object Ledger : Screen("ledger", "Ledger", Icons.Default.AccountBalanceWallet)
}
