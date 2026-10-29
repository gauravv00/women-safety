package com.womensafety.sos.ui.screens.home

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.sos.data.entity.IncidentLog
import com.womensafety.sos.data.entity.TrustedContact
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.domain.repository.SafetyRepository
import com.womensafety.sos.service.EmergencyForegroundService
import com.womensafety.sos.service.EmergencySosService
import com.womensafety.sos.service.ShakeDetector
import com.womensafety.sos.service.SosTriggerHelper
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: SafetyRepository = ServiceLocator.repository
) : ViewModel() {

    val userName: StateFlow<String> = ServiceLocator.userPreferences.settings
        .map { settings ->
            val prefName = settings.displayName
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val firebaseName = firebaseUser?.displayName
            when {
                prefName.isNotBlank() && prefName != "My Phone" && prefName != "User" -> prefName
                !firebaseName.isNullOrBlank() -> firebaseName
                prefName.isNotBlank() -> prefName
                else -> "User"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")

    val activeIncident: StateFlow<IncidentLog?> = repository.getActiveIncident()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trustedContacts: StateFlow<List<TrustedContact>> = repository.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isServiceRunning: StateFlow<Boolean> = EmergencyForegroundService.isServiceRunning

    private val _showCallPanel = MutableStateFlow(false)
    val showCallPanel: StateFlow<Boolean> = _showCallPanel.asStateFlow()

    private var shakeDetector: ShakeDetector? = null
    private var sensorManager: SensorManager? = null

    fun triggerSos(context: Context, triggerSource: String = "MANUAL_HOLD") {
        SosTriggerHelper.triggerEmergency(context, triggerSource)
    }

    fun cancelSos(context: Context) {
        viewModelScope.launch {
            val active = activeIncident.value
            val settings = ServiceLocator.userPreferences.settings.value

            if (active != null) {
                repository.resolveIncident(active.id)
                repository.broadcastSosToGuardians(
                    myPairingCode = settings.myPairingCode,
                    myName = settings.displayName,
                    incidentId = active.id,
                    lat = active.latitude,
                    lng = active.longitude,
                    status = "RESOLVED"
                )
            }
            try {
                EmergencySosService.stop(context)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed stopping EmergencySosService on cancel", e)
            }
            val serviceIntent = Intent(context, EmergencyForegroundService::class.java).apply {
                action = EmergencyForegroundService.ACTION_STOP_SOS
            }
            context.startService(serviceIntent)
        }
    }

    fun setShowCallPanel(show: Boolean) {
        _showCallPanel.value = show
    }

    fun registerShakeDetector(context: Context) {
        val settings = ServiceLocator.userPreferences.settings.value
        if (!settings.shakeToTriggerEnabled) return

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        shakeDetector = ShakeDetector {
            if (activeIncident.value == null && !EmergencyForegroundService.isServiceRunning.value) {
                triggerSos(context, "SHAKE")
            }
        }
        accelerometer?.let {
            sensorManager?.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun unregisterShakeDetector() {
        shakeDetector?.let { sensorManager?.unregisterListener(it) }
        shakeDetector = null
    }
}
