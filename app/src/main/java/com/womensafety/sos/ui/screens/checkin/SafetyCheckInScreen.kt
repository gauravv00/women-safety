package com.womensafety.sos.ui.screens.checkin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.screens.home.HomeViewModel
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue

@Composable
fun SafetyCheckInScreen(
    onBackClick: (() -> Unit)? = null,
    onTimerTriggeredSos: () -> Unit,
    viewModel: SafetyCheckInViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()

    var customMinutesInput by remember { mutableStateOf("") }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    BackHandler(enabled = true) {
        onBackClick?.invoke()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "SAFETY CHECK-IN TIMER",
                onBackClick = onBackClick
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isLandscape = screenWidth > screenHeight && screenHeight < 600.dp

            if (isLandscape) {
                // Responsive Landscape 2-Column Split
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Timer Display Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        TimerDisplayCard(
                            isTimerRunning = isTimerRunning,
                            formattedTime = formattedTime,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Right Column: Controls & Presets
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isTimerRunning)
                                "If you don't disarm before 00:00, SOS triggers automatically."
                            else
                                "Auto-SOS will trigger if you don't check in before the timer ends.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        CheckInControls(
                            isTimerRunning = isTimerRunning,
                            customMinutesInput = customMinutesInput,
                            onCustomMinutesChange = { customMinutesInput = it },
                            onStartTimer = { min ->
                                viewModel.startTimer(min, context) {
                                    homeViewModel.triggerSos(context, "CHECKIN_TIMER")
                                    onTimerTriggeredSos()
                                }
                            },
                            onMarkSafe = { viewModel.markAsSafe() }
                        )
                    }
                }
            } else {
                // Portrait Layout with safe scrolling and keyboard support
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 560.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Walking Home Safety Timer",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "If you don't tap 'I'm Safe' before the timer reaches 00:00, emergency SOS and location broadcast will automatically trigger.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Timer Display Card
                    TimerDisplayCard(
                        isTimerRunning = isTimerRunning,
                        formattedTime = formattedTime,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Controls
                    CheckInControls(
                        isTimerRunning = isTimerRunning,
                        customMinutesInput = customMinutesInput,
                        onCustomMinutesChange = { customMinutesInput = it },
                        onStartTimer = { min ->
                            viewModel.startTimer(min, context) {
                                homeViewModel.triggerSos(context, "CHECKIN_TIMER")
                                onTimerTriggeredSos()
                            }
                        },
                        onMarkSafe = { viewModel.markAsSafe() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TimerDisplayCard(
    isTimerRunning: Boolean,
    formattedTime: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isTimerRunning) AlertRed else DarkSurface),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isTimerRunning) formattedTime else "00:00",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            if (isTimerRunning) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "MONITORING ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CheckInControls(
    isTimerRunning: Boolean,
    customMinutesInput: String,
    onCustomMinutesChange: (String) -> Unit,
    onStartTimer: (Int) -> Unit,
    onMarkSafe: () -> Unit
) {
    if (!isTimerRunning) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Select Walk Duration:",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TimerPresetButton("5 Min", modifier = Modifier.weight(1f)) {
                    onStartTimer(5)
                }
                TimerPresetButton("15 Min", modifier = Modifier.weight(1f)) {
                    onStartTimer(15)
                }
                TimerPresetButton("30 Min", modifier = Modifier.weight(1f)) {
                    onStartTimer(30)
                }
            }

            // Custom Minutes Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = customMinutesInput,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        if (digits.length <= 3) {
                            onCustomMinutesChange(digits)
                        }
                    },
                    label = { Text("Custom Minutes") },
                    placeholder = { Text("e.g. 10, 45", color = Color.DarkGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrustBlue,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                val customMin = customMinutesInput.toIntOrNull() ?: 0
                Button(
                    onClick = {
                        if (customMin in 1..180) {
                            onStartTimer(customMin)
                        }
                    },
                    enabled = customMin in 1..180,
                    colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Button(
            onClick = onMarkSafe,
            colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "I'M SAFE (DISARM TIMER)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TimerPresetButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(50.dp)
    ) {
        Text(text = label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
