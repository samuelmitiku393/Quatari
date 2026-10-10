package com.quatari.app.ui.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.quatari.app.ui.theme.Emerald400
import com.quatari.app.ui.theme.Rose500
import com.quatari.app.ui.theme.Slate700
import com.quatari.app.ui.theme.Slate800
import com.quatari.app.ui.theme.Slate900

@Composable
fun BottomNavBar(
    currentRoute: String?,
    pendingTriageCount: Int,
    onNavigate: (Screen) -> Unit
) {
    val items = listOf(
        Screen.Triage,
        Screen.Dashboard,
        Screen.Ledger
    )

    NavigationBar(
        containerColor = Slate900,
        contentColor = Color.White
    ) {
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = {
                    if (screen == Screen.Triage && pendingTriageCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Rose500,
                                    contentColor = Color.White
                                ) {
                                    Text(text = pendingTriageCount.toString(), fontSize = 10.sp)
                                }
                            }
                        ) {
                            Icon(imageVector = screen.icon, contentDescription = screen.title)
                        }
                    } else {
                        Icon(imageVector = screen.icon, contentDescription = screen.title)
                    }
                },
                label = { Text(screen.title, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Emerald400,
                    selectedTextColor = Emerald400,
                    unselectedIconColor = Slate700,
                    unselectedTextColor = Slate700,
                    indicatorColor = Slate800
                )
            )
        }
    }
}
