package com.womensafety.sos.ui.screens.map

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.components.EmergencyCallPanel
import com.womensafety.sos.ui.components.OsmMap
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: LiveTrackingViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentLocation by viewModel.currentLocation.collectAsState()
    val debouncedLocation by viewModel.debouncedLocation.collectAsState()
    val history by viewModel.locationHistory.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val pairedWards by viewModel.pairedWards.collectAsState()
    val selectedTargetCode by viewModel.selectedTargetCode.collectAsState()

    var showCallPanel by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BackHandler(enabled = onBackClick != null) {
        onBackClick?.invoke()
    }

    // Determine currently viewed ward or self
    val selectedWard = pairedWards.find { it.wardPairingCode == selectedTargetCode }
    val isViewingWard = selectedWard != null

    val mapLat: Double
    val mapLng: Double
    val displayLat: Double
    val displayLng: Double

    if (isViewingWard && selectedWard != null) {
        mapLat = if (selectedWard.lastKnownLat != 0.0) selectedWard.lastKnownLat else 28.6139
        mapLng = if (selectedWard.lastKnownLng != 0.0) selectedWard.lastKnownLng else 77.2090
        displayLat = selectedWard.lastKnownLat
        displayLng = selectedWard.lastKnownLng
    } else {
        mapLat = debouncedLocation?.latitude ?: currentLocation?.latitude ?: 28.6139
        mapLng = debouncedLocation?.longitude ?: currentLocation?.longitude ?: 77.2090
        displayLat = currentLocation?.latitude ?: 28.6139
        displayLng = currentLocation?.longitude ?: 77.2090
    }

    fun shareLiveLocation(lat: Double, lng: Double, label: String) {
        val trackingUrl = "https://maps.google.com/?q=$lat,$lng"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "$label LIVE LOCATION")
            putExtra(Intent.EXTRA_TEXT, "$label Live Tracking URL: $trackingUrl\nCoordinates: ($lat, $lng)")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Live Location"))
    }

    fun openInGoogleMaps(lat: Double, lng: Double, label: String) {
        val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lng"))
            context.startActivity(browserIntent)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isViewingWard) "${selectedWard?.wardName?.uppercase()} TRACKING" else "LIVE TRACKING MAP",
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            // OSM Map Rendering
            OsmMap(
                current = GeoPoint(mapLat, mapLng),
                accuracyMeters = if (isViewingWard) 20.0 else (currentLocation?.accuracy?.toDouble() ?: 30.0),
                path = if (isViewingWard) emptyList() else history.map { GeoPoint(it.first, it.second) },
                accentColor = if (isViewingWard && selectedWard?.lastStatus.equals("ACTIVE", ignoreCase = true)) AlertRed else TrustBlue,
                pathColor = AlertRed,
                modifier = Modifier.fillMaxSize()
            )

            // Top Target Selector Chips (My Location vs Paired Wards)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // My Location Chip
                FilterChip(
                    selected = selectedTargetCode == null,
                    onClick = { viewModel.selectTarget(null) },
                    label = { Text("📍 My Device", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TrustBlue,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurface.copy(alpha = 0.9f),
                        labelColor = Color.LightGray
                    )
                )

                // Paired Wards Chips
                pairedWards.forEach { ward ->
                    val isSelected = selectedTargetCode == ward.wardPairingCode
                    val isDanger = ward.lastStatus.equals("ACTIVE", ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectTarget(ward.wardPairingCode) },
                        label = {
                            Text(
                                text = if (isDanger) "🚨 ${ward.wardName}" else "👤 ${ward.wardName}",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDanger) AlertRed else SafetyGreen,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurface.copy(alpha = 0.9f),
                            labelColor = if (isDanger) AlertRed else Color.LightGray
                        )
                    )
                }
            }

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
                    if (isViewingWard && selectedWard != null) {
                        // Viewing Paired Ward Live Info
                        val isDanger = selectedWard.lastStatus.equals("ACTIVE", ignoreCase = true)
                        val lastUpdatedStr = if (selectedWard.lastActiveTimestamp > 0) {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(selectedWard.lastActiveTimestamp))
                        } else "Pending"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedWard.wardName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isDanger) "🚨 EMERGENCY SOS" else "✓ SAFE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDanger) AlertRed else SafetyGreen,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Text(
                                    text = "Code: ${selectedWard.wardPairingCode} • Last Sync: $lastUpdatedStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Lat: ${String.format("%.5f", displayLat)}, Lng: ${String.format("%.5f", displayLng)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { openInGoogleMaps(displayLat, displayLng, selectedWard.wardName) },
                                colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.NearMe, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Navigate", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { shareLiveLocation(displayLat, displayLng, selectedWard.wardName) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Share Link", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Viewing My Own Location
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isServiceRunning) "LIVE BROADCAST ACTIVE" else "MY DEVICE LOCATION",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isServiceRunning) AlertRed else Color.White
                                )
                                Text(
                                    text = "Lat: ${String.format("%.5f", displayLat)}, Lng: ${String.format("%.5f", displayLng)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Broadcast Live Location to Paired Guardians Button
                        Button(
                            onClick = {
                                viewModel.broadcastLocationToGuardians(context) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Live location shared to paired guardians")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Radio, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Broadcast Live Location to Guardians", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showCallPanel = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Call, contentDescription = "Emergency Call", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Emergency Call", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { shareLiveLocation(displayLat, displayLng, "My Emergency") },
                                colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Share Link", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (showCallPanel) {
                EmergencyCallPanel(
                    sheetState = sheetState,
                    contacts = contacts,
                    onDismiss = { showCallPanel = false }
                )
            }
        }
    }
}
