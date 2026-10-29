package com.womensafety.sos.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import com.womensafety.sos.data.entity.PairedWard
import com.womensafety.sos.data.util.PhoneUtils
import com.womensafety.sos.ui.components.AppTopBar
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
import com.womensafety.sos.ui.theme.JhumkaTextTertiary
import androidx.compose.foundation.BorderStroke
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBackClick: (() -> Unit)? = null,
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val settings by viewModel.settings.collectAsState()
    val pairedWards by viewModel.pairedWards.collectAsState()


    var showPairDialog by remember { mutableStateOf(false) }
    var inputWardCode by remember { mutableStateOf("") }
    var inputWardName by remember { mutableStateOf("") }
    var pairAttempted by remember { mutableStateOf(false) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var inputDisplayName by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val isWardCodeValid = remember(inputWardCode) { PhoneUtils.isValidPairingCode(inputWardCode) }
    val isWardNameValid = remember(inputWardName) { inputWardName.trim().length >= 2 }
    val pairRequestState by viewModel.pairRequestState.collectAsState()

    LaunchedEffect(pairRequestState) {
        when (val state = pairRequestState) {
            is PairRequestState.Success -> {
                showPairDialog = false
                inputWardCode = ""
                inputWardName = ""
                pairAttempted = false
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetPairRequestState()
            }
            is PairRequestState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetPairRequestState()
            }
            else -> Unit
        }
    }

    fun copyCodeToClipboard(code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Safety Pairing Code", code)
        clipboard.setPrimaryClip(clip)
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Pairing code copied to clipboard")
        }
    }

    fun sharePairingCode(code: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My Jhumka Safety Pairing Code")
            putExtra(
                Intent.EXTRA_TEXT,
                "Add me as your ward on the Jhumka App! My Device Safety Code is: $code. You will get instant alerts and live location if I am ever in danger."
            )
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Safety Pairing Code"))
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "SAFETY SETTINGS",
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        JhumkaWatermarkBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Guardian Device Pairing Section
                Text(
                    text = "Guardian Device Pairing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaPinkPrimary
                )

                // Account & Profile Information Card
                val currentUser = remember { FirebaseAuth.getInstance().currentUser }
                val userEmail = currentUser?.email
                val userPhone = currentUser?.phoneNumber

                Card(
                    colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(TrustBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = JhumkaPinkPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Account Profile",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = JhumkaTextPrimary
                                    )
                                    Text(
                                        text = if (currentUser != null && !currentUser.isAnonymous) "Verified Account" else "Guest Account",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (currentUser != null && !currentUser.isAnonymous) SafetyGreen else Color.Gray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            IconButton(onClick = {
                                inputDisplayName = settings.displayName
                                showEditNameDialog = true
                            }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Name", tint = JhumkaPinkPrimary)
                            }
                        }

                        // Display Name
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Name: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = settings.displayName.ifBlank { currentUser?.displayName ?: "User" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = JhumkaTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Google Email (if present)
                        if (!userEmail.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Email: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = JhumkaTextSecondary
                                )
                            }
                        }

                        // Verified Phone Number (if present)
                        if (!userPhone.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Phone: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = userPhone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SafetyGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, JhumkaCrimsonRed.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = JhumkaCrimsonRed
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Log Out",
                                tint = JhumkaCrimsonRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Log Out of Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = JhumkaCrimsonRed
                            )
                        }
                    }
                }

                // Shareable Safety Pairing Code Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = JhumkaPinkPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your Safety Pairing Code",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = JhumkaTextPrimary
                            )
                        }
                        Text(
                            text = "Give this 6-character code to your family or guardian so they can track your live SOS broadcasts in real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = JhumkaTextSecondary
                        )

                        // Highlighted Code Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(JhumkaPinkContainer.copy(alpha = 0.4f))
                                .border(1.dp, JhumkaBorder, RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = settings.myPairingCode,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = JhumkaPinkPrimary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 3.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { copyCodeToClipboard(settings.myPairingCode) },
                                colors = ButtonDefaults.buttonColors(containerColor = JhumkaSurface),
                                border = BorderStroke(1.dp, JhumkaBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = JhumkaTextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Code", color = JhumkaTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { sharePairingCode(settings.myPairingCode) },
                                colors = ButtonDefaults.buttonColors(containerColor = JhumkaPinkPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Code", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Paired Wards List
                Card(
                    colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = SafetyGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Paired Wards / Friends",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = JhumkaTextPrimary
                                )
                            }
                            IconButton(onClick = {
                                inputWardName = ""
                                inputWardCode = ""
                                pairAttempted = false
                                showPairDialog = true
                            }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Pair Ward", tint = SafetyGreen)
                            }
                        }

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
                                        onDelete = {
                                            viewModel.deletePairedWard(ward.id)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Unpaired ${ward.wardName}")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Guardian Monitoring Toggle Card
                SettingSwitchCard(
                    title = "Guardian Background Monitor",
                    subtitle = "Continuously poll Firebase for SOS emergencies triggered by your paired wards.",
                    icon = Icons.Default.Security,
                    checked = settings.guardianMonitoringEnabled,
                    onCheckedChange = { viewModel.setGuardianMonitoring(it, context) }
                )

                // Emergency Triggers Section
                Text(
                    text = "Emergency Trigger Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaTextPrimary
                )

                SettingSwitchCard(
                    title = "Loud Siren Alarm",
                    subtitle = "Play max-volume alarm tone on SOS activation to deter threats.",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    checked = settings.loudSirenEnabled,
                    onCheckedChange = { viewModel.setLoudSiren(it) }
                )

                // Auto-Dialing & Escalation
                Text(
                    text = "Auto-Dialing & Escalation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaTextPrimary
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
                        colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Auto-call Delay: ${settings.autoCallDelaySeconds} seconds",
                                color = JhumkaTextPrimary,
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
                    subtitle = "If top contact doesn't answer, automatically dial next contact in priority.",
                    icon = Icons.Default.Call,
                    checked = settings.sequentialEscalationEnabled,
                    onCheckedChange = { viewModel.setSequentialEscalation(it) }
                )

                // Display & Theme
                Text(
                    text = "Display & Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaTextPrimary
                )

                SettingSwitchCard(
                    title = "Dark Mode",
                    subtitle = "Switch between clean blush and dark obsidian theme.",
                    icon = Icons.Default.DarkMode,
                    checked = settings.darkModeEnabled,
                    onCheckedChange = { viewModel.setDarkMode(it) }
                )

                // Battery Awareness
                Text(
                    text = "Battery & Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaTextPrimary
                )

                SettingSwitchCard(
                    title = "Battery-Aware Location Polling",
                    subtitle = "Adjust GPS frequency dynamically when phone battery is low (<15%).",
                    icon = Icons.Default.BatteryAlert,
                    checked = settings.batteryAwarePolling,
                    onCheckedChange = { viewModel.setBatteryAware(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Validated Pair Ward Dialog with Responsive Scrolling
            if (showPairDialog) {
                AlertDialog(
                    onDismissRequest = { showPairDialog = false },
                    containerColor = JhumkaSurface,
                    modifier = Modifier.widthIn(max = 520.dp),
                    title = {
                        Text(
                            text = "Pair Protected Ward / Friend",
                            color = JhumkaTextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Ward Name Input
                            OutlinedTextField(
                                value = inputWardName,
                                onValueChange = { inputWardName = it },
                                label = { Text("Ward Name *") },
                                placeholder = { Text("e.g. Sister, Mom, Bestie", color = Color.DarkGray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                isError = pairAttempted && !isWardNameValid,
                                supportingText = {
                                    if (pairAttempted && !isWardNameValid) {
                                        Text("Please enter a name (at least 2 characters)", color = AlertRed)
                                    }
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = JhumkaPinkPrimary,
                                    unfocusedBorderColor = JhumkaBorder,
                                    focusedTextColor = JhumkaTextPrimary,
                                    unfocusedTextColor = JhumkaTextPrimary,
                                    focusedLabelColor = JhumkaPinkPrimary,
                                    unfocusedLabelColor = JhumkaTextSecondary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Safety Pairing Code Input with Real-time Normalization and Validation
                            OutlinedTextField(
                                value = inputWardCode,
                                onValueChange = { raw ->
                                    val cleaned = raw.uppercase().filter { it.isLetterOrDigit() || it == '-' }
                                    if (cleaned.length <= 10) {
                                        inputWardCode = cleaned
                                    }
                                },
                                label = { Text("Safety Pairing Code *") },
                                placeholder = { Text("e.g. SOS-9X2K7B or 9X2K7B", color = Color.DarkGray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    keyboardType = KeyboardType.Ascii,
                                    imeAction = ImeAction.Done
                                ),
                                isError = pairAttempted && !isWardCodeValid,
                                supportingText = {
                                    if (inputWardCode.isNotBlank()) {
                                        if (isWardCodeValid) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SafetyGreen, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Valid format: ${PhoneUtils.normalizePairingCode(inputWardCode)}", color = SafetyGreen)
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = AlertRed, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Code must be 6 letters/digits (e.g. SOS-XXXXXX)", color = AlertRed)
                                            }
                                        }
                                    } else if (pairAttempted) {
                                        Text("Pairing code is required", color = AlertRed)
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = if (isWardCodeValid) SafetyGreen else Color.Gray
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isWardCodeValid) JhumkaSafetyGreen else JhumkaPinkPrimary,
                                    unfocusedBorderColor = JhumkaBorder,
                                    focusedTextColor = JhumkaTextPrimary,
                                    unfocusedTextColor = JhumkaTextPrimary,
                                    focusedLabelColor = JhumkaPinkPrimary,
                                    unfocusedLabelColor = JhumkaTextSecondary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                pairAttempted = true
                                if (isWardNameValid && isWardCodeValid) {
                                    viewModel.sendPairRequest(inputWardCode, inputWardName)
                                    // dialog closed + snackbar shown via LaunchedEffect(pairRequestState)
                                }
                            },
                            enabled = pairRequestState !is PairRequestState.Loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isWardNameValid && isWardCodeValid) JhumkaPinkPrimary else JhumkaPinkPrimary.copy(alpha = 0.5f)
                            )
                        ) {
                            if (pairRequestState is PairRequestState.Loading) {
                                CircularProgressIndicator(color = Color.White, modifier = androidx.compose.ui.Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Send Request", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPairDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }

            // Logout Confirmation Dialog
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    containerColor = JhumkaSurface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.widthIn(max = 480.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = JhumkaCrimsonRed,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Log Out of Jhumka?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = JhumkaTextPrimary
                        )
                    },
                    text = {
                        Text(
                            text = "You will be signed out of your current session. You can easily sign back in anytime using Google or Phone OTP.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JhumkaTextSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLogoutDialog = false
                                viewModel.logout(context, onLogout)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JhumkaCrimsonRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Cancel", color = JhumkaTextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            // Edit User Name Dialog with Responsive Bounds
            if (showEditNameDialog) {
                AlertDialog(
                    onDismissRequest = { showEditNameDialog = false },
                    containerColor = JhumkaSurface,
                    modifier = Modifier.widthIn(max = 480.dp),
                    title = { Text("Your Display Name", color = JhumkaTextPrimary, fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = inputDisplayName,
                            onValueChange = { inputDisplayName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JhumkaPinkPrimary,
                                unfocusedBorderColor = JhumkaBorder,
                                focusedTextColor = JhumkaTextPrimary,
                                unfocusedTextColor = JhumkaTextPrimary,
                                focusedLabelColor = JhumkaPinkPrimary,
                                unfocusedLabelColor = JhumkaTextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (inputDisplayName.trim().isNotBlank()) {
                                    viewModel.updateDisplayName(inputDisplayName.trim())
                                    showEditNameDialog = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Updated name to ${inputDisplayName.trim()}")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JhumkaPinkPrimary)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditNameDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }
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
        colors = CardDefaults.cardColors(containerColor = if (isEmergency) JhumkaCrimsonContainer else JhumkaSurface),
        border = BorderStroke(1.dp, if (isEmergency) JhumkaCrimsonRed.copy(alpha = 0.4f) else JhumkaBorder),
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
                        .size(38.dp)
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
                            color = JhumkaTextPrimary
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
                        color = JhumkaTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Ward",
                    tint = AlertRed
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
        colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, JhumkaBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(imageVector = icon, contentDescription = null, tint = JhumkaPinkPrimary)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = JhumkaTextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = JhumkaTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = JhumkaPinkPrimary,
                    checkedTrackColor = JhumkaPinkContainer,
                    uncheckedThumbColor = JhumkaTextSecondary,
                    uncheckedTrackColor = JhumkaSurfaceVariant,
                    uncheckedBorderColor = JhumkaBorder
                )
            )
        }
    }
}
