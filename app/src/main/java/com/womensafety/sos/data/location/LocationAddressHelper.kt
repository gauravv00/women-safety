package com.womensafety.sos.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object LocationAddressHelper {

    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = withContext(Dispatchers.IO) {
        if (latitude == 0.0 && longitude == 0.0) {
            return@withContext "Location unavailable"
        }

        try {
            if (!Geocoder.isPresent()) {
                return@withContext String.format(Locale.US, "Lat: %.4f, Lng: %.4f", latitude, longitude)
            }

            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val addr = addresses.firstOrNull()
                            cont.resume(formatAddress(addr, latitude, longitude))
                        }

                        override fun onError(errorMessage: String?) {
                            cont.resume(String.format(Locale.US, "Lat: %.4f, Lng: %.4f", latitude, longitude))
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val addr = addresses?.firstOrNull()
                formatAddress(addr, latitude, longitude)
            }
        } catch (e: Exception) {
            String.format(Locale.US, "Lat: %.4f, Lng: %.4f", latitude, longitude)
        }
    }

    private fun formatAddress(address: Address?, latitude: Double, longitude: Double): String {
        if (address == null) {
            return String.format(Locale.US, "Lat: %.4f, Lng: %.4f", latitude, longitude)
        }

        val feature = address.featureName ?: address.thoroughfare
        val subLocality = address.subLocality ?: address.locality
        val city = address.locality ?: address.subAdminArea ?: address.adminArea

        val parts = listOfNotNull(feature, subLocality, city)
            .distinct()
            .filter { it.isNotBlank() }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            address.getAddressLine(0) ?: String.format(Locale.US, "Lat: %.4f, Lng: %.4f", latitude, longitude)
        }
    }
}
