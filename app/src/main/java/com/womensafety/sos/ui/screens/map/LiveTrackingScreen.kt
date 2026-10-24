package com.womensafety.sos.ui.screens.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
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
import com.womensafety.sos.ui.components.OsmMap
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue
import org.osmdroid.util.GeoPoint
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

    var showResolveDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = onBackClick != null) {
        onBackClick?.invoke()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            viewModel.refreshLocation()
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
        ?: (currentLocation?.accuracy?.toDouble() ?: 20.0)

    val isEmergencyActive = emergency?.status == "ACTIVE"
    val isWardUser = emergency != null && emergency?.wardUid == viewModel.currentUserId
    val accentColor = when {
        emergency != null && isEmergencyActive -> AlertRed
        emergency != null -> SafetyGreen
        else -> TrustBlue
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val lastUpdateText = remember(emergency?.lastLocationTs) {
        val ts = emergency?.lastLocationTs ?: 0L
        if (ts > 0) "Updated " + timeFormatter.format(Date(ts)) else "Waiting for GPS..."
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "LIVE TRACKING",
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
            // OSM Map Component
            if (targetLat != 0.0 && targetLng != 0.0) {
                OsmMap(
                    current = GeoPoint(targetLat, targetLng),
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
                            text = "Acquiring live location coordinates...",
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

            // Bottom overlay: Ward safety status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header row: Status chip + Accuracy badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when {
                                    emergency != null && isEmergencyActive -> "🚨 IN DANGER • ACTIVE"
                                    emergency != null -> "✓ EMERGENCY RESOLVED"
                                    else -> "MY LOCATION"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (accuracy > 0) {
                            Text(
                                text = "±${accuracy.toInt()}m accuracy",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ward name and last update time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (emergency != null && isEmergencyActive) Icons.Default.Warning else Icons.Default.Shield,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = emergency?.wardName?.ifBlank { "Ward" } ?: "Current Device",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = lastUpdateText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
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
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = TrustBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Navigate", color = TrustBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Resolve button: only visible if current user is the ward who triggered the emergency
                        if (isWardUser && isEmergencyActive) {
                            Button(
                                onClick = { showResolveDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RESOLVE SOS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
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
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen)
                        ) {
                            Text("Yes, I am Safe", color = Color.White, fontWeight = FontWeight.Bold)
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
