package com.womensafety.sos.ui.screens.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.womensafety.sos.data.entity.PairedWard
import com.womensafety.sos.data.entity.TrustedContact
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.service.EmergencyForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LiveTrackingViewModel : ViewModel() {
    val currentLocation: StateFlow<Location?> = EmergencyForegroundService.currentLocation
    val isServiceRunning: StateFlow<Boolean> = EmergencyForegroundService.isServiceRunning

    val contacts: StateFlow<List<TrustedContact>> =
        ServiceLocator.repository.getAllContacts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pairedWards: StateFlow<List<PairedWard>> =
        ServiceLocator.repository.getAllPairedWards()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected tracking target: null = "My Location", or wardPairingCode
    private val _selectedTargetCode = MutableStateFlow<String?>(null)
    val selectedTargetCode: StateFlow<String?> = _selectedTargetCode.asStateFlow()

    private val _isBroadcastingLocation = MutableStateFlow(false)
    val isBroadcastingLocation: StateFlow<Boolean> = _isBroadcastingLocation.asStateFlow()

    fun selectTarget(code: String?) {
        _selectedTargetCode.value = code
    }

    /**
     * Distinct location history to prevent the UI from processing
     * every intermediate list mutation. Only emits when the list size changes.
     */
    val locationHistory: StateFlow<List<Pair<Double, Double>>> =
        EmergencyForegroundService.locationHistory
            .distinctUntilChanged { old, new -> old.size == new.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Debounced location that only emits when distance from last emitted
     * point is >= 5 meters. Reduces map recomposition on GPS jitter.
     */
    val debouncedLocation: StateFlow<Location?> =
        EmergencyForegroundService.currentLocation
            .distinctUntilChanged { old, new ->
                if (old == null || new == null) false
                else old.distanceTo(new) < 5f
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @SuppressLint("MissingPermission")
    fun broadcastLocationToGuardians(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            val settings = ServiceLocator.userPreferences.settings.value
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val location = currentLocation.value ?: try {
                fusedClient.lastLocation.await()
            } catch (e: Exception) {
                null
            }

            if (location != null && settings.myPairingCode.isNotBlank()) {
                _isBroadcastingLocation.value = true
                ServiceLocator.repository.broadcastSosToGuardians(
                    myPairingCode = settings.myPairingCode,
                    myName = settings.displayName,
                    incidentId = -1L,
                    lat = location.latitude,
                    lng = location.longitude,
                    status = if (isServiceRunning.value) "ACTIVE" else "TRACKING"
                )
            }
            onComplete()
        }
    }
}
