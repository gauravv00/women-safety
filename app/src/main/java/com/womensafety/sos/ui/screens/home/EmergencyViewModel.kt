package com.womensafety.sos.ui.screens.home

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.sos.data.pref.UserPreferencesRepository
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.domain.repository.EmergencyRepository
import com.womensafety.sos.service.EmergencyForegroundService
import com.womensafety.sos.service.EmergencySosService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmergencyViewModel(
    private val emergencyRepository: EmergencyRepository = ServiceLocator.emergencyRepository,
    private val userPreferences: UserPreferencesRepository = ServiceLocator.userPreferences
) : ViewModel() {

    private val _emergencyId = MutableStateFlow("")
    val emergencyId: StateFlow<String> = _emergencyId.asStateFlow()

    val isEmergencyActive: StateFlow<Boolean> = _emergencyId
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Sync active emergency if already running (e.g. from ShakeDetector or background service)
        viewModelScope.launch {
            emergencyRepository.observeActiveEmergencies().collect { list ->
                if (list.isNotEmpty() && _emergencyId.value.isBlank()) {
                    _emergencyId.value = list.first().id
                }
            }
        }
    }

    fun startEmergency(context: Context, wardName: String = "") {
        viewModelScope.launch {
            try {
                val resolvedName = wardName.ifBlank { userPreferences.settings.value.displayName }.ifBlank { "Ward" }
                val res = emergencyRepository.startEmergency(resolvedName)
                if (res.isSuccess) {
                    val id = res.getOrThrow()
                    _emergencyId.value = id
                    EmergencySosService.start(context, id)
                } else {
                    Log.e(TAG, "Failed to start emergency", res.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting emergency", e)
            }
        }
    }

    fun stopEmergency(context: Context) {
        viewModelScope.launch {
            try {
                var currentId = _emergencyId.value
                if (currentId.isBlank()) {
                    currentId = emergencyRepository.observeActiveEmergencies().firstOrNull()?.firstOrNull()?.id ?: ""
                }
                if (currentId.isNotBlank()) {
                    emergencyRepository.resolveEmergency(currentId)
                }
                EmergencySosService.stop(context)

                // Stop EmergencyForegroundService (stops siren, location updates, and RTDB broadcast)
                val serviceIntent = Intent(context, EmergencyForegroundService::class.java).apply {
                    action = EmergencyForegroundService.ACTION_STOP_SOS
                }
                context.startService(serviceIntent)

                // Resolve local Room incident & notify RTDB guardians of RESOLVED status
                val activeIncident = ServiceLocator.repository.getActiveIncidentSync()
                if (activeIncident != null) {
                    ServiceLocator.repository.resolveIncident(activeIncident.id)
                    val settings = userPreferences.settings.value
                    ServiceLocator.repository.broadcastSosToGuardians(
                        myPairingCode = settings.myPairingCode,
                        myName = settings.displayName,
                        incidentId = activeIncident.id,
                        lat = activeIncident.latitude,
                        lng = activeIncident.longitude,
                        status = "RESOLVED"
                    )
                }

                _emergencyId.value = ""
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping emergency", e)
            }
        }
    }

    fun setEmergencyId(id: String) {
        _emergencyId.value = id
    }

    companion object {
        private const val TAG = "EmergencyViewModel"
    }
}
