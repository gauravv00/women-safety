package com.womensafety.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.service.EmergencyForegroundService
import com.womensafety.sos.service.GuardianMonitorService
import com.womensafety.sos.ui.navigation.MainNavGraph
import com.womensafety.sos.ui.screens.login.LoginPreferences
import com.womensafety.sos.ui.screens.login.LoginScreen
import com.womensafety.sos.ui.theme.WomenSafetyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // Initialize guardian background monitor if enabled
        val initSettings = ServiceLocator.userPreferences.settings.value
        if (initSettings.guardianMonitoringEnabled) {
            GuardianMonitorService.start(this)
        }

        setContent {
            val settings by ServiceLocator.userPreferences.settings.collectAsState()
            WomenSafetyTheme(darkTheme = settings.darkModeEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var loginCompleted by remember {
                        mutableStateOf(LoginPreferences.isLoginCompleted(this@MainActivity))
                    }

                    if (!loginCompleted) {
                        LoginScreen(
                            onLoginSuccess = {
                                loginCompleted = true
                            }
                        )
                    } else {
                        MainNavGraph(
                            onLogout = {
                                loginCompleted = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        cancelNonEmergencyVibration()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelNonEmergencyVibration()
    }

    private fun cancelNonEmergencyVibration() {
        try {
            if (!EmergencyForegroundService.isServiceRunning.value) {
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    (getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                }
                vibrator?.cancel()
            }
        } catch (_: Exception) {}
    }
}
