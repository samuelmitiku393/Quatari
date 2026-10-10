package com.quatari.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.quatari.app.ui.dashboard.DashboardScreen
import com.quatari.app.ui.dashboard.DashboardViewModel
import com.quatari.app.ui.ledger.LedgerScreen
import com.quatari.app.ui.ledger.LedgerViewModel
import com.quatari.app.ui.navigation.BottomNavBar
import com.quatari.app.ui.navigation.Screen
import com.quatari.app.ui.theme.QuatariTheme
import com.quatari.app.ui.theme.Slate950
import com.quatari.app.ui.triage.TriageScreen
import com.quatari.app.ui.triage.TriageViewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled; app continues locally regardless
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request SMS permissions for local interception (side-loaded app)
        checkAndRequestSmsPermissions()

        val app = application as QuatariApp
        val repository = app.repository

        setContent {
            QuatariTheme(darkTheme = true) {
                MainAppScreen(repository = repository)
            }
        }
    }

    private fun checkAndRequestSmsPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}

@Composable
fun MainAppScreen(repository: com.quatari.app.data.repository.DoubleEntryLedgerRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val triageViewModel: TriageViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = TriageViewModel.Factory(repository)
    )
    val dashboardViewModel: DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = DashboardViewModel.Factory(repository)
    )
    val ledgerViewModel: LedgerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = LedgerViewModel.Factory(repository)
    )

    val untriagedList by triageViewModel.untriagedTransactions.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                pendingTriageCount = untriagedList.size,
                onNavigate = { screen ->
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Triage.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        },
        containerColor = Slate950,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Triage.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Triage.route) {
                TriageScreen(viewModel = triageViewModel)
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel = dashboardViewModel)
            }
            composable(Screen.Ledger.route) {
                LedgerScreen(viewModel = ledgerViewModel)
            }
        }
    }
}
