package com.womensafety.sos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.womensafety.sos.R
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.womensafety.sos.ui.theme.JhumkaBlushBackground
import com.womensafety.sos.ui.theme.JhumkaBorder
import com.womensafety.sos.ui.theme.JhumkaCrimsonContainer
import com.womensafety.sos.ui.theme.JhumkaCrimsonRed
import com.womensafety.sos.ui.theme.JhumkaPinkContainer
import com.womensafety.sos.ui.theme.JhumkaPinkPrimary
import com.womensafety.sos.ui.theme.JhumkaSurface
import com.womensafety.sos.ui.theme.JhumkaSurfaceVariant
import com.womensafety.sos.ui.theme.JhumkaTextPrimary
import com.womensafety.sos.ui.theme.JhumkaTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String = "JHUMKA",
    subtitle: String? = null,
    isSosActive: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    unreadNotificationCount: Int = 0,
    onNotificationClick: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null
) {
    TopAppBar(
        navigationIcon = {
            if (onBackClick != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 6.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(JhumkaSurface)
                        .border(1.dp, JhumkaBorder, CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = JhumkaTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = if (onBackClick == null) 8.dp else 4.dp)
            ) {
                if (onBackClick == null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isSosActive) JhumkaCrimsonContainer else JhumkaSurface)
                            .border(
                                1.dp,
                                if (isSosActive) JhumkaCrimsonRed else JhumkaBorder,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSosActive) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Active SOS",
                                tint = JhumkaCrimsonRed,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Jhumka Logo",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.0.sp,
                        color = if (isSosActive) JhumkaCrimsonRed else JhumkaPinkPrimary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = JhumkaTextSecondary
                        )
                    }
                }
            }
        },
        actions = {
            if (onNotificationClick != null) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(JhumkaSurface)
                        .border(1.dp, JhumkaBorder, CircleShape)
                        .clickable { onNotificationClick() },
                    contentAlignment = Alignment.Center
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge(
                                    containerColor = JhumkaCrimsonRed,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = JhumkaTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            actions?.invoke()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = JhumkaBlushBackground
        ),
        windowInsets = TopAppBarDefaults.windowInsets
    )
}
