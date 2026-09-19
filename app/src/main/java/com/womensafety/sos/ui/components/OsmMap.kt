package com.womensafety.sos.ui.components

import android.graphics.Paint
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File

/**
 * Minimum distance (meters) the location must move before we re-center the map.
 * Prevents constant animateTo jitter on stationary GPS drift.
 */
private const val MIN_MOVE_METERS = 5.0

@Composable
fun OsmMap(
    current: GeoPoint,
    accuracyMeters: Double,
    path: List<GeoPoint>,
    accentColor: Color,
    pathColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Persistent overlay references — avoids clear-and-recreate every recomposition
    val polyline = remember { Polyline() }
    val accuracyCircle = remember { Polygon() }
    // Marker must be created with a MapView reference, so we defer its creation

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
            )
            controller.setZoom(16.0)
            overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS) // dark tiles
        }
    }

    val marker = remember { Marker(mapView).apply { title = "Live SOS Location" } }

    // Track last animated-to position to skip no-op animateTo calls
    var lastAnimatedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    // Track whether overlays have been added to the map
    var overlaysAttached by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            // One-time: attach persistent overlays
            if (!overlaysAttached) {
                map.overlays.add(polyline)
                map.overlays.add(accuracyCircle)
                map.overlays.add(marker)
                overlaysAttached = true
            }

            // Update polyline path (only mutate data, don't recreate overlay)
            if (path.size >= 2) {
                polyline.setPoints(path)
                polyline.outlinePaint.color = pathColor.toArgb()
                polyline.outlinePaint.strokeWidth = 10f
                polyline.isVisible = true
            } else {
                polyline.isVisible = false
            }

            // Update accuracy circle
            accuracyCircle.points = Polygon.pointsAsCircle(current, accuracyMeters.coerceAtLeast(15.0))
            accuracyCircle.fillPaint.color = accentColor.copy(alpha = 0.25f).toArgb()
            accuracyCircle.outlinePaint.color = accentColor.toArgb()
            accuracyCircle.outlinePaint.strokeWidth = 3f

            // Update marker position
            marker.position = current
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Only animate camera if moved more than MIN_MOVE_METERS
            val last = lastAnimatedPoint
            if (last == null || last.distanceToAsDouble(current) >= MIN_MOVE_METERS) {
                map.controller.animateTo(current)
                lastAnimatedPoint = current
            }

            map.invalidate()
        }
    )
}
