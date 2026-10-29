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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.runtime.DisposableEffect
import android.util.Log
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
import androidx.compose.material3.Surface
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
import com.womensafety.sos.ui.theme.JhumkaBlushBackground
import com.womensafety.sos.ui.theme.JhumkaBorder
import com.womensafety.sos.ui.theme.JhumkaCrimsonRed
import com.womensafety.sos.ui.theme.JhumkaPinkContainer
import com.womensafety.sos.ui.theme.JhumkaPinkPrimary
import com.womensafety.sos.ui.theme.JhumkaSafetyGreen
import com.womensafety.sos.ui.theme.JhumkaSurface
import com.womensafety.sos.ui.theme.JhumkaSurfaceVariant
import com.womensafety.sos.ui.theme.JhumkaTextPrimary
import com.womensafety.sos.ui.theme.JhumkaTextSecondary
import com.womensafety.sos.ui.theme.JhumkaTrustBlue
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
            viewModel.startLocationUpdates()
            viewModel.refreshLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.startLocationUpdates()
            viewModel.refreshLocation(context)
        }
    }

    val hasValidTarget = (emergency != null && emergency?.lastLat != 0.0) || (currentLocation != null && currentLocation?.latitude != 0.0)
    val defaultFallbackLat = 28.6139
    val defaultFallbackLng = 77.2090
    val targetLat = emergency?.let { if (it.lastLat != 0.0) it.lastLat else null }
        ?: currentLocation?.latitude
        ?: defaultFallbackLat
    val targetLng = emergency?.let { if (it.lastLng != 0.0) it.lastLng else null }
        ?: currentLocation?.longitude
        ?: defaultFallbackLng
    val accuracy = emergency?.lastAccuracy?.toDouble()
        ?: (currentLocation?.accuracy?.toDouble() ?: 20.0)

    LaunchedEffect(targetLat, targetLng) {
        if (targetLat != 0.0 && targetLng != 0.0) {
            viewModel.resolveAddressForCoordinates(context, targetLat, targetLng)
        }
    }

    val isEmergencyActive = emergency?.status == "ACTIVE"
    var isAudioPlaying by remember { mutableStateOf(false) }
    var audioPlayer: android.media.MediaPlayer? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer?.stop()
            audioPlayer?.release()
            audioPlayer = null
        }
    }
    val isWardUser = emergency != null && emergency?.wardUid == viewModel.currentUserId
    val accentColor = when {
        emergency != null && isEmergencyActive -> AlertRed
        emergency != null -> SafetyGreen
        else -> JhumkaPinkPrimary
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
        containerColor = JhumkaBlushBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map Component (SafetyMap with Google Maps / OpenStreetMap fallback)
            SafetyMap(
                current = LatLng(targetLat, targetLng),
                accuracyMeters = accuracy,
                path = emptyList(),
                accentColor = accentColor,
                pathColor = accentColor,
                modifier = Modifier.fillMaxSize()
            )

            if (!hasValidTarget) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = JhumkaSurface.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, JhumkaBorder),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = JhumkaPinkPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acquiring live satellite coordinates...",
                            color = JhumkaTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Top overlay: multiple active emergencies selector (deduplicated by wardUid / wardName)
            val distinctEmergencies = remember(activeEmergencies) {
                activeEmergencies.distinctBy { it.wardUid.ifBlank { it.wardName } }
            }
            if (distinctEmergencies.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .horizontalScroll(rememberScrollState())
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    distinctEmergencies.forEach { em ->
                        val isSelected = emergency?.id == em.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectEmergency(em) },
                            label = { Text("🚨 " + em.wardName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AlertRed,
                                selectedLabelColor = Color.White,
                                containerColor = JhumkaSurface,
                                labelColor = JhumkaTextSecondary
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
                border = BorderStroke(1.dp, JhumkaBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = JhumkaSurface)
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
                                    color = JhumkaTextSecondary,
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
                                    .background(JhumkaPinkContainer)
                                    .border(1.dp, JhumkaBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRefreshingLocation) {
                                    CircularProgressIndicator(
                                        color = JhumkaPinkPrimary,
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
                                            tint = JhumkaPinkPrimary,
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
                                color = JhumkaTextPrimary
                            )

                            Text(
                                text = if (resolvedAddress.isNotBlank()) resolvedAddress
                                else if (targetLat != 0.0 && targetLng != 0.0)
                                    String.format(Locale.US, "Lat: %.4f, Lng: %.4f", targetLat, targetLng)
                                else "Acquiring coordinates...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = JhumkaTextPrimary
                            )

                            Text(
                                text = "$lastUpdateText • ${String.format(Locale.US, "%.4f, %.4f", targetLat, targetLng)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = JhumkaTextSecondary
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
                                .background(JhumkaPinkContainer.copy(alpha = 0.4f))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = JhumkaPinkPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Default simulated location in Android Emulator. On physical phones, live hardware GPS coordinates are used.",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = JhumkaTextSecondary,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    // Audio Evidence Player (if ward recorded audio during emergency)
                    val audioUrl = emergency?.audioUrl
                    if (!audioUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(JhumkaPinkContainer.copy(alpha = 0.5f))
                                .border(1.dp, JhumkaBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = JhumkaPinkPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Ward Audio Evidence",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = JhumkaTextPrimary
                                    )
                                    Text(
                                        text = if (isAudioPlaying) "Playing live audio..." else "Tap to play recorded audio",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JhumkaTextSecondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (isAudioPlaying) {
                                        audioPlayer?.stop()
                                        audioPlayer?.release()
                                        audioPlayer = null
                                        isAudioPlaying = false
                                    } else {
                                        try {
                                            val player = android.media.MediaPlayer().apply {
                                                setAudioAttributes(
                                                    android.media.AudioAttributes.Builder()
                                                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                                        .build()
                                                )
                                                setDataSource(audioUrl)
                                                setOnPreparedListener {
                                                    start()
                                                    isAudioPlaying = true
                                                }
                                                setOnCompletionListener {
                                                    isAudioPlaying = false
                                                    release()
                                                    audioPlayer = null
                                                }
                                                prepareAsync()
                                            }
                                            audioPlayer = player
                                        } catch (e: Exception) {
                                            Log.e("LiveTracking", "Error playing audio: ${e.message}")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(JhumkaPinkPrimary)
                            ) {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isAudioPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
                                    val uri = Uri.parse("geo:$targetLat,$targetLng?q=$targetLat,$targetLng(SOS+Location)")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$targetLat,$targetLng")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, JhumkaBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = JhumkaSurface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = JhumkaPinkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open in Maps",
                                color = JhumkaPinkPrimary,
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
                    containerColor = JhumkaSurface,
                    shape = RoundedCornerShape(20.dp),
                    icon = {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SafetyGreen)
                    },
                    title = {
                        Text(
                            text = "Resolve Emergency SOS?",
                            color = JhumkaTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Are you safe now? This will mark the emergency as RESOLVED, stop location broadcasting, and notify all your guardians.",
                            color = JhumkaTextSecondary,
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
