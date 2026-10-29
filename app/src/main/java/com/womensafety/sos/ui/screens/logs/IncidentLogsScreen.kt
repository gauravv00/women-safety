package com.womensafety.sos.ui.screens.logs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.data.entity.IncidentLog
import com.womensafety.sos.ui.components.AppTopBar
import com.womensafety.sos.ui.components.JhumkaWatermarkBackground
import com.womensafety.sos.ui.theme.JhumkaBlushBackground
import com.womensafety.sos.ui.theme.JhumkaBorder
import com.womensafety.sos.ui.theme.JhumkaCrimsonRed
import com.womensafety.sos.ui.theme.JhumkaPinkPrimary
import com.womensafety.sos.ui.theme.JhumkaSafetyGreen
import com.womensafety.sos.ui.theme.JhumkaSurface
import com.womensafety.sos.ui.theme.JhumkaTextPrimary
import com.womensafety.sos.ui.theme.JhumkaTextSecondary
import androidx.compose.foundation.BorderStroke
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncidentLogsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: IncidentLogsViewModel = viewModel()
) {
    val incidents by viewModel.incidentLogs.collectAsState()
    val playingId by viewModel.currentlyPlayingId.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "INCIDENT LOGS & EVIDENCE",
                onBackClick = onBackClick
            )
        },
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
            if (incidents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No emergency incidents recorded.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 700.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(incidents) { incident ->
                        IncidentItemCard(
                            incident = incident,
                            isPlaying = playingId == incident.id,
                            onPlayClick = {
                                if (playingId == incident.id) {
                                    viewModel.stopAudio()
                                } else {
                                    viewModel.playAudio(incident)
                                }
                            }
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
fun IncidentItemCard(
    incident: IncidentLog,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(incident.timestamp))
    val hasAudio = !incident.audioFilePath.isNullOrBlank() || !incident.audioCloudUrl.isNullOrBlank()

    Card(
        colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
        border = BorderStroke(1.dp, JhumkaBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Incident #${incident.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaTextPrimary
                )

                Text(
                    text = incident.status,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (incident.status == "ACTIVE") JhumkaCrimsonRed else JhumkaSafetyGreen
                )
            }

            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = JhumkaTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            Text(
                text = "Trigger: ${incident.triggerSource} | Lat: ${String.format("%.4f", incident.latitude)}, Lng: ${String.format("%.4f", incident.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = JhumkaTextSecondary
            )

            if (hasAudio) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) JhumkaCrimsonRed else JhumkaPinkPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlaying) "Stop Playing Audio Evidence" else "Play Recorded Audio Evidence",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
