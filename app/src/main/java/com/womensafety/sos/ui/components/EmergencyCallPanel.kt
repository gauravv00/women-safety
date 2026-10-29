package com.womensafety.sos.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.womensafety.sos.data.entity.TrustedContact
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.JhumkaBorder
import com.womensafety.sos.ui.theme.JhumkaCrimsonRed
import com.womensafety.sos.ui.theme.JhumkaPinkPrimary
import com.womensafety.sos.ui.theme.JhumkaSafetyGreen
import com.womensafety.sos.ui.theme.JhumkaSurface
import com.womensafety.sos.ui.theme.JhumkaSurfaceVariant
import com.womensafety.sos.ui.theme.JhumkaTextPrimary
import com.womensafety.sos.ui.theme.JhumkaTextSecondary
import androidx.compose.foundation.BorderStroke
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyCallPanel(
    sheetState: SheetState,
    contacts: List<TrustedContact>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isEscalating by remember { mutableStateOf(false) }
    var escalationStatus by remember { mutableStateOf("") }
    var escalationJob by remember { mutableStateOf<Job?>(null) }

    fun initiateCall(phoneNumber: String) {
        val cleanPhone = com.womensafety.sos.data.util.PhoneUtils.sanitizePhoneNumber(phoneNumber)
        if (cleanPhone.isBlank()) return

        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED && telecomManager != null) {
                val uri = Uri.fromParts("tel", cleanPhone, null)
                val extras = Bundle().apply {
                    putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true)
                }
                telecomManager.placeCall(uri, extras)
                return
            }
        } catch (e: Exception) {
            Log.w("EmergencyCallPanel", "TelecomManager.placeCall failed: ${e.localizedMessage}")
        }

        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$cleanPhone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("EmergencyCallPanel", "CALL_PHONE permission unavailable, launching DIAL: ${e.localizedMessage}")
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(dialIntent)
        }
    }

    fun stopEscalation() {
        escalationJob?.cancel()
        escalationJob = null
        isEscalating = false
        escalationStatus = ""
    }

    fun startSequentialEscalation() {
        if (isEscalating) {
            stopEscalation()
            return
        }
        if (contacts.isEmpty()) return

        isEscalating = true
        escalationJob = coroutineScope.launch {
            for ((index, contact) in contacts.withIndex()) {
                val cleanPhone = com.womensafety.sos.data.util.PhoneUtils.sanitizePhoneNumber(contact.phoneNumber)
                if (cleanPhone.isBlank()) continue

                initiateCall(cleanPhone)

                if (index < contacts.size - 1) {
                    val nextContact = contacts[index + 1]

                    // 1. Wait 5s for call to initiate
                    var elapsedSec = 0
                    while (isEscalating && elapsedSec < 5) {
                        escalationStatus = "Calling ${contact.name} (${elapsedSec}s) • Tap to Stop"
                        delay(1000L)
                        elapsedSec++
                    }

                    // 2. Wait for call to disconnect (up to 35s max ring/talk window)
                    val maxWaitSec = 35
                    var waitingSec = 5
                    while (isEscalating && waitingSec < maxWaitSec) {
                        var inCall = false
                        try {
                            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                            inCall = (tm?.callState ?: TelephonyManager.CALL_STATE_IDLE) != TelephonyManager.CALL_STATE_IDLE
                        } catch (e: Exception) {}

                        if (!inCall && waitingSec >= 8) {
                            break
                        }
                        escalationStatus = "Calling ${contact.name} (${waitingSec}s) • Next: ${nextContact.name}"
                        delay(1000L)
                        waitingSec++
                    }

                    if (!isEscalating) break

                    // 3. Essential 3.5s cooldown buffer between calls so telecom modem releases previous audio
                    for (c in 3 downTo 1) {
                        if (!isEscalating) break
                        escalationStatus = "Next in ${c}s: Dialing ${nextContact.name} • Tap to Stop"
                        delay(1200L)
                    }
                } else {
                    escalationStatus = "Calling last contact: ${contact.name}"
                    delay(5000L)
                }
            }
            isEscalating = false
            escalationStatus = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            stopEscalation()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = JhumkaSurface
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "EMERGENCY CALL PANEL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaCrimsonRed
                )
                Text(
                    text = "Tap to immediately dial trusted contacts or official helplines",
                    style = MaterialTheme.typography.bodySmall,
                    color = JhumkaTextSecondary,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // National / Official Helplines Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { initiateCall("112") },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 52.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LocalPolice, contentDescription = "Police")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Police (112)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { initiateCall("1091") },
                        colors = ButtonDefaults.buttonColors(containerColor = JhumkaPinkPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 52.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SupportAgent, contentDescription = "Helpline")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Helpline (1091)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Trusted Emergency Contacts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = JhumkaTextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (contacts.isNotEmpty()) {
                    Button(
                        onClick = { startSequentialEscalation() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEscalating) AlertRed else JhumkaSurfaceVariant,
                            contentColor = if (isEscalating) Color.White else JhumkaPinkPrimary
                        ),
                        border = BorderStroke(1.dp, if (isEscalating) AlertRed else JhumkaPinkPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = if (isEscalating) Icons.Default.CallEnd else Icons.Default.PhoneForwarded,
                            contentDescription = null,
                            tint = if (isEscalating) Color.White else JhumkaPinkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEscalating) escalationStatus else "Start Sequential Auto-Dial (${contacts.size} Contacts)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No contacts added yet. Add trusted contacts in the Contacts tab.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(contacts) { contact ->
                            ContactCallRow(contact = contact, onCallClick = { initiateCall(contact.phoneNumber) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ContactCallRow(
    contact: TrustedContact,
    onCallClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JhumkaSurfaceVariant),
        border = BorderStroke(1.dp, JhumkaBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCallClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SafetyGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = JhumkaTextPrimary
                        )
                        if (contact.priorityRank == 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PRIORITY #1",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlertRed,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Text(
                        text = "${contact.relationship} • ${contact.phoneNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = JhumkaTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SafetyGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = Color.White
                )
            }
        }
    }
}
