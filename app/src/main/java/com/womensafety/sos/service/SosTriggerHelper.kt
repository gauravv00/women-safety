package com.womensafety.sos.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.womensafety.sos.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Centralized SOS trigger coordinator that handles both local emergency dispatch
 * (Room incident, SMS, foreground service with siren/audio) and Firebase Firestore
 * real-time emergency broadcast for guardian live tracking.
 */
object SosTriggerHelper {
    private const val TAG = "SosTriggerHelper"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("MissingPermission")
    fun triggerEmergency(
        context: Context,
        triggerSource: String = "MANUAL_HOLD",
        onComplete: (() -> Unit)? = null
    ) {
        val appContext = context.applicationContext

        // Haptic feedback for tactile user confirmation
        vibrateConfirmation(appContext)

        scope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
                val location = EmergencyForegroundService.currentLocation.value ?: try {
                    fusedClient.lastLocation.await()
                } catch (e: Exception) {
                    null
                }
                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0

                val repository = ServiceLocator.repository
                val incidentId = repository.createIncident(lat, lng, triggerSource)
                repository.sendEmergencySmsToContacts(lat, lng, incidentId)

                val settings = ServiceLocator.userPreferences.settings.value
                repository.broadcastSosToGuardians(
                    myPairingCode = settings.myPairingCode,
                    myName = settings.displayName,
                    incidentId = incidentId,
                    lat = lat,
                    lng = lng,
                    status = "ACTIVE"
                )

                // Also initiate real-time Firestore emergency session for guardian live tracking
                try {
                    val wardName = settings.displayName.ifBlank { "Ward" }
                    val res = ServiceLocator.emergencyRepository.startEmergency(wardName)
                    if (res.isSuccess) {
                        val emergencyId = res.getOrThrow()
                        EmergencyForegroundService.activeEmergencySessionId = emergencyId
                        EmergencySosService.start(appContext, emergencyId)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start live emergency repository session", e)
                }

                val serviceIntent = Intent(appContext, EmergencyForegroundService::class.java).apply {
                    action = EmergencyForegroundService.ACTION_START_SOS
                    putExtra(EmergencyForegroundService.EXTRA_INCIDENT_ID, incidentId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(serviceIntent)
                } else {
                    appContext.startService(serviceIntent)
                }

                onComplete?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering emergency SOS from source: $triggerSource", e)
            }
        }
    }

    private fun vibrateConfirmation(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Distinct emergency buzz confirmation: (0, 300ms, 150ms, 400ms)
                    val timings = longArrayOf(0, 300, 150, 400)
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(longArrayOf(0, 300, 150, 400), -1)
                }
            }
        } catch (_: Exception) {}
    }
}
