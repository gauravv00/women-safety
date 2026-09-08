package com.womensafety.sos.ui.screens.contacts

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.data.entity.TrustedContact
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustedContactsScreen(
    viewModel: TrustedContactsViewModel = viewModel()
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputPhone by remember { mutableStateOf("") }
    var inputRelation by remember { mutableStateOf("Family") }

    // Native Contact Picker Contract
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                    val hasPhoneIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                    val name = if (nameIndex != -1) c.getString(nameIndex) else "Unknown"
                    val contactId = if (idIndex != -1) c.getString(idIndex) else ""
                    val hasPhone = if (hasPhoneIndex != -1) c.getInt(hasPhoneIndex) else 0

                    var phone = ""
                    if (hasPhone > 0) {
                        val pCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )
                        pCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                val pIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (pIndex != -1) phone = pc.getString(pIndex)
                            }
                        }
                    }

                    if (name.isNotEmpty() && phone.isNotEmpty()) {
                        viewModel.addContact(name, phone, "Friend")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "TRUSTED CONTACTS") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AlertRed,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
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
            ) {
                Text(
                    text = "Manage Safety Escalation Hierarchy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Priority #1 contact will be auto-called during SOS emergencies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = { contactPickerLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Contacts, contentDescription = null, tint = SafetyGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Pick From Phone Contacts", color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No emergency contacts added yet.\nTap + to add friends or family.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(contacts) { index, contact ->
                            ContactItemCard(
                                contact = contact,
                                isFirst = index == 0,
                                isLast = index == contacts.size - 1,
                                onMoveUp = { viewModel.movePriorityUp(contact) },
                                onMoveDown = { viewModel.movePriorityDown(contact) },
                                onDelete = { viewModel.deleteContact(contact.id) }
                            )
                        }
                    }
                }
            }

            // Manual Add Contact Dialog
            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    containerColor = DarkSurface,
                    title = { Text("Add Trusted Contact", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                label = { Text("Full Name", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = inputPhone,
                                onValueChange = { inputPhone = it },
                                label = { Text("Phone Number", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = inputRelation,
                                onValueChange = { inputRelation = it },
                                label = { Text("Relationship (e.g. Parent, Friend)", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (inputName.isNotBlank() && inputPhone.isNotBlank()) {
                                    viewModel.addContact(inputName, inputPhone, inputRelation)
                                    inputName = ""
                                    inputPhone = ""
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ContactItemCard(
    contact: TrustedContact,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(14.dp),
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (contact.priorityRank == 1) AlertRed else SafetyGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${contact.priorityRank}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${contact.relationship} • ${contact.phoneNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Move Up",
                        tint = if (!isFirst) Color.White else Color.DarkGray
                    )
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Move Down",
                        tint = if (!isLast) Color.White else Color.DarkGray
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
                }
            }
        }
    }
}
