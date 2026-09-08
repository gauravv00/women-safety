package com.womensafety.sos.ui.screens.checkin

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun SafetyCheckInScreen(
    onTimerTriggeredSos: () -> Unit,
    viewModel: SafetyCheckInViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = { AppTopBar(title = "SAFETY CHECK-IN TIMER") }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Walking Home Safety Timer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "If you don't check in before the timer reaches 00:00, emergency SOS will trigger automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Timer Display Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isTimerRunning) AlertRed else DarkSurface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isTimerRunning) formattedTime else "SELECT DURATION",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                // Controls
                if (!isTimerRunning) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Start Walk Timer:",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TimerPresetButton("5 Min", 5, modifier = Modifier.weight(1f)) {
                                viewModel.startTimer(5, context) {
                                    homeViewModel.triggerSos(context, "CHECKIN_TIMER")
                                }
                            }
                            TimerPresetButton("15 Min", 15, modifier = Modifier.weight(1f)) {
                                viewModel.startTimer(15, context) {
                                    homeViewModel.triggerSos(context, "CHECKIN_TIMER")
                                }
                            }
                            TimerPresetButton("30 Min", 30, modifier = Modifier.weight(1f)) {
                                viewModel.startTimer(30, context) {
                                    homeViewModel.triggerSos(context, "CHECKIN_TIMER")
                                }
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.markAsSafe() },
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "I'M SAFE (DISARM TIMER)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun TimerPresetButton(
    label: String,
    minutes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(50.dp)
    ) {
        Text(text = label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
