package com.womensafety.sos.ui.screens.fakecall

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FakeCallScreen(
    onDismiss: () -> Unit,
    viewModel: FakeCallViewModel = viewModel()
) {
    val context = LocalContext.current
    val stage by viewModel.stage.collectAsState()
    val callerName by viewModel.callerName.collectAsState()
    val callerNumber by viewModel.callerNumber.collectAsState()
    val callSeconds by viewModel.callSeconds.collectAsState()
    val scheduledCountdown by viewModel.scheduledCountdown.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isOnHold by viewModel.isOnHold.collectAsState()
    val showKeypad by viewModel.showKeypad.collectAsState()
    val dialedDigits by viewModel.dialedDigits.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initTts(context)
        if (stage == FakeCallStage.INCOMING) {
            viewModel.startRinging(context)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopRinging()
        }
    }

    BackHandler {
        when (stage) {
            FakeCallStage.SETUP -> onDismiss()
            FakeCallStage.INCOMING -> viewModel.declineCall(context, onDismiss)
            FakeCallStage.IN_CALL -> viewModel.endCall(onDismiss)
            FakeCallStage.ENDED -> onDismiss()
        }
    }

    when (stage) {
        FakeCallStage.SETUP -> {
            FakeCallSetupView(
                callerName = callerName,
                callerNumber = callerNumber,
                scheduledCountdown = scheduledCountdown,
                onBackClick = onDismiss,
                onSetCaller = { name, num -> viewModel.setCallerDetails(name, num) },
                onTriggerNow = { viewModel.startImmediateIncoming(context) },
                onSchedule = { sec -> viewModel.scheduleCall(sec, context) },
                onCancelSchedule = { viewModel.cancelSchedule() }
            )
        }
        FakeCallStage.INCOMING -> {
            IncomingCallView(
                callerName = callerName,
                callerNumber = callerNumber,
                onAccept = { viewModel.answerCall(context) },
                onDecline = { viewModel.declineCall(context, onDismiss) },
                onOpenSettings = { viewModel.showSetup() }
            )
        }
        FakeCallStage.IN_CALL -> {
            InCallActiveView(
                callerName = callerName,
                callerNumber = callerNumber,
                callSeconds = callSeconds,
                isMuted = isMuted,
                isSpeakerOn = isSpeakerOn,
                isOnHold = isOnHold,
                showKeypad = showKeypad,
                dialedDigits = dialedDigits,
                onToggleMute = { viewModel.toggleMute() },
                onToggleSpeaker = { viewModel.toggleSpeaker() },
                onToggleHold = { viewModel.toggleHold() },
                onToggleKeypad = { viewModel.toggleKeypad() },
                onKeypadDigit = { viewModel.pressKeypadDigit(it) },
                onDeleteDigit = { viewModel.deleteKeypadDigit() },
                onEndCall = { viewModel.endCall(onDismiss) }
            )
        }
        FakeCallStage.ENDED -> {
            LaunchedEffect(Unit) {
                onDismiss()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FakeCallSetupView(
    callerName: String,
    callerNumber: String,
    scheduledCountdown: Int,
    onBackClick: () -> Unit,
    onSetCaller: (String, String) -> Unit,
    onTriggerNow: () -> Unit,
    onSchedule: (Int) -> Unit,
    onCancelSchedule: () -> Unit
) {
    var editName by remember { mutableStateOf(callerName) }
    var editNumber by remember { mutableStateOf(callerNumber) }

    val presets = listOf(
        "Mom" to "+1 (555) 019-2834",
        "Dad" to "+1 (555) 018-9124",
        "Police" to "112",
        "Cab Driver" to "+91 98765 12345",
        "Boss" to "+1 (555) 014-4455",
        "Home" to "+1 (555) 010-0011"
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = "FAKE CALL ESCAPE",
                onBackClick = onBackClick
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Discreet Situation Exit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Simulate an authentic incoming phone call with live audio conversation and interactive in-call controls to gracefully leave unsafe settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }

                // Preset Chips
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.LightGray
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEach { (name, num) ->
                        val isSelected = editName.equals(name, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                editName = name
                                editNumber = num
                                onSetCaller(name, num)
                            },
                            label = { Text(name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TrustBlue,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceVariant,
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }

                // Custom Inputs
                OutlinedTextField(
                    value = editName,
                    onValueChange = {
                        editName = it
                        onSetCaller(it, editNumber)
                    },
                    label = { Text("Caller Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrustBlue,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editNumber,
                    onValueChange = {
                        editNumber = it
                        onSetCaller(editName, it)
                    },
                    label = { Text("Caller Number / Label") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrustBlue,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

                if (scheduledCountdown > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ringing in $scheduledCountdown seconds...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = onCancelSchedule,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text("Cancel", color = AlertRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSchedule(5) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("In 5s", color = Color.White)
                        }
                        Button(
                            onClick = { onSchedule(15) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("In 15s", color = Color.White)
                        }
                        Button(
                            onClick = { onSchedule(30) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("In 30s", color = Color.White)
                        }
                    }

                    Button(
                        onClick = onTriggerNow,
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ring Phone Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingCallView(
    callerName: String,
    callerNumber: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Settings shortcut
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.LightGray)
                }
            }

            // Caller Info & Pulsing Avatar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // Pulsing Ring
                    Box(
                        modifier = Modifier
                            .size(140.dp * pulseScale)
                            .clip(CircleShape)
                            .background(TrustBlue.copy(alpha = pulseAlpha))
                    )
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = callerName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = callerNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Incoming Call...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SafetyGreen
                )
            }

            // Accept & Decline Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline (Red)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(AlertRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Decline", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Accept (Green)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(SafetyGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Accept",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Accept", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun InCallActiveView(
    callerName: String,
    callerNumber: String,
    callSeconds: Int,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isOnHold: Boolean,
    showKeypad: Boolean,
    dialedDigits: String,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onToggleKeypad: () -> Unit,
    onKeypadDigit: (Char) -> Unit,
    onDeleteDigit: () -> Unit,
    onEndCall: () -> Unit
) {
    val minutes = callSeconds / 60
    val seconds = callSeconds % 60
    val durationFormatted = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Caller Info & Timer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 36.dp)
            ) {
                Text(
                    text = callerName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = callerNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isOnHold) "CALL ON HOLD" else durationFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOnHold) Color.Yellow else SafetyGreen
                )
            }

            // Center Content: Keypad OR 6 In-Call Control Tiles
            if (showKeypad) {
                // Interactive In-Call Dialpad Overlay
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Display typed numbers with backspace
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dialedDigits.ifBlank { " " },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        if (dialedDigits.isNotEmpty()) {
                            IconButton(onClick = onDeleteDigit) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", tint = Color.LightGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Keypad grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            KeypadButton("1", "") { onKeypadDigit('1') }
                            KeypadButton("2", "ABC") { onKeypadDigit('2') }
                            KeypadButton("3", "DEF") { onKeypadDigit('3') }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            KeypadButton("4", "GHI") { onKeypadDigit('4') }
                            KeypadButton("5", "JKL") { onKeypadDigit('5') }
                            KeypadButton("6", "MNO") { onKeypadDigit('6') }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            KeypadButton("7", "PQRS") { onKeypadDigit('7') }
                            KeypadButton("8", "TUV") { onKeypadDigit('8') }
                            KeypadButton("9", "WXYZ") { onKeypadDigit('9') }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            KeypadButton("*", "") { onKeypadDigit('*') }
                            KeypadButton("0", "+") { onKeypadDigit('0') }
                            KeypadButton("#", "") { onKeypadDigit('#') }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onToggleKeypad,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Hide Keypad", color = Color.White)
                    }
                }
            } else {
                // 6 Interactive Call Controls (2 rows of 3)
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallTileButton(
                            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (isMuted) "Unmute" else "Mute",
                            isActive = isMuted,
                            onClick = onToggleMute
                        )
                        CallTileButton(
                            icon = Icons.Default.Dialpad,
                            label = "Keypad",
                            isActive = false,
                            onClick = onToggleKeypad
                        )
                        CallTileButton(
                            icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            label = if (isSpeakerOn) "Speaker" else "Earpiece",
                            isActive = isSpeakerOn,
                            onClick = onToggleSpeaker
                        )
                    }

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallTileButton(
                            icon = Icons.Default.Phone,
                            label = "Add Call",
                            isActive = false,
                            onClick = { /* simulated */ }
                        )
                        CallTileButton(
                            icon = Icons.Default.Videocam,
                            label = "FaceTime",
                            isActive = false,
                            onClick = { /* simulated */ }
                        )
                        CallTileButton(
                            icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                            label = if (isOnHold) "Resume" else "Hold",
                            isActive = isOnHold,
                            onClick = onToggleHold
                        )
                    }
                }
            }

            // Red End Call Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 36.dp)
            ) {
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(AlertRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "End Call", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun CallTileButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.White else Color(0xFF1E293B))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun KeypadButton(
    number: String,
    subText: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E293B))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            if (subText.isNotEmpty()) {
                Text(
                    text = subText,
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
