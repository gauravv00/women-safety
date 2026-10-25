package com.womensafety.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.service.GuardianMonitorService
import com.womensafety.sos.ui.navigation.MainNavGraph
import com.womensafety.sos.ui.theme.WomenSafetyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // Initialize guardian background monitor if enabled
        val settings = ServiceLocator.userPreferences.settings.value
        if (settings.guardianMonitoringEnabled) {
            GuardianMonitorService.start(this)
        }

        setContent {
            WomenSafetyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavGraph()
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
            if (!com.womensafety.sos.service.EmergencyForegroundService.isServiceRunning.value) {
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