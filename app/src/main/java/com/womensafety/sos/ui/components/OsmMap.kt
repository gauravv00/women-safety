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
            map.overlays.clear()
            if (path.size >= 2) {
                map.overlays.add(Polyline().apply {
                    setPoints(path)
                    outlinePaint.color = pathColor.toArgb()
                    outlinePaint.strokeWidth = 10f
                })
            }
            map.overlays.add(Polygon().apply {
                points = Polygon.pointsAsCircle(current, accuracyMeters.coerceAtLeast(15.0))
                fillPaint.color = accentColor.copy(alpha = 0.25f).toArgb()
                outlinePaint.color = accentColor.toArgb()
                outlinePaint.strokeWidth = 3f
            })
            map.overlays.add(Marker(map).apply {
                position = current
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Live SOS Location"
            })
            map.controller.animateTo(current)
            map.invalidate()
        }
    )
}
