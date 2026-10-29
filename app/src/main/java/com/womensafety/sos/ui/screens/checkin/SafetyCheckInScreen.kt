package com.womensafety.sos.ui.screens.checkin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.womensafety.sos.ui.components.JhumkaWatermarkBackground
import com.womensafety.sos.ui.theme.JhumkaBlushBackground
import com.womensafety.sos.ui.theme.JhumkaBorder
import com.womensafety.sos.ui.theme.JhumkaCrimsonRed
import com.womensafety.sos.ui.theme.JhumkaCrimsonContainer
import com.womensafety.sos.ui.theme.JhumkaPinkContainer
import com.womensafety.sos.ui.theme.JhumkaPinkPrimary
import com.womensafety.sos.ui.theme.JhumkaSafetyGreen
import com.womensafety.sos.ui.theme.JhumkaSurface
import com.womensafety.sos.ui.theme.JhumkaSurfaceVariant
import com.womensafety.sos.ui.theme.JhumkaTextPrimary
import com.womensafety.sos.ui.theme.JhumkaTextSecondary
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.components.JhumkaWatermarkBackground
import com.womensafety.sos.ui.theme.JhumkaBlushBackground
import com.womensafety.sos.ui.theme.JhumkaBorder
import com.womensafety.sos.ui.theme.JhumkaCrimsonRed
import com.womensafety.sos.ui.theme.JhumkaCrimsonContainer
import com.womensafety.sos.ui.theme.JhumkaPinkContainer
import com.womensafety.sos.ui.theme.JhumkaPinkPrimary
import com.womensafety.sos.ui.theme.JhumkaSafetyGreen
import com.womensafety.sos.ui.theme.JhumkaSurface
import com.womensafety.sos.ui.theme.JhumkaSurfaceVariant
import com.womensafety.sos.ui.theme.JhumkaTextPrimary
import com.womensafety.sos.ui.theme.JhumkaTextSecondary
import com.womensafety.sos.ui.theme.AlertRedGradientEnd
import com.womensafety.sos.ui.components.JhumkaWatermarkBackground
import com.womensafety.sos.ui.theme.JhumkaBlushBackground
import com.womensafety.sos.ui.theme.JhumkaBorder
import com.womensafety.sos.ui.theme.JhumkaCrimsonRed
import com.womensafety.sos.ui.theme.JhumkaCrimsonContainer
import com.womensafety.sos.ui.theme.JhumkaPinkContainer
import com.womensafety.sos.ui.theme.JhumkaPinkPrimary
import com.womensafety.sos.ui.theme.JhumkaSafetyGreen
import com.womensafety.sos.ui.theme.JhumkaSurface
import com.womensafety.sos.ui.theme.JhumkaSurfaceVariant
import com.womensafety.sos.ui.theme.JhumkaTextPrimary
import com.womensafety.sos.ui.theme.JhumkaTextSecondary
import com.womensafety.sos.ui.theme.AlertRedGradientStart
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkCardBorder
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TextSecondaryDark
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
                title = "WALKING CHECK-IN",
                subtitle = if (isTimerRunning) "Live countdown active" else "Auto-SOS safety timer",
                onBackClick = onBackClick
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = JhumkaBlushBackground
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(JhumkaBlushBackground)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            JhumkaWatermarkBackground(modifier = Modifier.fillMaxSize()) {}

            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isLandscape = screenWidth > screenHeight && screenHeight < 600.dp

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                "Auto-SOS triggers if not disarmed before 00:00."
                            else
                                "Auto-SOS will trigger if you don't check in before time runs out.",
                            style = MaterialTheme.typography.bodySmall,
                            color = JhumkaTextSecondary,
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
                            onMarkSafe = { viewModel.markAsSafe() },
                            onBackClick = onBackClick
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 540.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Header Subtext
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Walk Safe Countdown",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = JhumkaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Set a duration for your walk home. If you don't check in safely before 00:00, the app triggers emergency SOS & broadcasts your GPS to guardians.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JhumkaTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }

                    // Main Countdown Card
                    TimerDisplayCard(
                        isTimerRunning = isTimerRunning,
                        formattedTime = formattedTime,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Controls & Presets
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
                        onMarkSafe = { viewModel.markAsSafe() },
                        onBackClick = onBackClick
                    )

                    Spacer(modifier = Modifier.height(16.dp))
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
    val cardBackground by animateColorAsState(
        targetValue = if (isTimerRunning) JhumkaPinkContainer.copy(alpha = 0.4f) else JhumkaSurface,
        animationSpec = tween(400),
        label = "TimerCardBg"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isTimerRunning) JhumkaCrimsonRed.copy(alpha = 0.6f) else JhumkaBorder
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isTimerRunning) JhumkaCrimsonContainer
                        else JhumkaPinkContainer
                    )
                    .border(
                        1.dp,
                        if (isTimerRunning) JhumkaCrimsonRed.copy(alpha = 0.5f) else JhumkaPinkPrimary.copy(alpha = 0.4f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (isTimerRunning) JhumkaCrimsonRed else JhumkaPinkPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isTimerRunning) formattedTime else "00:00",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = if (isTimerRunning) JhumkaCrimsonRed else JhumkaTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isTimerRunning) JhumkaCrimsonRed else JhumkaSafetyGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isTimerRunning) "MONITORING ACTIVE • DO NOT CLOSE" else "READY TO START",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isTimerRunning) JhumkaCrimsonRed else JhumkaSafetyGreen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
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
    onMarkSafe: () -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    if (!isTimerRunning) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Quick Presets:",
                color = JhumkaTextPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TimerPresetButton("5 Min", modifier = Modifier.weight(1f)) { onStartTimer(5) }
                TimerPresetButton("15 Min", modifier = Modifier.weight(1f)) { onStartTimer(15) }
                TimerPresetButton("30 Min", modifier = Modifier.weight(1f)) { onStartTimer(30) }
                TimerPresetButton("60 Min", modifier = Modifier.weight(1f)) { onStartTimer(60) }
            }

            // Custom Minutes Row
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
                    placeholder = { Text("e.g. 20", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JhumkaPinkPrimary,
                        unfocusedBorderColor = JhumkaBorder,
                        focusedTextColor = JhumkaTextPrimary,
                        unfocusedTextColor = JhumkaTextPrimary,
                        focusedContainerColor = JhumkaSurface,
                        unfocusedContainerColor = JhumkaSurface
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JhumkaPinkPrimary,
                        disabledContainerColor = JhumkaPinkPrimary.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start", fontWeight = FontWeight.Bold)
                }
            }

            // Explicit Back to Home Action Button
            if (onBackClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, JhumkaBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = JhumkaSurface,
                        contentColor = JhumkaTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back to Safety Dashboard",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onMarkSafe,
                colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "I'M SAFE (DISARM TIMER)",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }

            // Explicit Back to Home Action while running
            if (onBackClick != null) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, JhumkaBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = JhumkaSurface,
                        contentColor = JhumkaTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Return to Home (Timer Stays Active)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
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
        colors = ButtonDefaults.buttonColors(containerColor = JhumkaSurface),
        border = BorderStroke(1.dp, JhumkaBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(50.dp)
    ) {
        Text(
            text = label,
            color = JhumkaTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
