package com.womensafety.sos.ui.screens.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = { AppTopBar(title = "SAFETY SETTINGS") }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Emergency Trigger Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                SettingSwitchCard(
                    title = "Shake to Trigger SOS",
                    subtitle = "Rapidly shaking your phone will automatically launch emergency broadcast.",
                    icon = Icons.Default.Vibration,
                    checked = settings.shakeToTriggerEnabled,
                    onCheckedChange = { viewModel.setShakeToTrigger(it) }
                )

                SettingSwitchCard(
                    title = "Loud Siren Alarm",
                    subtitle = "Play max-volume alarm tone on SOS activation to deter threats.",
                    icon = Icons.Default.VolumeUp,
                    checked = settings.loudSirenEnabled,
                    onCheckedChange = { viewModel.setLoudSiren(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Auto-Dialing & Escalation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                SettingSwitchCard(
                    title = "Auto-Call Priority #1 Contact",
                    subtitle = "Automatically dial priority #1 trusted contact after activation.",
                    icon = Icons.Default.Call,
                    checked = settings.autoCallTopContact,
                    onCheckedChange = { viewModel.setAutoCallTopContact(it) }
                )

                if (settings.autoCallTopContact) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Auto-call Delay: ${settings.autoCallDelaySeconds} seconds",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = settings.autoCallDelaySeconds.toFloat(),
                                onValueChange = { viewModel.setAutoCallDelay(it.toInt()) },
                                valueRange = 5f..30f,
                                steps = 5
                            )
                        }
                    }
                }

                SettingSwitchCard(
                    title = "Sequential Escalation",
                    subtitle = "If top contact doesn't answer, automatically dial next contact.",
                    icon = Icons.Default.Call,
                    checked = settings.sequentialEscalationEnabled,
                    onCheckedChange = { viewModel.setSequentialEscalation(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Battery & Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                SettingSwitchCard(
                    title = "Battery-Aware Location Polling",
                    subtitle = "Adjust GPS frequency dynamically when phone battery is low (<15%).",
                    icon = Icons.Default.BatteryAlert,
                    checked = settings.batteryAwarePolling,
                    onCheckedChange = { viewModel.setBatteryAware(it) }
                )
            }
        }
    }
}

@Composable
fun SettingSwitchCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(imageVector = icon, contentDescription = null, tint = AlertRed)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = AlertRed, checkedTrackColor = AlertRed.copy(alpha = 0.5f))
            )
        }
    }
}
