package com.womensafety.sos.ui.screens.map

import android.location.Location
import androidx.lifecycle.ViewModel
import com.womensafety.sos.service.EmergencyForegroundService
import kotlinx.coroutines.flow.StateFlow

class LiveTrackingViewModel : ViewModel() {
    val currentLocation: StateFlow<Location?> = EmergencyForegroundService.currentLocation
    val locationHistory: StateFlow<List<Pair<Double, Double>>> = EmergencyForegroundService.locationHistory
    val isServiceRunning: StateFlow<Boolean> = EmergencyForegroundService.isServiceRunning
}
