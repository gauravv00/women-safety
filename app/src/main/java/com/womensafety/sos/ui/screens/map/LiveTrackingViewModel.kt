package com.womensafety.sos.ui.screens.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.sos.service.EmergencyForegroundService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

class LiveTrackingViewModel : ViewModel() {
    val currentLocation: StateFlow<Location?> = EmergencyForegroundService.currentLocation
    val isServiceRunning: StateFlow<Boolean> = EmergencyForegroundService.isServiceRunning

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
}
