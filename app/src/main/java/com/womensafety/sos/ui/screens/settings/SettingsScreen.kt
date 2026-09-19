package com.womensafety.sos.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.data.entity.PairedWard
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val pairedWards by viewModel.pairedWards.collectAsState()

    var showPairDialog by remember { mutableStateOf(false) }
    var inputWardCode by remember { mutableStateOf("") }
    var inputWardName by remember { mutableStateOf("") }

    fun copyCodeToClipboard(code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Safety Pairing Code", code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Pairing code copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun sharePairingCode(code: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My Women Safety Pairing Code")
            putExtra(
                Intent.EXTRA_TEXT,
                "Add me as your ward on Women Safety App! My Device Safety Code is: $code"
            )
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Safety Pairing Code"))
    }

    Scaffold(
        topBar = { AppTopBar(title = "SAFETY SETTINGS") },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                // Guardian Device Pairing Section
                Text(
                    text = "Guardian Device Pairing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TrustBlue
                )

                // My Safety Pairing Code Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = TrustBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "My Safety Pairing Code",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Share this code with your guardian so their phone receives instant alerts if you trigger SOS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = settings.myPairingCode.ifBlank { "SOS-XXXXXX" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = SafetyGreen
                            )
                            Row {
                                IconButton(onClick = { copyCodeToClipboard(settings.myPairingCode) }) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                                }
                                IconButton(onClick = { sharePairingCode(settings.myPairingCode) }) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = TrustBlue)
                                }
                            }
                        }
                    }
                }

                // Guardian Monitoring Toggle Card
                SettingSwitchCard(
                    title = "Guardian Mode (Monitor Wards)",
                    subtitle = "Listen for emergency SOS broadcasts from paired friends or family members.",
                    icon = Icons.Default.Security,
                    checked = settings.guardianMonitoringEnabled,
                    onCheckedChange = { viewModel.setGuardianMonitoring(it, context) }
                )

                // Paired Wards List & Add Button
                if (settings.guardianMonitoringEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Monitored Wards (${pairedWards.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Button(
                                    onClick = { showPairDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Pair Ward", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (pairedWards.isEmpty()) {
                                Text(
                                    text = "No wards paired yet. Tap 'Pair Ward' and enter their 6-character Safety Code to monitor them.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pairedWards.forEach { ward ->
                                        PairedWardRow(
                                            ward = ward,
                                            onDelete = { viewModel.deletePairedWard(ward.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

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
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    checked = settings.loudSirenEnabled,
                    onCheckedChange = { viewModel.setLoudSiren(it) }
                )

                Spacer(modifier = Modifier.height(6.dp))

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

                Spacer(modifier = Modifier.height(6.dp))

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

            // Pair Ward Dialog
            if (showPairDialog) {
                AlertDialog(
                    onDismissRequest = { showPairDialog = false },
                    containerColor = DarkSurface,
                    title = { Text("Pair Protected Ward / Friend", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputWardName,
                                onValueChange = { inputWardName = it },
                                label = { Text("Ward Name (e.g. Sister, Bestie)", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = inputWardCode,
                                onValueChange = { inputWardCode = it.uppercase() },
                                label = { Text("Safety Pairing Code (e.g. SOS-XXXXXX)", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (inputWardName.isNotBlank() && inputWardCode.isNotBlank()) {
                                    viewModel.addPairedWard(inputWardCode, inputWardName)
                                    inputWardName = ""
                                    inputWardCode = ""
                                    showPairDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TrustBlue)
                        ) {
                            Text("Pair Device", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPairDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PairedWardRow(
    ward: PairedWard,
    onDelete: () -> Unit
) {
    val isEmergency = ward.lastStatus.equals("ACTIVE", ignoreCase = true)

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isEmergency) AlertRed.copy(alpha = 0.2f) else DarkBackground),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isEmergency) AlertRed else SafetyGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ward.wardName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEmergency) "🚨 IN DANGER" else "✓ SAFE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isEmergency) AlertRed else SafetyGreen
                        )
                    }
                    Text(
                        text = "Code: ${ward.wardPairingCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Ward",
                    tint = Color.Gray
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
