package com.womensafety.sos.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "SOS")
    object Contacts : Screen("contacts", "Contacts")
    object Map : Screen("map", "Map")
    object FakeCall : Screen("fake_call", "Fake Call")
    object CheckIn : Screen("check_in", "Check-In")
    object Settings : Screen("settings", "Settings")
    object Logs : Screen("logs", "History")
}
