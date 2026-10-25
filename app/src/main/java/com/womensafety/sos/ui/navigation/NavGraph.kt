package com.womensafety.sos.ui.navigation

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.womensafety.sos.ui.adaptive.WindowInfo
import com.womensafety.sos.ui.adaptive.rememberWindowInfo
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import com.womensafety.sos.ui.theme.DarkCardBorder
import com.womensafety.sos.ui.theme.TrustBlue


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

    // Window width < 600dp -> BottomBar (all phones in portrait). Width >= 600dp -> NavigationRail.
    val win = rememberWindowInfo()

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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (win.useBottomBar && showNav) {
                AppBottomBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onNavigate = ::onNavigateToItem
                )
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (win.useRail && showNav) {
                AppRail(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onNavigate = ::onNavigateToItem
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                NavHostContainer(navController = navController)
            }
        }
    }
}

@Composable
fun AppBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        NavigationBar(
            modifier = Modifier.height(68.dp),
            containerColor = DarkSurface,
            windowInsets = NavigationBarDefaults.windowInsets
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val activeColor = if (item.route == Screen.Home.route) AlertRed else TrustBlue
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    selected = isSelected,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = activeColor.copy(alpha = 0.15f)
                    ),
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
fun AppRail(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        containerColor = DarkSurface,
        contentColor = Color.White,
        modifier = modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationRailItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = isSelected,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = AlertRed,
                    selectedTextColor = AlertRed,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                ),
                onClick = { onNavigate(item.route) }
            )
        }
    }
}

@Preview(name = "1. Phone Portrait 360x800", widthDp = 360, heightDp = 800)
@Composable
private fun PreviewPhonePortrait360x800() {
    NavPreviewWrapper()
}

@Preview(name = "2. Small Phone Portrait 320x568", widthDp = 320, heightDp = 568)
@Composable
private fun PreviewSmallPhonePortrait320x568() {
    NavPreviewWrapper()
}

@Preview(name = "3. Large Phone 412x915 Font 1.3", widthDp = 412, heightDp = 915, fontScale = 1.3f)
@Composable
private fun PreviewLargePhonePortrait412x915() {
    NavPreviewWrapper()
}

@Preview(name = "4. Phone Display Large 320x640", widthDp = 320, heightDp = 640)
@Composable
private fun PreviewPhoneDisplayLarge() {
    NavPreviewWrapper()
}

@Preview(name = "5. Phone Landscape 800x360", widthDp = 800, heightDp = 360)
@Composable
private fun PreviewPhoneLandscape800x360() {
    NavPreviewWrapper()
}

@Preview(name = "6. Tablet Portrait 800x1280", widthDp = 800, heightDp = 1280)
@Composable
private fun PreviewTabletPortrait800x1280() {
    NavPreviewWrapper()
}

@Composable
private fun NavPreviewWrapper() {
    val win = rememberWindowInfo()
    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home.route, "SOS", Icons.Default.Shield),
        BottomNavItem(Screen.Contacts.route, "Contacts", Icons.Default.People),
        BottomNavItem(Screen.Map.route, "Tracking", Icons.Default.LocationOn),
        BottomNavItem(Screen.Logs.route, "History", Icons.Default.History),
        BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
    )
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (win.useBottomBar) {
                AppBottomBar(
                    items = bottomNavItems,
                    currentRoute = Screen.Home.route,
                    onNavigate = {}
                )
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (win.useRail) {
                AppRail(
                    items = bottomNavItems,
                    currentRoute = Screen.Home.route,
                    onNavigate = {}
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (win.useBottomBar) "Phone mode: BottomBar" else "Wide mode: Rail",
                    color = Color.White
                )
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
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                onNavigateToTracking = { emergencyId ->
                    if (emergencyId.isNotBlank()) {
                        navController.navigate(Screen.Map.createRoute(emergencyId))
                    } else {
                        navController.navigate(Screen.Map.route)
                    }
                }
            )
        }

        composable(
            route = Screen.Notifications.route,
            deepLinks = listOf(navDeepLink { uriPattern = "womensafety://notifications" })
        ) {
            NotificationsScreen(
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Home.route)
                    }
                },
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
                onDismiss = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.CheckIn.route) {
            SafetyCheckInScreen(
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Home.route)
                    }
                },
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
