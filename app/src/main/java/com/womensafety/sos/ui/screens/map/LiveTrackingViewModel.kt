package com.womensafety.sos.ui.screens.map

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.sos.data.firebase.FirebaseAuthManager
import com.womensafety.sos.data.location.LocationAddressHelper
import com.womensafety.sos.data.location.LocationTracker
import com.womensafety.sos.data.model.EmergencyEvent
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.domain.repository.EmergencyRepository
import com.womensafety.sos.service.EmergencySosService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveTrackingViewModel(
    private val emergencyRepository: EmergencyRepository = ServiceLocator.emergencyRepository,
    private val authManager: FirebaseAuthManager = ServiceLocator.authManager,
    private val locationTracker: LocationTracker = ServiceLocator.locationTracker
) : ViewModel() {

    private val _emergency = MutableStateFlow<EmergencyEvent?>(null)
    val emergency: StateFlow<EmergencyEvent?> = _emergency.asStateFlow()

    private val _activeEmergencies = MutableStateFlow<List<EmergencyEvent>>(emptyList())
    val activeEmergencies: StateFlow<List<EmergencyEvent>> = _activeEmergencies.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _resolvedAddress = MutableStateFlow<String>("")
    val resolvedAddress: StateFlow<String> = _resolvedAddress.asStateFlow()

    private val _isRefreshingLocation = MutableStateFlow(false)
    val isRefreshingLocation: StateFlow<Boolean> = _isRefreshingLocation.asStateFlow()

    val currentUserId: String
        get() = authManager.currentUserId

    private var observeEmergencyJob: Job? = null
    private var locationUpdatesJob: Job? = null
    private var addressJob: Job? = null

    init {
        startLocationUpdates()
    }

    fun startLocationUpdates() {
        locationUpdatesJob?.cancel()
        locationUpdatesJob = viewModelScope.launch {
            // Immediately populate last known location so the map doesn't wait
            val lastLoc = locationTracker.getLastKnownLocation()
            if (lastLoc != null && _currentLocation.value == null) {
                _currentLocation.value = lastLoc
            }

            try {
                locationTracker.getLocationUpdates(intervalMs = 3000L).collect { loc ->
                    _currentLocation.value = loc
                }
            } catch (e: Exception) {
                Log.w(TAG, "Location updates stream paused/failed: ${e.message}")
                val loc = locationTracker.getLastKnownLocation() ?: locationTracker.getCurrentLocation()
                if (loc != null) _currentLocation.value = loc
            }
        }
    }

    fun loadEmergency(emergencyId: String) {
        if (emergencyId.isNotBlank()) {
            observeEmergencyJob?.cancel()
            observeEmergencyJob = viewModelScope.launch {
                emergencyRepository.observeEmergency(emergencyId).collect { event ->
                    _emergency.value = event
                }
            }
        } else {
            // Observe all active emergencies for wards
            observeEmergencyJob?.cancel()
            observeEmergencyJob = viewModelScope.launch {
                emergencyRepository.observeActiveEmergencies().collect { list ->
                    _activeEmergencies.value = list
                    if (_emergency.value == null && list.isNotEmpty()) {
                        _emergency.value = list.first()
                    }
                }
            }
        }

        // Acquire local location as fallback
        viewModelScope.launch {
            val loc = locationTracker.getLastKnownLocation() ?: locationTracker.getCurrentLocation()
            if (loc != null) {
                _currentLocation.value = loc
            }
        }
    }

    fun selectEmergency(event: EmergencyEvent) {
        _emergency.value = event
    }

    fun resolveEmergency(context: Context, emergencyId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                emergencyRepository.resolveEmergency(emergencyId)
                EmergencySosService.stop(context)
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve emergency $emergencyId", e)
            }
        }
    }

    fun resolveAddressForCoordinates(context: Context, lat: Double, lng: Double) {
        if (lat == 0.0 && lng == 0.0) return
        addressJob?.cancel()
        addressJob = viewModelScope.launch {
            val addr = LocationAddressHelper.getAddressFromCoordinates(context, lat, lng)
            _resolvedAddress.value = addr
        }
    }

    fun refreshLocation(context: Context? = null) {
        viewModelScope.launch {
            _isRefreshingLocation.value = true
            val loc = locationTracker.getCurrentLocation(timeoutMs = 5000L)
            if (loc != null) {
                _currentLocation.value = loc
                if (context != null) {
                    val addr = LocationAddressHelper.getAddressFromCoordinates(context, loc.latitude, loc.longitude)
                    _resolvedAddress.value = addr
                }
            }
            _isRefreshingLocation.value = false
        }
    }

    companion object {
        private const val TAG = "LiveTrackingViewModel"
    }
}
