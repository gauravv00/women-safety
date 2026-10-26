package com.womensafety.sos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.womensafety.sos.BuildConfig
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkCardBorder
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

// High-contrast brutalist dark map styling for Google Maps
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
 * accuracy radius visualization, and seamless OpenStreetMap fallback when an API key is missing.
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
    val hasValidGoogleMapsKey = BuildConfig.MAPS_API_KEY.isNotBlank() &&
            !BuildConfig.MAPS_API_KEY.contains("Dummy")

    // Initialize MapsInitializer early to prevent CameraUpdateFactory NullPointerException
    LaunchedEffect(Unit) {
        try {
            MapsInitializer.initialize(context)
        } catch (_: Exception) {}
    }

    // Default to Google Maps if API key is present; otherwise default to OpenStreetMap so tiles actually render!
    var useGoogleMaps by remember { mutableStateOf(hasValidGoogleMapsKey) }
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

    LaunchedEffect(current, useGoogleMaps) {
        if (!useGoogleMaps) return@LaunchedEffect
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
        if (useGoogleMaps) {
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

            // If user opened Google Maps without an API key, explain why tiles are beige and provide 1-tap switch
            if (!hasValidGoogleMapsKey) {
                Surface(
                    color = DarkSurface.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Maps API Key Missing", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Google rejected tile download (Authorization failure). Add MAPS_API_KEY to local.properties, or switch to OpenStreetMap for instant tiles.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { useGoogleMaps = false },
                            colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Switch to OpenStreetMap (Instant Tiles)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // High-reliability OpenStreetMap fallback (renders instantly without an API key)
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

        // Map provider toggle / indicator badge (Google Maps vs OpenStreetMap)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceVariant.copy(alpha = 0.92f))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                .clickable { useGoogleMaps = !useGoogleMaps }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = "Switch Map",
                tint = if (useGoogleMaps) TrustBlue else SafetyGreen,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (useGoogleMaps) "Google Maps" else "OpenStreetMap",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Re-center FAB button
        FloatingActionButton(
            onClick = {
                if (useGoogleMaps) {
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
