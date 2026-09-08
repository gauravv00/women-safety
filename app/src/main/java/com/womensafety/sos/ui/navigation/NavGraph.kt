package com.womensafety.sos.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.womensafety.sos.ui.screens.checkin.SafetyCheckInScreen
import com.womensafety.sos.ui.screens.contacts.TrustedContactsScreen
import com.womensafety.sos.ui.screens.fakecall.FakeCallScreen
import com.womensafety.sos.ui.screens.home.HomeScreen
import com.womensafety.sos.ui.screens.logs.IncidentLogsScreen
import com.womensafety.sos.ui.screens.map.LiveTrackingScreen
import com.womensafety.sos.ui.screens.settings.SettingsScreen
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkSurface

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun MainNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home.route, "SOS", Icons.Default.Shield),
        BottomNavItem(Screen.Contacts.route, "Contacts", Icons.Default.People),
        BottomNavItem(Screen.Map.route, "Tracking", Icons.Default.LocationOn),
        BottomNavItem(Screen.Logs.route, "History", Icons.Default.History),
        BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on Full-screen overlay modes (like Fake Call)
    val showBottomBar = currentRoute != Screen.FakeCall.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                            label = { Text(text = item.title) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AlertRed,
                                selectedTextColor = AlertRed,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            ),
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToMap = { navController.navigate(Screen.Map.route) },
                    onNavigateToFakeCall = { navController.navigate(Screen.FakeCall.route) },
                    onNavigateToCheckIn = { navController.navigate(Screen.CheckIn.route) }
                )
            }

            composable(Screen.Contacts.route) {
                TrustedContactsScreen()
            }

            composable(Screen.Map.route) {
                LiveTrackingScreen()
            }

            composable(Screen.FakeCall.route) {
                FakeCallScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Screen.CheckIn.route) {
                SafetyCheckInScreen(
                    onTimerTriggeredSos = { navController.navigate(Screen.Home.route) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(Screen.Logs.route) {
                IncidentLogsScreen()
            }
        }
    }
}
