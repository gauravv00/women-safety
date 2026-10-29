package com.womensafety.sos.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.womensafety.sos.ui.theme.JhumkaCrimsonPressed
import com.womensafety.sos.ui.theme.JhumkaCrimsonRed
import com.womensafety.sos.ui.theme.JhumkaPinkLight
import com.womensafety.sos.ui.theme.JhumkaPinkPressed
import com.womensafety.sos.ui.theme.JhumkaPinkPrimary
import com.womensafety.sos.ui.theme.JhumkaSafetyGreen
import com.womensafety.sos.ui.theme.Poppins
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun SosButton(
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    isActive: Boolean = false,
    onSosTriggered: () -> Unit,
    onSosCancelled: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    val requiredHoldMs = 2000L // 2 seconds hold requirement

    fun triggerVibrationTick() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(25L)
        }
    }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val startTime = System.currentTimeMillis()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            while (isHolding && holdProgress < 1.0f) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed.toFloat() / requiredHoldMs).coerceAtMost(1.0f)
                if (holdProgress % 0.25f < 0.05f) {
                    triggerVibrationTick()
                }
                if (holdProgress >= 1.0f) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isActive) {
                        onSosCancelled()
                    } else {
                        onSosTriggered()
                    }
                    isHolding = false
                    holdProgress = 0f
                    break
                }
                delay(16)
            }
        } else {
            holdProgress = 0f
        }
    }

    // Subtle breathing ambient pulse (soft, clean, not aggressive)
    val infiniteTransition = rememberInfiniteTransition(label = "SosCleanPulse")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .pointerInput(isActive) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        tryAwaitRelease()
                        isHolding = false
                        holdProgress = 0f
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.toPx() / 2, size.toPx() / 2)
            val outerRadius = min(size.toPx(), size.toPx()) / 2 * 0.72f

            val primaryColor = if (isActive) JhumkaCrimsonRed else JhumkaPinkPrimary
            val pressedColor = if (isActive) JhumkaCrimsonPressed else JhumkaPinkPressed
            val lightColor = if (isActive) JhumkaCrimsonRed.copy(alpha = 0.85f) else JhumkaPinkLight

            // 1. Soft Ambient Halo (Subtle Breathing Shadow)
            drawCircle(
                color = primaryColor.copy(alpha = if (isActive) 0.35f else breathingAlpha),
                radius = outerRadius * (if (isActive) 1.25f else breathingScale),
                center = centerOffset
            )

            // 2. Outer Track Ring
            val ringRadius = outerRadius * 1.08f
            val ringStrokeWidth = 4.dp.toPx()
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                radius = ringRadius,
                center = centerOffset,
                style = Stroke(width = ringStrokeWidth)
            )

            // 3. Dynamic Progress Arc on Outer Ring
            val arcProgress = if (isHolding) holdProgress else 0.45f
            val arcSize = Size(ringRadius * 2, ringRadius * 2)
            val arcTopLeft = Offset(centerOffset.x - ringRadius, centerOffset.y - ringRadius)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.4f), primaryColor),
                    center = centerOffset
                ),
                startAngle = if (isHolding) -90f else -60f,
                sweepAngle = if (isHolding) 360f * holdProgress else 120f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = if (isHolding) 6.dp.toPx() else ringStrokeWidth, cap = StrokeCap.Round)
            )

            // 4. Main Tactile Gradient Button Disc
            val baseColor = when {
                isActive -> JhumkaCrimsonRed
                isHolding -> pressedColor
                else -> primaryColor
            }
            val gradientDisc = Brush.radialGradient(
                colors = listOf(lightColor, baseColor),
                center = Offset(centerOffset.x, centerOffset.y - (outerRadius * 0.25f)),
                radius = outerRadius * 1.1f
            )
            drawCircle(
                brush = gradientDisc,
                radius = outerRadius,
                center = centerOffset
            )

            // 5. Delicate inner soft specular highlight rim
            drawCircle(
                color = Color.White.copy(alpha = 0.32f),
                radius = outerRadius * 0.94f,
                center = centerOffset,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Inner Content Column: Jhumka Motif + "SOS" + "• HOLD 2s"
        val scaleFactor = (size.value / 260f).coerceIn(0.6f, 1.4f)
        val iconSize = (28f * scaleFactor).dp
        val sosFontSize = (32f * scaleFactor).sp
        val subtitleFontSize = (12f * scaleFactor).sp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Spa,
                contentDescription = "Jhumka Motif",
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(iconSize)
            )

            Spacer(modifier = Modifier.height((2f * scaleFactor).dp))

            Text(
                text = if (isActive) "CANCEL" else "SOS",
                color = Color.White,
                fontFamily = Poppins,
                fontSize = sosFontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Text(
                text = when {
                    isHolding && isActive -> "HOLD ${String.format("%.1fs", (1.0f - holdProgress) * 2)}"
                    isHolding -> "HOLD ${String.format("%.1fs", (1.0f - holdProgress) * 2)}"
                    isActive -> "• HOLD TO CANCEL •"
                    else -> "• HOLD 2s •"
                },
                color = Color.White.copy(alpha = 0.9f),
                fontFamily = Poppins,
                fontSize = subtitleFontSize,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}
