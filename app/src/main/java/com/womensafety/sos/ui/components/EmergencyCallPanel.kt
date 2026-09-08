package com.womensafety.sos.ui.components

import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
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

    fun initiateCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("EmergencyCallPanel", "CALL_PHONE permission unavailable, launching DIAL: ${e.localizedMessage}")
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(dialIntent)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "EMERGENCY CALL PANEL",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AlertRed
            )
            Text(
                text = "Tap to immediately dial trusted contacts or official helplines",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 16.dp)
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
                        .height(54.dp)
                ) {
                    Icon(imageVector = Icons.Default.LocalPolice, contentDescription = "Police")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Police (112)", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { initiateCall("1091") },
                    colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(imageVector = Icons.Default.SupportAgent, contentDescription = "Helpline")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Helpline (1091)", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Trusted Emergency Contacts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

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
                    modifier = Modifier.height(240.dp)
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

@Composable
fun ContactCallRow(
    contact: TrustedContact,
    onCallClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                            color = Color.White
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
                        color = Color.LightGray
                    )
                }
            }

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
