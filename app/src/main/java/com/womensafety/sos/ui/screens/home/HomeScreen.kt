package com.womensafety.sos.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.components.EmergencyCallPanel
import com.womensafety.sos.ui.components.SosButton
import com.womensafety.sos.ui.screens.notifications.NotificationViewModel
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.AlertRedGradientEnd
import com.womensafety.sos.ui.theme.AlertRedGradientStart
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToFakeCall: () -> Unit,
    onNavigateToCheckIn: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val activeIncident by viewModel.activeIncident.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val trustedContacts by viewModel.trustedContacts.collectAsState()
    val showCallPanel by viewModel.showCallPanel.collectAsState()
    val unreadNotifCount by notificationViewModel.unreadCount.collectAsState()

    val isSosActive = activeIncident != null || isServiceRunning

    var showNotifRationaleDialog by remember { mutableStateOf(false) }

    val singleNotifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Notification permission handled
    }

    // Permission launcher for runtime permissions
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

    val backgroundColor by animateColorAsState(
        targetValue = if (isSosActive) AlertRedGradientEnd else DarkBackground,
        animationSpec = tween(600),
        label = "BgColor"
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            AppTopBar(
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
                            colors = listOf(AlertRedGradientStart, AlertRedGradientEnd)
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(backgroundColor, backgroundColor)
                        )
                    }
                )
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isLandscapeOrWide = screenWidth > screenHeight || screenWidth >= 600.dp

            if (isLandscapeOrWide) {
                // Responsive Two-Column Layout for Landscape / Tablets
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: SOS Button & Instructions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val landscapeButtonSize = minOf(screenHeight * 0.65f, screenWidth * 0.35f).coerceIn(160.dp, 260.dp)

                        SosButton(
                            size = landscapeButtonSize,
                            isActive = isSosActive,
                            onSosTriggered = { viewModel.triggerSos(context) },
                            onSosCancelled = { viewModel.cancelSos(context) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isSosActive) "Hold 2s to cancel emergency broadcast" else "Hold SOS 2s to broadcast emergency",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Right Column: Banner & Quick Actions
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedVisibility(visible = isSosActive) {
                            AlertBannerCard(onNavigateToMap = onNavigateToMap)
                        }

                        QuickActionGrid(
                            onEmergencyPanelClick = { viewModel.setShowCallPanel(true) },
                            onNavigateToMap = onNavigateToMap,
                            onNavigateToFakeCall = onNavigateToFakeCall,
                            onNavigateToCheckIn = onNavigateToCheckIn
                        )
                    }
                }
            } else {
                // Portrait Layout with safe scrolling and adaptive button size
                val portraitButtonSize = minOf(screenWidth * 0.62f, screenHeight * 0.36f).coerceIn(180.dp, 250.dp)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.widthIn(max = 540.dp)
                    ) {
                        // Active Alert Banner
                        AnimatedVisibility(visible = isSosActive) {
                            AlertBannerCard(onNavigateToMap = onNavigateToMap)
                        }

                        if (!isSosActive) {
                            Text(
                                text = "Press and hold SOS for 2 seconds to trigger emergency broadcast",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                        }
                    }

                    // CENTER SOS BUTTON with dynamic sizing
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SosButton(
                            size = portraitButtonSize,
                            isActive = isSosActive,
                            onSosTriggered = { viewModel.triggerSos(context) },
                            onSosCancelled = { viewModel.cancelSos(context) }
                        )
                    }

                    // Quick Action Cards
                    Box(
                        modifier = Modifier
                            .widthIn(max = 540.dp)
                            .padding(top = 10.dp)
                    ) {
                        QuickActionGrid(
                            onEmergencyPanelClick = { viewModel.setShowCallPanel(true) },
                            onNavigateToMap = onNavigateToMap,
                            onNavigateToFakeCall = onNavigateToFakeCall,
                            onNavigateToCheckIn = onNavigateToCheckIn
                        )
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
                    containerColor = DarkSurface,
                    icon = {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = TrustBlue)
                    },
                    title = {
                        Text(text = "Enable Emergency Notifications", color = Color.White, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(
                            text = "To receive critical SOS danger alerts, real-time ward live tracking notifications, and device pairing requests, notification permission is required.",
                            color = Color.LightGray,
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
                            colors = ButtonDefaults.buttonColors(containerColor = TrustBlue)
                        ) {
                            Text("Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNotifRationaleDialog = false }) {
                            Text("Later", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AlertBannerCard(
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AlertRed),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onNavigateToMap,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "View Map", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Call,
                label = "Emergency\nPanel",
                color = SafetyGreen,
                modifier = Modifier.weight(1f),
                onClick = onEmergencyPanelClick
            )

            QuickActionButton(
                icon = Icons.Default.LocationOn,
                label = "Live\nTracking",
                color = TrustBlue,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToMap
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                icon = Icons.AutoMirrored.Filled.PhoneCallback,
                label = "Fake\nCall",
                color = DarkSurfaceVariant,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToFakeCall
            )

            QuickActionButton(
                icon = Icons.Default.Timer,
                label = "Walking\nCheck-In",
                color = DarkSurfaceVariant,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCheckIn
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 76.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 16.sp
            )
        }
    }
}
