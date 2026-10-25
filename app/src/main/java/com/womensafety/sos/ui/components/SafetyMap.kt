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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
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

// High-contrast brutalist dark map styling matching the app's #0A0A0A aesthetic
private const val DARK_MAP_STYLE_JSON = """
[
  {"elementType": "geometry", "stylers": [{"color": "#121212"}]},
  {"elementType": "labels.icon", "stylers": [{"visibility": "off"}]},
  {"elementType": "labels.text.fill", "stylers": [{"color": "#757575"}]},
  {"elementType": "labels.text.stroke", "stylers": [{"color": "#121212"}]},
  {"featureType": "administrative", "elementType": "geometry", "stylers": [{"color": "#333333"}]},
  {"featureType": "administrative.country", "elementType": "labels.text.fill", "stylers": [{"color": "#9e9e9e"}]},
  {"featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{"color": "#bdbdbd"}]},
  {"featureType": "poi", "elementType": "labels.text.fill", "stylers": [{"color": "#757575"}]},
  {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#181818"}]},
  {"featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{"color": "#616161"}]},
  {"featureType": "poi.park", "elementType": "labels.text.stroke", "stylers": [{"color": "#121212"}]},
  {"featureType": "road", "elementType": "geometry.fill", "stylers": [{"color": "#212121"}]},
  {"featureType": "road", "elementType": "labels.text.fill", "stylers": [{"color": "#8a8a8a"}]},
  {"featureType": "road.arterial", "elementType": "geometry", "stylers": [{"color": "#2c2c2c"}]},
  {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#3a3a3a"}]},
  {"featureType": "road.highway.controlled_access", "elementType": "geometry", "stylers": [{"color": "#4e4e4e"}]},
  {"featureType": "road.local", "elementType": "labels.text.fill", "stylers": [{"color": "#616161"}]},
  {"featureType": "transit", "elementType": "labels.text.fill", "stylers": [{"color": "#757575"}]},
  {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#0a0a0a"}]},
  {"featureType": "water", "elementType": "labels.text.fill", "stylers": [{"color": "#3d3d3d"}]}
]
"""

@Composable
fun SafetyMap(
    current: LatLng,
    accuracyMeters: Double,
    path: List<LatLng> = emptyList(),
    accentColor: Color,
    pathColor: Color = accentColor,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(current, 16.5f)
    }

    val mapProperties = remember {
        MapProperties(
            isMyLocationEnabled = false,
            mapStyleOptions = MapStyleOptions(DARK_MAP_STYLE_JSON)
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

    // Track whether camera has been initialized
    var hasCenteredInitially by remember { mutableStateOf(false) }

    LaunchedEffect(current) {
        if (!hasCenteredInitially && current.latitude != 0.0 && current.longitude != 0.0) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(current, 16.5f)
            hasCenteredInitially = true
        } else if (hasCenteredInitially && !cameraPositionState.isMoving) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(current),
                durationMs = 600
            )
        }
    }

    Box(modifier = modifier) {
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

        // Re-center FAB button
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(current, 16.5f),
                        durationMs = 500
                    )
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
