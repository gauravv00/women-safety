package com.womensafety.sos.ui.screens.fakecall

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.SafetyGreen

@Composable
fun FakeCallScreen(
    onDismiss: () -> Unit,
    viewModel: FakeCallViewModel = viewModel()
) {
    val callerName by viewModel.callerName.collectAsState()
    val isCallAnswered by viewModel.isCallAnswered.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = callerName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isCallAnswered) "00:14" else "Incoming Call...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCallAnswered) SafetyGreen else Color.LightGray
                )
            }

            // Call Actions
            if (!isCallAnswered) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 50.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Decline
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AlertRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Accept
                    IconButton(
                        onClick = { viewModel.answerCall() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SafetyGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Accept",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(bottom = 50.dp)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AlertRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
