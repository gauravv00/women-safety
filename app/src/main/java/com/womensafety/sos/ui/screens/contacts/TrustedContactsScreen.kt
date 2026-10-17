package com.womensafety.sos.ui.screens.contacts

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.data.entity.TrustedContact
import com.womensafety.sos.data.util.PhoneUtils
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrustedContactsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: TrustedContactsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val contacts by viewModel.contacts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputPhone by remember { mutableStateOf("") }
    var inputRelation by remember { mutableStateOf("Family") }
    var attemptedSubmit by remember { mutableStateOf(false) }

    // Relationship presets
    val relationChips = listOf("Family", "Parent", "Partner", "Friend", "Neighbor", "Colleague")

    // Validation
    val isPhoneValid = remember(inputPhone) { PhoneUtils.isValidPhoneNumber(inputPhone) }
    val isNameValid = remember(inputName) { inputName.trim().length >= 2 }

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

                    val name = if (nameIndex != -1) c.getString(nameIndex) else "Contact"
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

                    if (PhoneUtils.isValidPhoneNumber(phone)) {
                        viewModel.addContact(name, phone, "Friend")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Added $name to emergency contacts")
                        }
                    } else if (phone.isNotBlank()) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Invalid phone number format: $phone")
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "TRUSTED CONTACTS",
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    inputName = ""
                    inputPhone = ""
                    inputRelation = "Family"
                    attemptedSubmit = false
                    showAddDialog = true
                },
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
                // Info Header Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = SafetyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Emergency Escalation Hierarchy",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Priority #1 contact is automatically dialed when SOS triggers. If unanswered, call escalates down the list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Import Contacts Button
                Button(
                    onClick = { contactPickerLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Contacts, contentDescription = null, tint = TrustBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Import from Phone Contacts", color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.DarkGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No trusted contacts yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add at least 1-2 contacts who will receive your SOS alerts & live location.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(contacts) { index, contact ->
                            ContactItemCard(
                                contact = contact,
                                isFirst = index == 0,
                                isLast = index == contacts.size - 1,
                                onMoveUp = { viewModel.movePriorityUp(contact) },
                                onMoveDown = { viewModel.movePriorityDown(contact) },
                                onDelete = {
                                    viewModel.deleteContact(contact.id)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Removed ${contact.name}")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Validated Manual Add Contact Dialog
            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    containerColor = DarkSurface,
                    title = {
                        Text(
                            text = "Add Trusted Contact",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Full Name Input
                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                label = { Text("Full Name *") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                isError = attemptedSubmit && !isNameValid,
                                supportingText = {
                                    if (attemptedSubmit && !isNameValid) {
                                        Text("Please enter a valid name (at least 2 characters)", color = AlertRed)
                                    }
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TrustBlue,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = TrustBlue,
                                    unfocusedLabelColor = Color.Gray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Phone Number Input with Real-time Validation & Filtering
                            OutlinedTextField(
                                value = inputPhone,
                                onValueChange = { raw ->
                                    // Only allow valid phone characters: digits, +, -, spaces, ()
                                    val filtered = raw.filter { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' }
                                    if (filtered.length <= 18) {
                                        inputPhone = filtered
                                    }
                                },
                                label = { Text("Phone Number *") },
                                placeholder = { Text("+91 98765 43210 or 10-digit number", color = Color.DarkGray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                isError = attemptedSubmit && !isPhoneValid,
                                supportingText = {
                                    if (inputPhone.isNotBlank()) {
                                        if (isPhoneValid) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = SafetyGreen,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Valid phone format", color = SafetyGreen)
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = AlertRed,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Enter a valid 7-15 digit phone number", color = AlertRed)
                                            }
                                        }
                                    } else if (attemptedSubmit) {
                                        Text("Phone number is required", color = AlertRed)
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = if (isPhoneValid) SafetyGreen else Color.Gray
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isPhoneValid) SafetyGreen else TrustBlue,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = TrustBlue,
                                    unfocusedLabelColor = Color.Gray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Relationship Chips Selection
                            Text(
                                text = "Relationship:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.LightGray,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                relationChips.forEach { chip ->
                                    val isSelected = inputRelation.equals(chip, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { inputRelation = chip },
                                        label = { Text(chip, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TrustBlue,
                                            selectedLabelColor = Color.White,
                                            containerColor = DarkSurfaceVariant,
                                            labelColor = Color.LightGray
                                        )
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                attemptedSubmit = true
                                if (isNameValid && isPhoneValid) {
                                    viewModel.addContact(inputName, inputPhone, inputRelation)
                                    showAddDialog = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Added $inputName to emergency contacts")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNameValid && isPhoneValid) AlertRed else AlertRed.copy(alpha = 0.5f)
                            )
                        ) {
                            Text("Save Contact", color = Color.White, fontWeight = FontWeight.Bold)
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
                        .size(46.dp)
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
                                text = "TOP PRIORITY",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlertRed,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        text = "${contact.relationship} • ${PhoneUtils.formatForDisplay(contact.phoneNumber)}",
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
