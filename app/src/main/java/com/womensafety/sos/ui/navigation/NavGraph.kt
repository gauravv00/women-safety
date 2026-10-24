package com.womensafety.sos.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.womensafety.sos.ui.screens.checkin.SafetyCheckInScreen
import com.womensafety.sos.ui.screens.contacts.TrustedContactsScreen
import com.womensafety.sos.ui.screens.fakecall.FakeCallScreen
import com.womensafety.sos.ui.screens.home.HomeScreen
import com.womensafety.sos.ui.screens.logs.IncidentLogsScreen
import com.womensafety.sos.ui.screens.map.LiveTrackingScreen
import com.womensafety.sos.ui.screens.notifications.NotificationsScreen
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

    // Hide navigation chrome on Full-screen overlay modes (like Fake Call)
    val showNav = currentRoute != Screen.FakeCall.route

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = configuration.screenWidthDp >= 600
    val useNavRail = (isWideScreen || isLandscape) && showNav

    fun onNavigateToItem(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    if (useNavRail) {
        // Material 3 Responsive Navigation Rail for Tablets and Landscape Phones
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = DarkSurface,
                contentColor = Color.White,
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationRailItem(
                        icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                        label = { Text(text = item.title, fontSize = 11.sp) },
                        selected = isSelected,
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = AlertRed,
                            selectedTextColor = AlertRed,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        ),
                        onClick = { onNavigateToItem(item.route) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                NavHostContainer(navController = navController)
            }
        }
    } else {
        // Standard Portrait Mobile Navigation Bar
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showNav) {
                    NavigationBar(
                        containerColor = DarkSurface,
                        windowInsets = NavigationBarDefaults.windowInsets
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
                                onClick = { onNavigateToItem(item.route) }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                NavHostContainer(navController = navController)
            }
        }
    }
}

@Composable
fun NavHostContainer(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier.fillMaxSize()
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToFakeCall = { navController.navigate(Screen.FakeCall.route) },
                onNavigateToCheckIn = { navController.navigate(Screen.CheckIn.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) }
            )
        }

        composable(
            route = Screen.Notifications.route,
            deepLinks = listOf(navDeepLink { uriPattern = "womensafety://notifications" })
        ) {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToTracking = { emergencyId ->
                    navController.navigate(Screen.Map.createRoute(emergencyId))
                }
            )
        }

        composable(Screen.Contacts.route) {
            TrustedContactsScreen(
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Map.route) {
            LiveTrackingScreen(
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.Map.routeWithArgs,
            arguments = listOf(
                navArgument("emergencyId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            ),
            deepLinks = listOf(navDeepLink { uriPattern = "womensafety://tracking/{emergencyId}" })
        ) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            LiveTrackingScreen(
                emergencyId = emergencyId,
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.FakeCall.route) {
            FakeCallScreen(
                onDismiss = { navController.popBackStack() }
            )
        }

        composable(Screen.CheckIn.route) {
            SafetyCheckInScreen(
                onBackClick = { navController.popBackStack() },
                onTimerTriggeredSos = { navController.navigate(Screen.Home.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Logs.route) {
            IncidentLogsScreen(
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Home.route)
                    }
                }
            )
        }
    }
}
