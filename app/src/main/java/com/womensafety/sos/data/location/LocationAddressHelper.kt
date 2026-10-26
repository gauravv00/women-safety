package com.womensafety.sos.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object LocationAddressHelper {

    private val addressCache = LruCache<String, String>(60)

    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = withContext(Dispatchers.IO) {
        if (latitude == 0.0 && longitude == 0.0) {
            return@withContext "Location unavailable"
        }

        val cacheKey = String.format(Locale.US, "%.3f,%.3f", latitude, longitude)
        val cached = addressCache.get(cacheKey)
        if (cached != null) {
            return@withContext cached
        }

        val fallback = String.format(Locale.US, "Lat: %.4f, Lng: %.4f", latitude, longitude)

        try {
            if (!Geocoder.isPresent()) {
                return@withContext fallback
            }

            val geocoder = Geocoder(context, Locale.getDefault())

            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val addr = addresses.firstOrNull()
                            cont.resume(formatAddress(addr, latitude, longitude))
                        }

                        override fun onError(errorMessage: String?) {
                            cont.resume(fallback)
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val addr = addresses?.firstOrNull()
                formatAddress(addr, latitude, longitude)
            }

            addressCache.put(cacheKey, resolved)
            resolved
        } catch (_: Exception) {
            fallback
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
            String.format(Locale.US, "Lat: %.4f, Lng: %.4f", latitude, longitude)
        }
    }
}
