package com.womensafety.sos.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.womensafety.sos.ui.theme.DarkSurface
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

// High-contrast brutalist dark map styling for Google Maps matching the app's aesthetic
private const val DARK_MAP_STYLE_JSON = """
[
  {"elementType": "geometry", "stylers": [{"color": "#121212"}]},
  {"elementType": "labels.icon", "stylers": [{"visibility": "off"}]},
  {"elementType": "labels.text.fill", "stylers": [{"color": "#8c8c8c"}]},
  {"elementType": "labels.text.stroke", "stylers": [{"color": "#121212"}]},
  {"featureType": "administrative", "elementType": "geometry", "stylers": [{"color": "#333333"}]},
  {"featureType": "administrative.country", "elementType": "labels.text.fill", "stylers": [{"color": "#a8a8a8"}]},
  {"featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{"color": "#d0d0d0"}]},
  {"featureType": "poi", "elementType": "labels.text.fill", "stylers": [{"color": "#8c8c8c"}]},
  {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#181818"}]},
  {"featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{"color": "#616161"}]},
  {"featureType": "poi.park", "elementType": "labels.text.stroke", "stylers": [{"color": "#121212"}]},
  {"featureType": "road", "elementType": "geometry.fill", "stylers": [{"color": "#242424"}]},
  {"featureType": "road", "elementType": "labels.text.fill", "stylers": [{"color": "#9e9e9e"}]},
  {"featureType": "road.arterial", "elementType": "geometry", "stylers": [{"color": "#2e2e2e"}]},
  {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#3d3d3d"}]},
  {"featureType": "road.highway.controlled_access", "elementType": "geometry", "stylers": [{"color": "#525252"}]},
  {"featureType": "road.local", "elementType": "labels.text.fill", "stylers": [{"color": "#757575"}]},
  {"featureType": "transit", "elementType": "labels.text.fill", "stylers": [{"color": "#8c8c8c"}]},
  {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#0a0a0a"}]},
  {"featureType": "water", "elementType": "labels.text.fill", "stylers": [{"color": "#4a4a4a"}]}
]
"""

/**
 * Dedicated Google Maps component for real-time SOS tracking with high-contrast dark theme,
 * accuracy radius visualization, and camera re-centering.
 */
@Composable
fun SafetyMap(
    current: LatLng,
    accuracyMeters: Double,
    path: List<LatLng> = emptyList(),
    accentColor: Color,
    pathColor: Color = accentColor,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize MapsInitializer early to prevent CameraUpdateFactory NullPointerException
    LaunchedEffect(Unit) {
        try {
            MapsInitializer.initialize(context)
        } catch (_: Exception) {}
    }

    // Google Maps is the primary map
    var isGoogleMapsAvailable by remember { mutableStateOf(true) }
    var recenterTrigger by remember { mutableIntStateOf(0) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(current, 16.5f)
    }

    val mapProperties = remember {
        val styleOptions = try {
            MapStyleOptions(DARK_MAP_STYLE_JSON)
        } catch (e: Exception) {
            null
        }
        MapProperties(
            isMyLocationEnabled = false,
            mapStyleOptions = styleOptions
        )
    }

    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = true,
            rotationGesturesEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = false,
            zoomGesturesEnabled = true,
            mapToolbarEnabled = false
        )
    }

    val markerState = remember(current) { MarkerState(position = current) }

    var hasCenteredInitially by remember { mutableStateOf(false) }

    LaunchedEffect(current) {
        if (!isGoogleMapsAvailable) return@LaunchedEffect
        try {
            if (!hasCenteredInitially && current.latitude != 0.0 && current.longitude != 0.0) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(current, 16.5f)
                hasCenteredInitially = true
            } else if (hasCenteredInitially && !cameraPositionState.isMoving) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLng(current),
                    durationMs = 600
                )
            }
        } catch (_: Exception) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(current, 16.5f)
        }
    }

    Box(modifier = modifier) {
        if (isGoogleMapsAvailable) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings
            ) {
                // Live location marker
                Marker(
                    state = markerState,
                    title = "Live SOS Location",
                    snippet = "Accuracy: ±${accuracyMeters.toInt()}m"
                )

                // Accuracy Radius Circle
                Circle(
                    center = current,
                    radius = accuracyMeters.coerceAtLeast(15.0),
                    fillColor = accentColor.copy(alpha = 0.22f),
                    strokeColor = accentColor,
                    strokeWidth = 3f
                )

                // Path Polyline (if breadcrumbs available)
                if (path.size >= 2) {
                    Polyline(
                        points = path,
                        color = pathColor,
                        width = 8f
                    )
                }
            }
        } else {
            // Safety fallback if Google Play Services is missing
            val osmCurrent = remember(current) { GeoPoint(current.latitude, current.longitude) }
            val osmPath = remember(path) { path.map { GeoPoint(it.latitude, it.longitude) } }

            OsmMap(
                current = osmCurrent,
                accuracyMeters = accuracyMeters,
                path = osmPath,
                accentColor = accentColor,
                pathColor = pathColor,
                recenterTrigger = recenterTrigger,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Re-center FAB button
        FloatingActionButton(
            onClick = {
                if (isGoogleMapsAvailable) {
                    coroutineScope.launch {
                        try {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(current, 16.5f),
                                durationMs = 500
                            )
                        } catch (_: Exception) {
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(current, 16.5f)
                        }
                    }
                } else {
                    recenterTrigger++
                }
            },
            containerColor = DarkSurface,
            contentColor = accentColor,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 180.dp, end = 16.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Re-center map",
                tint = accentColor
            )
        }
    }
}
