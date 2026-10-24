package com.womensafety.sos.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class LocationTracker(private val context: Context) {

    private val TAG = "LocationTracker"
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _lastKnownLocation = MutableStateFlow<Location?>(null)
    val lastKnownLocation: StateFlow<Location?> = _lastKnownLocation.asStateFlow()

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return null
        }
        return try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                _lastKnownLocation.value = location
            }
            location
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last location: ${e.localizedMessage}")
            null
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(timeoutMs: Long = 4000L): Location? {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        return try {
            withTimeoutOrNull(timeoutMs) {
                val cancellationTokenSource = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()

                if (location != null) {
                    _lastKnownLocation.value = location
                }
                location
            } ?: getLastKnownLocation()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location: ${e.localizedMessage}")
            getLastKnownLocation()
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(
        intervalMs: Long = 3000L,
        minIntervalMs: Long = 1500L
    ): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission missing"))
            return@callbackFlow
        }

        // Emit cached last location immediately if available
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    _lastKnownLocation.value = loc
                    trySend(loc)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed emitting last location on flow start: ${e.message}")
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(minIntervalMs)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    _lastKnownLocation.value = location
                    trySend(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e(TAG, "requestLocationUpdates failed: ${e.localizedMessage}")
            close(e)
            return@callbackFlow
        }

        awaitClose {
            try {
                fusedLocationClient.removeLocationUpdates(callback)
            } catch (e: Exception) {
                Log.w(TAG, "removeLocationUpdates error: ${e.localizedMessage}")
            }
        }
    }

    fun updateCache(location: Location) {
        _lastKnownLocation.value = location
    }
}
