package com.womensafety.sos.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.components.EmergencyCallPanel
import com.womensafety.sos.ui.components.SosButton
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.AlertRedGradientEnd
import com.womensafety.sos.ui.theme.AlertRedGradientStart
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToFakeCall: () -> Unit,
    onNavigateToCheckIn: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val activeIncident by viewModel.activeIncident.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val trustedContacts by viewModel.trustedContacts.collectAsState()
    val showCallPanel by viewModel.showCallPanel.collectAsState()

    val isSosActive = activeIncident != null || isServiceRunning

    // Permission launcher for runtime permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        val neededPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
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
        topBar = { AppTopBar(isSosActive = isSosActive) }
    ) { paddingValues ->
        Box(
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Active Alert Banner
                AnimatedVisibility(visible = isSosActive) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
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
                            Button(
                                onClick = onNavigateToMap,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text(text = "View Map", color = AlertRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (!isSosActive) {
                    Text(
                        text = "Press and hold SOS for 2 seconds to trigger emergency broadcast",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CENTER SOS BUTTON
                SosButton(
                    isActive = isSosActive,
                    onSosTriggered = { viewModel.triggerSos(context) },
                    onSosCancelled = { viewModel.cancelSos(context) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Action Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.Call,
                            label = "Emergency\nPanel",
                            color = SafetyGreen,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setShowCallPanel(true) }
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.PhoneCallback,
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

            // Bottom sheet panel for emergency calls
            if (showCallPanel) {
                EmergencyCallPanel(
                    sheetState = sheetState,
                    contacts = trustedContacts,
                    onDismiss = { viewModel.setShowCallPanel(false) }
                )
            }
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
            .height(84.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp
            )
        }
    }
}
