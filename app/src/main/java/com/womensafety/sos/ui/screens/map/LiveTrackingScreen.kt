package com.womensafety.sos.ui.screens.map

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.components.OsmMap
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.TrustBlue
import org.osmdroid.util.GeoPoint

@Composable
fun LiveTrackingScreen(
    viewModel: LiveTrackingViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentLocation by viewModel.currentLocation.collectAsState()
    val history by viewModel.locationHistory.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()

    val currentLat = currentLocation?.latitude ?: 28.6139
    val currentLng = currentLocation?.longitude ?: 77.2090

    fun shareLiveLocation() {
        val trackingUrl = "https://maps.google.com/?q=$currentLat,$currentLng"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MY LIVE EMERGENCY LOCATION")
            putExtra(Intent.EXTRA_TEXT, "EMERGENCY: Track my live location here: $trackingUrl")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Live Location"))
    }

    Scaffold(
        topBar = { AppTopBar(title = "LIVE TRACKING MAP") },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            OsmMap(
                current = GeoPoint(currentLat, currentLng),
                accuracyMeters = (currentLocation?.accuracy?.toDouble() ?: 30.0),
                path = history.map { GeoPoint(it.first, it.second) },
                accentColor = TrustBlue,
                pathColor = AlertRed,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom Overlay Status & Action Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isServiceRunning) "LIVE BROADCAST ACTIVE" else "LOCATION MONITORED",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isServiceRunning) AlertRed else Color.White
                            )
                            Text(
                                text = "Lat: ${String.format("%.5f", currentLat)}, Lng: ${String.format("%.5f", currentLng)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { shareLiveLocation() },
                        colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Share Live Location Link", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
