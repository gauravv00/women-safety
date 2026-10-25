package com.womensafety.sos.ui.screens.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.components.SafetyMap
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkCardBorder
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TextSecondaryDark
import com.womensafety.sos.ui.theme.TrustBlue
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    emergencyId: String = "",
    onBackClick: (() -> Unit)? = null,
    viewModel: LiveTrackingViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(emergencyId) {
        viewModel.loadEmergency(emergencyId)
    }

    val emergency by viewModel.emergency.collectAsState()
    val activeEmergencies by viewModel.activeEmergencies.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val resolvedAddress by viewModel.resolvedAddress.collectAsState()
    val isRefreshingLocation by viewModel.isRefreshingLocation.collectAsState()

    var showResolveDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        onBackClick?.invoke()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            viewModel.refreshLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val targetLat = emergency?.let { if (it.lastLat != 0.0) it.lastLat else null }
        ?: currentLocation?.latitude
        ?: 0.0
    val targetLng = emergency?.let { if (it.lastLng != 0.0) it.lastLng else null }
        ?: currentLocation?.longitude
        ?: 0.0
    val accuracy = emergency?.lastAccuracy?.toDouble()
        ?: (currentLocation?.accuracy?.toDouble() ?: 15.0)

    LaunchedEffect(targetLat, targetLng) {
        if (targetLat != 0.0 && targetLng != 0.0) {
            viewModel.resolveAddressForCoordinates(context, targetLat, targetLng)
        }
    }

    val isEmergencyActive = emergency?.status == "ACTIVE"
    val isWardUser = emergency != null && emergency?.wardUid == viewModel.currentUserId
    val accentColor = when {
        emergency != null && isEmergencyActive -> AlertRed
        emergency != null -> SafetyGreen
        else -> TrustBlue
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val lastUpdateText = remember(emergency?.lastLocationTs, currentLocation?.time) {
        val emTs = emergency?.lastLocationTs ?: 0L
        val locTs = currentLocation?.time ?: 0L
        when {
            emTs > 0L -> "Updated " + timeFormatter.format(Date(emTs))
            emergency != null -> "Waiting for GPS from ${emergency?.wardName?.ifBlank { "Ward" }}..."
            locTs > 0L -> "Updated " + timeFormatter.format(Date(locTs))
            else -> "Live GPS synchronized"
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "LIVE TRACKING",
                subtitle = if (isEmergencyActive) "Active SOS broadcast" else "Real-time satellite GPS",
                onBackClick = onBackClick
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Google Maps Component
            if (targetLat != 0.0 && targetLng != 0.0) {
                SafetyMap(
                    current = LatLng(targetLat, targetLng),
                    accuracyMeters = accuracy,
                    path = emptyList(),
                    accentColor = accentColor,
                    pathColor = accentColor,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TrustBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Acquiring live satellite coordinates...",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Top overlay: multiple active emergencies selector (if > 1)
            if (activeEmergencies.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .horizontalScroll(rememberScrollState())
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeEmergencies.forEach { em ->
                        val isSelected = emergency?.id == em.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectEmergency(em) },
                            label = { Text("🚨 " + em.wardName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AlertRed,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurface,
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }
            }

            // Bottom overlay: Ward safety & location status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, DarkCardBorder),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header row: Status chip + Accuracy badge + Refresh button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = when {
                                    emergency != null && isEmergencyActive -> "🚨 SOS ACTIVE • BROADCASTING"
                                    emergency != null -> "✓ EMERGENCY RESOLVED"
                                    else -> "📍 DEVICE GPS ACTIVE"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (accuracy > 0) {
                                Text(
                                    text = "±${accuracy.toInt()}m",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // Quick Refresh GPS button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, DarkCardBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRefreshingLocation) {
                                    CircularProgressIndicator(
                                        color = TrustBlue,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = { viewModel.refreshLocation(context) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh GPS",
                                            tint = TrustBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ward name and last update time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.18f))
                                .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (emergency != null && isEmergencyActive) Icons.Default.Warning else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = emergency?.wardName?.ifBlank { "My Device" } ?: "My Device Location",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = if (resolvedAddress.isNotBlank()) resolvedAddress
                                else if (targetLat != 0.0 && targetLng != 0.0)
                                    String.format(Locale.US, "Lat: %.4f, Lng: %.4f", targetLat, targetLng)
                                else "Acquiring coordinates...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            Text(
                                text = "$lastUpdateText • ${String.format(Locale.US, "%.4f, %.4f", targetLat, targetLng)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    // Notice explaining emulator coordinates if showing Kartavya Path / New Delhi
                    if (resolvedAddress.contains("Delhi", ignoreCase = true) || resolvedAddress.contains("Kartavya", ignoreCase = true) || (targetLat in 28.5..28.7 && targetLng in 77.1..77.3)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TrustBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Default simulated location in Android Emulator. On physical phones, live hardware GPS coordinates are used.",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = TextSecondaryDark,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Open in external Google Maps
                        OutlinedButton(
                            onClick = {
                                if (targetLat != 0.0 && targetLng != 0.0) {
                                    val uri = Uri.parse("geo:$targetLat,$targetLng?q=$targetLat,$targetLng")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(mapIntent)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DarkCardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = TrustBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open in Maps",
                                color = TrustBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Resolve button: only visible if current user is the ward who triggered the emergency
                        if (isWardUser && isEmergencyActive) {
                            Button(
                                onClick = { showResolveDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RESOLVE SOS",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // Resolve SOS Confirmation Dialog
            if (showResolveDialog) {
                AlertDialog(
                    onDismissRequest = { showResolveDialog = false },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(20.dp),
                    icon = {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SafetyGreen)
                    },
                    title = {
                        Text(
                            text = "Resolve Emergency SOS?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Are you safe now? This will mark the emergency as RESOLVED, stop location broadcasting, and notify all your guardians.",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showResolveDialog = false
                                emergency?.id?.let { emId ->
                                    viewModel.resolveEmergency(context, emId) {
                                        onBackClick?.invoke()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Yes, I am Safe", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResolveDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}
