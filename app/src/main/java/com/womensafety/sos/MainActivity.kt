package com.womensafety.sos

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.service.EmergencyForegroundService
import com.womensafety.sos.service.GuardianMonitorService
import com.womensafety.sos.service.ShakeDetector
import com.womensafety.sos.service.SosTriggerHelper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.womensafety.sos.ui.screens.login.LoginPreferences
import com.womensafety.sos.ui.screens.login.LoginScreen
import com.womensafety.sos.ui.navigation.MainNavGraph
import com.womensafety.sos.ui.theme.WomenSafetyTheme

class MainActivity : ComponentActivity() {

    private var sensorManager: SensorManager? = null
    private var shakeDetector: ShakeDetector? = null

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

        // Initialize app-wide Shake detection for SOS
        initShakeDetection()

        setContent {
            WomenSafetyTheme {
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
                        MainNavGraph()
                    }
                }
            }
        }
    }

    private fun initShakeDetection() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        shakeDetector = ShakeDetector {
            val settings = ServiceLocator.userPreferences.settings.value
            if (settings.shakeToTriggerEnabled && !EmergencyForegroundService.isServiceRunning.value) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Shake detected! Broadcasting Emergency SOS...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                SosTriggerHelper.triggerEmergency(this@MainActivity, "SHAKE")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerShakeDetectorIfEnabled()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector?.let { sensorManager?.unregisterListener(it) }
    }

    override fun onStop() {
        super.onStop()
        cancelNonEmergencyVibration()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelNonEmergencyVibration()
    }

    private fun registerShakeDetectorIfEnabled() {
        val settings = ServiceLocator.userPreferences.settings.value
        if (settings.shakeToTriggerEnabled) {
            val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer?.let {
                shakeDetector?.reset()
                sensorManager?.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
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
