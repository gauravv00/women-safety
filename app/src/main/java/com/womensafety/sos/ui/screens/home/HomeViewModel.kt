package com.womensafety.sos.ui.screens.home

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.sos.data.entity.IncidentLog
import com.womensafety.sos.data.entity.TrustedContact
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.domain.repository.SafetyRepository
import com.womensafety.sos.service.EmergencyForegroundService
import com.womensafety.sos.service.ShakeDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: SafetyRepository = ServiceLocator.repository
) : ViewModel() {

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
        viewModelScope.launch {
            val location = EmergencyForegroundService.currentLocation.value
            val lat = location?.latitude ?: 0.0
            val lng = location?.longitude ?: 0.0

            val incidentId = repository.createIncident(lat, lng, triggerSource)
            repository.sendEmergencySmsToContacts(lat, lng, incidentId)

            val serviceIntent = Intent(context, EmergencyForegroundService::class.java).apply {
                action = EmergencyForegroundService.ACTION_START_SOS
                putExtra(EmergencyForegroundService.EXTRA_INCIDENT_ID, incidentId)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    fun cancelSos(context: Context) {
        viewModelScope.launch {
            val active = activeIncident.value
            if (active != null) {
                repository.resolveIncident(active.id)
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

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        shakeDetector = ShakeDetector {
            if (activeIncident.value == null) {
                triggerSos(context, "SHAKE")
            }
        }
        sensorManager?.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun unregisterShakeDetector() {
        shakeDetector?.let { sensorManager?.unregisterListener(it) }
    }
}
