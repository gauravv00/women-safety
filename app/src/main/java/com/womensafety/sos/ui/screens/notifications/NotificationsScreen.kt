package com.womensafety.sos.ui.screens.notifications

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.data.model.NotificationItem
import com.womensafety.sos.data.model.NotificationStatus
import com.womensafety.sos.data.model.NotificationType
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    onNavigateToTracking: (String) -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "NOTIFICATIONS",
                onBackClick = onBackClick,
                actions = {
                    if (unreadCount > 0) {
                        IconButton(onClick = { viewModel.markAllRead() }) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark All Read",
                                tint = TrustBlue
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = uiState) {
                is NotificationsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TrustBlue)
                    }
                }
                is NotificationsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = Color.Gray)
                    }
                }
                is NotificationsUiState.Success -> {
                    if (state.items.isEmpty()) {
                        EmptyNotificationsView()
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 680.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = state.items,
                                key = { it.id }
                            ) { item ->
                                DismissibleNotificationCard(
                                    item = item,
                                    onDismiss = { viewModel.deleteNotification(item.id) },
                                    onClick = {
                                        viewModel.markRead(item.id)
                                        if (item.type == NotificationType.EMERGENCY || item.type == NotificationType.RESOLVED) {
                                            if (item.refId.isNotBlank()) {
                                                onNavigateToTracking(item.refId)
                                            }
                                        }
                                    },
                                    onAccept = { viewModel.acceptPairRequest(item) },
                                    onReject = { viewModel.rejectPairRequest(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissibleNotificationCard(
    item: NotificationItem,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) AlertRed else Color.Transparent,
                label = "DismissColor"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        }
    ) {
        NotificationCardContent(
            item = item,
            onClick = onClick,
            onAccept = onAccept,
            onReject = onReject
        )
    }
}

@Composable
fun NotificationCardContent(
    item: NotificationItem,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val isEmergency = item.type == NotificationType.EMERGENCY && item.status == NotificationStatus.ACTIVE
    val isResolved = item.type == NotificationType.RESOLVED || item.status == NotificationStatus.RESOLVED

    val cardColor = when {
        isEmergency -> AlertRed.copy(alpha = 0.22f)
        isResolved -> SafetyGreen.copy(alpha = 0.16f)
        !item.read -> DarkSurfaceVariant
        else -> DarkSurface
    }

    val typeIcon: ImageVector = when (item.type) {
        NotificationType.PAIR_REQUEST -> Icons.Default.PersonAdd
        NotificationType.PAIR_ACCEPTED -> Icons.Default.CheckCircle
        NotificationType.PAIR_REJECTED -> Icons.Default.Close
        NotificationType.EMERGENCY -> Icons.Default.Warning
        NotificationType.RESOLVED -> Icons.Default.Shield
    }

    val iconTint: Color = when {
        isEmergency -> AlertRed
        isResolved -> SafetyGreen
        item.type == NotificationType.PAIR_REQUEST -> TrustBlue
        item.type == NotificationType.PAIR_ACCEPTED -> SafetyGreen
        else -> Color.LightGray
    }

    val relativeTime = remember(item.createdAt) {
        try {
            DateUtils.getRelativeTimeSpanString(
                item.createdAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        } catch (e: Exception) {
            "Recently"
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Icon Box
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = relativeTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }

                // Unread dot indicator
                if (!item.read) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isEmergency) AlertRed else TrustBlue)
                    )
                }
            }

            // Status Badge for Emergency & Resolved
            if (isEmergency || isResolved) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isEmergency) AlertRed else SafetyGreen)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isEmergency) "🚨 ACTIVE EMERGENCY" else "✓ RESOLVED",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = "Tap to open live map →",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isEmergency) AlertRed else SafetyGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Inline Accept / Reject buttons for PAIR_REQUEST when status is PENDING
            if (item.type == NotificationType.PAIR_REQUEST && item.status == NotificationStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Accept", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Decline", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotificationsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No notifications yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Emergency alerts, pairing requests, and safety check-ins will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
