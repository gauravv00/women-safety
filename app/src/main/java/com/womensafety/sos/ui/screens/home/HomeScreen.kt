package com.womensafety.sos.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.data.entity.TrustedContact
import com.womensafety.sos.ui.adaptive.rememberWindowInfo
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.components.EmergencyCallPanel
import com.womensafety.sos.ui.components.JhumkaWatermarkBackground
import com.womensafety.sos.ui.components.SosButton
import com.womensafety.sos.ui.screens.notifications.NotificationSideBar
import com.womensafety.sos.ui.screens.notifications.NotificationViewModel
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.AlertRedGradientEnd
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.JhumkaBlushBackground
import com.womensafety.sos.ui.theme.JhumkaBorder
import com.womensafety.sos.ui.theme.JhumkaCrimsonContainer
import com.womensafety.sos.ui.theme.JhumkaCrimsonGradientEnd
import com.womensafety.sos.ui.theme.JhumkaCrimsonGradientStart
import com.womensafety.sos.ui.theme.JhumkaCrimsonRed
import com.womensafety.sos.ui.theme.JhumkaPinkContainer
import com.womensafety.sos.ui.theme.JhumkaPinkPrimary
import com.womensafety.sos.ui.theme.JhumkaSafetyGreen
import com.womensafety.sos.ui.theme.JhumkaSurface
import com.womensafety.sos.ui.theme.JhumkaTextPrimary
import com.womensafety.sos.ui.theme.JhumkaTextSecondary
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToFakeCall: () -> Unit,
    onNavigateToCheckIn: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToContacts: () -> Unit = {},
    onNavigateToTracking: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel(),
    emergencyViewModel: EmergencyViewModel = viewModel()
) {
    val context = LocalContext.current
    val activeIncident by viewModel.activeIncident.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val trustedContacts by viewModel.trustedContacts.collectAsState()
    val showCallPanel by viewModel.showCallPanel.collectAsState()
    val unreadNotifCount by notificationViewModel.unreadCount.collectAsState()
    val emergencyId by emergencyViewModel.emergencyId.collectAsState()
    val isEmergencyActive by emergencyViewModel.isEmergencyActive.collectAsState()
    val activeWardEmergencies by emergencyViewModel.activeWardEmergencies.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val isSosActive = activeIncident != null || isServiceRunning || isEmergencyActive

    var showNotifRationaleDialog by remember { mutableStateOf(false) }
    var isNotificationSideBarOpen by remember { mutableStateOf(false) }

    val singleNotifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] == true
            if (!notifGranted && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                showNotifRationaleDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        val neededPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = neededPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    DisposableEffect(Unit) {
        viewModel.registerShakeDetector(context)
        onDispose {
            viewModel.unregisterShakeDetector()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isSosActive) "EMERGENCY SOS" else "JHUMKA",
                subtitle = if (isSosActive) "Broadcasting Live GPS" else "Hi, $userName",
                isSosActive = isSosActive,
                unreadNotificationCount = unreadNotifCount,
                onNotificationClick = onNavigateToNotifications
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    if (isSosActive) {
                        Brush.verticalGradient(
                            colors = listOf(JhumkaCrimsonGradientStart, JhumkaCrimsonGradientEnd)
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(JhumkaBlushBackground, JhumkaBlushBackground)
                        )
                    }
                )
        ) {
            if (!isSosActive) {
                JhumkaWatermarkBackground(modifier = Modifier.fillMaxSize()) {}
            }

            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isLandscape = screenWidth > screenHeight && screenHeight < 600.dp

            if (isLandscape) {
                // Two-pane strictly for true horizontal landscape orientation
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        GuardianStatusCapsule(
                            guardianCount = trustedContacts.size,
                            isSosActive = isSosActive,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val landscapeButtonSize = minOf(screenHeight * 0.65f, screenWidth * 0.35f).coerceIn(160.dp, 240.dp)

                        SosButton(
                            size = landscapeButtonSize,
                            isActive = isSosActive,
                            onSosTriggered = {
                                viewModel.triggerSos(context)
                                emergencyViewModel.startEmergency(context)
                            },
                            onSosCancelled = {
                                viewModel.cancelSos(context)
                                emergencyViewModel.stopEmergency(context)
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isSosActive) "Hold 2s to cancel emergency broadcast" else "Hold SOS 2s to broadcast emergency",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSosActive) Color.White.copy(alpha = 0.95f) else JhumkaTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedVisibility(visible = isSosActive) {
                            AlertBannerCard(
                                onNavigateToMap = onNavigateToMap,
                                emergencyId = emergencyId,
                                onStopSos = {
                                    viewModel.cancelSos(context)
                                    emergencyViewModel.stopEmergency(context)
                                }
                            )
                        }

                        AnimatedVisibility(visible = !isSosActive && activeWardEmergencies.isNotEmpty()) {
                            val wardEmergency = activeWardEmergencies.first()
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AlertRed.copy(alpha = 0.12f))
                                    .border(1.dp, AlertRed.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = JhumkaSurface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "🚨 WARD IN DANGER: ${wardEmergency.wardName}",
                                            fontWeight = FontWeight.Bold,
                                            color = JhumkaTextPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        text = "Emergency broadcast active! Tap below to track live location and listen to audio recorded by ${wardEmergency.wardName}.",
                                        color = JhumkaTextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onNavigateToTracking(wardEmergency.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Track on Map", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                com.womensafety.sos.service.NotificationHelper.cancelEmergencyNotification(context, wardEmergency.id)
                                                com.womensafety.sos.service.NotificationHelper.cancelAllAlerts(context)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Dismiss Siren", color = JhumkaTextPrimary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        QuickActionGrid(
                            onEmergencyPanelClick = { viewModel.setShowCallPanel(true) },
                            onNavigateToMap = onNavigateToMap,
                            onNavigateToFakeCall = onNavigateToFakeCall,
                            onNavigateToCheckIn = onNavigateToCheckIn
                        )

                        TrustedGuardiansCard(
                            contacts = trustedContacts,
                            onAddClick = onNavigateToContacts
                        )
                    }
                }
            } else {
                // Portrait Layout (Balanced & centered for both phones and tablet portrait)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Greeting Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Hi, $userName 👋",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSosActive) Color.White else JhumkaTextPrimary
                                )
                                Text(
                                    text = if (isSosActive) "Emergency broadcast in progress" else "You are protected by your safety network",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSosActive) Color.White.copy(alpha = 0.85f) else JhumkaTextSecondary
                                )
                            }
                        }

                        // 1. Top Guardian Status Capsule
                        GuardianStatusCapsule(
                            guardianCount = trustedContacts.size,
                            isSosActive = isSosActive,
                            modifier = Modifier.clickable { onNavigateToContacts() }
                        )

                        // 2. SOS Button Hero Center
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            val sosSize = minOf(screenWidth * 0.58f, 240.dp).coerceIn(180.dp, 240.dp)

                            SosButton(
                                size = sosSize,
                                isActive = isSosActive,
                                onSosTriggered = {
                                    viewModel.triggerSos(context)
                                    emergencyViewModel.startEmergency(context)
                                },
                                onSosCancelled = {
                                    viewModel.cancelSos(context)
                                    emergencyViewModel.stopEmergency(context)
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (isSosActive) "Hold 2s to cancel emergency broadcast" else "Hold SOS 2s to broadcast emergency",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSosActive) Color.White.copy(alpha = 0.95f) else JhumkaTextSecondary,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }

                        // 3. Active Alert Banner
                        AnimatedVisibility(visible = isSosActive) {
                            AlertBannerCard(
                                onNavigateToMap = onNavigateToMap,
                                emergencyId = emergencyId,
                                onStopSos = {
                                    viewModel.cancelSos(context)
                                    emergencyViewModel.stopEmergency(context)
                                }
                            )
                        }

                        AnimatedVisibility(visible = !isSosActive && activeWardEmergencies.isNotEmpty()) {
                            val wardEmergency = activeWardEmergencies.first()
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AlertRed.copy(alpha = 0.12f))
                                    .border(1.dp, AlertRed.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = JhumkaSurface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "🚨 WARD IN DANGER: ${wardEmergency.wardName}",
                                            fontWeight = FontWeight.Bold,
                                            color = JhumkaTextPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        text = "Emergency broadcast active! Tap below to track live location and listen to audio recorded by ${wardEmergency.wardName}.",
                                        color = JhumkaTextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onNavigateToTracking(wardEmergency.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Track on Map", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                com.womensafety.sos.service.NotificationHelper.cancelEmergencyNotification(context, wardEmergency.id)
                                                com.womensafety.sos.service.NotificationHelper.cancelAllAlerts(context)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Dismiss Siren", color = JhumkaTextPrimary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // 4. 2x2 Quick Action Cards Grid
                        QuickActionGrid(
                            onEmergencyPanelClick = { viewModel.setShowCallPanel(true) },
                            onNavigateToMap = onNavigateToMap,
                            onNavigateToFakeCall = onNavigateToFakeCall,
                            onNavigateToCheckIn = onNavigateToCheckIn
                        )

                        // 5. Trusted Guardians Card
                        TrustedGuardiansCard(
                            contacts = trustedContacts,
                            onAddClick = onNavigateToContacts
                        )

                        // Bottom spacing to avoid floating nav bar overlap
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }
            }

            // Bottom sheet panel for emergency calls
            if (showCallPanel) {
                EmergencyCallPanel(
                    sheetState = sheetState,
                    contacts = trustedContacts,
                    onDismiss = { viewModel.setShowCallPanel(false) }
                )
            }

            // Notification Permission Rationale Dialog (Android 13+)
            if (showNotifRationaleDialog) {
                AlertDialog(
                    onDismissRequest = { showNotifRationaleDialog = false },
                    containerColor = JhumkaSurface,
                    icon = {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = JhumkaPinkPrimary)
                    },
                    title = {
                        Text(text = "Enable Emergency Notifications", color = JhumkaTextPrimary, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(
                            text = "To receive critical SOS alerts, real-time ward live tracking notifications, and device pairing requests, notification permission is required.",
                            color = JhumkaTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showNotifRationaleDialog = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    singleNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JhumkaPinkPrimary)
                        ) {
                            Text("Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNotifRationaleDialog = false }) {
                            Text("Later", color = JhumkaTextSecondary)
                        }
                    }
                )
            }

            // Notification Side Bar Panel
            NotificationSideBar(
                isOpen = isNotificationSideBarOpen,
                onDismiss = { isNotificationSideBarOpen = false },
                onNavigateToTracking = { trackId ->
                    if (trackId.isNotBlank()) {
                        onNavigateToTracking(trackId)
                    } else {
                        onNavigateToMap()
                    }
                },
                viewModel = notificationViewModel
            )
        }
    }
}

@Composable
fun GuardianStatusCapsule(
    guardianCount: Int,
    isSosActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isSosActive) JhumkaCrimsonContainer else Color(0xFFF1F8F5),
        border = BorderStroke(1.dp, if (isSosActive) JhumkaCrimsonRed.copy(alpha = 0.5f) else Color(0xFFB7E4CF)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isSosActive) JhumkaCrimsonRed else JhumkaSafetyGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSosActive) "EMERGENCY BROADCAST ACTIVE" else "$guardianCount Guardians Linked • Live GPS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSosActive) JhumkaCrimsonRed else Color(0xFF0F5132)
            )
        }
    }
}

@Composable
fun AlertBannerCard(
    onNavigateToMap: () -> Unit,
    emergencyId: String = "",
    onStopSos: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AlertRed),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🚨 SOS ALERT ACTIVE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Broadcasting location & recording audio...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    if (emergencyId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Live ID: ${emergencyId.take(8)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onStopSos != null) {
                        Button(
                            onClick = onStopSos,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "STOP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    Button(
                        onClick = onNavigateToMap,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Map", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionGrid(
    onEmergencyPanelClick: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToFakeCall: () -> Unit,
    onNavigateToCheckIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionTile(
                icon = Icons.Default.Call,
                title = "Emergency Hotline",
                subtitle = "Police & Helplines",
                badgeBgColor = Color(0xFFE6F7F0),
                badgeIconColor = Color(0xFF00A86B),
                modifier = Modifier.weight(1f),
                onClick = onEmergencyPanelClick
            )

            QuickActionTile(
                icon = Icons.Default.LocationOn,
                title = "Live Tracking",
                subtitle = "Real-time GPS",
                badgeBgColor = Color(0xFFEBF3FE),
                badgeIconColor = Color(0xFF2563EB),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToMap
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionTile(
                icon = Icons.AutoMirrored.Filled.PhoneCallback,
                title = "Fake Call",
                subtitle = "Discreet Escape",
                badgeBgColor = Color(0xFFFFF3EB),
                badgeIconColor = Color(0xFFEA580C),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToFakeCall
            )

            QuickActionTile(
                icon = Icons.Default.Timer,
                title = "Safety Timer",
                subtitle = "Walking Check-In",
                badgeBgColor = Color(0xFFFCE7F0),
                badgeIconColor = Color(0xFFE11D48),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCheckIn
            )
        }
    }
}

@Composable
fun QuickActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badgeBgColor: Color,
    badgeIconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
        border = BorderStroke(1.dp, JhumkaBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = JhumkaTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = JhumkaTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TrustedGuardiansCard(
    contacts: List<TrustedContact>,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
        border = BorderStroke(1.dp, JhumkaBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Trusted Guardians",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaTextPrimary
                )
                Text(
                    text = "${contacts.size} Linked • Manage",
                    style = MaterialTheme.typography.labelSmall,
                    color = JhumkaPinkPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onAddClick() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (contacts.isEmpty()) {
                    Text(
                        text = "No guardians added yet. Tap + to link loved ones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = JhumkaTextSecondary,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clickable { onAddClick() }
                    )
                } else {
                    contacts.take(6).forEachIndexed { index, contact ->
                        GuardianAvatarBubble(
                            name = contact.name,
                            initial = contact.name.trim().take(1).uppercase(),
                            index = index,
                            onClick = onAddClick
                        )
                    }
                }

                // Add button bubble
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onAddClick() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(JhumkaPinkContainer)
                            .border(1.5.dp, JhumkaPinkPrimary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Guardian",
                            tint = JhumkaPinkPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelSmall,
                        color = JhumkaPinkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun GuardianAvatarBubble(
    name: String,
    initial: String,
    index: Int,
    onClick: (() -> Unit)? = null
) {
    val avatarColors = listOf(
        Color(0xFFFFE4E6) to Color(0xFFE11D48),
        Color(0xFFE0F2FE) to Color(0xFF0284C7),
        Color(0xFFDCFCE7) to Color(0xFF16A34A),
        Color(0xFFFEF3C7) to Color(0xFFD97706),
        Color(0xFFF3E8FF) to Color(0xFF9333EA)
    )
    val (bgColor, textColor) = avatarColors[index % avatarColors.size]

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Box(
            modifier = Modifier.size(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial.ifBlank { "G" },
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            // Active status indicator dot
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(JhumkaSurface)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(JhumkaSafetyGreen)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name.split(" ").firstOrNull() ?: name,
            style = MaterialTheme.typography.labelSmall,
            color = JhumkaTextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
