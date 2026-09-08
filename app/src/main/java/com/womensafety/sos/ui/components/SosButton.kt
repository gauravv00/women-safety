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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.AlertRedPressed
import com.womensafety.sos.ui.theme.SafetyGreen
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

    val requiredHoldMs = 2000L // 2 seconds press-and-hold requirement

    // Vibrator helper
    fun triggerVibrationTick() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30L)
        }
    }

    // Hold Progress timer
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
                delay(16) // ~60 FPS update tick
            }
        } else {
            holdProgress = 0f
        }
    }

    // Pulse Ripple Animation
    val infiniteTransition = rememberInfiniteTransition(label = "SosRippleTransition")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleAlpha"
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
            val outerRadius = min(size.toPx(), size.toPx()) / 2 * 0.75f

            // Pulse Ripples when idle or active
            if (!isHolding || isActive) {
                drawCircle(
                    color = if (isActive) AlertRed.copy(alpha = rippleAlpha) else AlertRed.copy(alpha = rippleAlpha * 0.5f),
                    radius = outerRadius * rippleScale,
                    center = centerOffset
                )
            }

            // Main Background Circle Button
            val baseColor = if (isActive) AlertRedPressed else (if (isHolding) AlertRedPressed else AlertRed)
            val gradientBrush = Brush.radialGradient(
                colors = listOf(baseColor, baseColor.copy(alpha = 0.8f)),
                center = centerOffset,
                radius = outerRadius
            )

            drawCircle(
                brush = gradientBrush,
                radius = outerRadius,
                center = centerOffset
            )

            // Hold-Progress Countdown Ring (drawArc)
            if (isHolding && holdProgress > 0f) {
                val strokeWidth = 14.dp.toPx()
                val arcSize = Size((outerRadius * 2) - strokeWidth, (outerRadius * 2) - strokeWidth)
                val topLeft = Offset(centerOffset.x - arcSize.width / 2, centerOffset.y - arcSize.height / 2)

                // Background track
                drawArc(
                    color = Color.White.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                // Active progress arc
                drawArc(
                    color = if (isActive) SafetyGreen else Color.White,
                    startAngle = -90f,
                    sweepAngle = 360f * holdProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Inner Label Text & Countdown Display
        Text(
            text = when {
                isHolding && isActive -> "CANCEL\n${String.format("%.1fs", (1.0f - holdProgress) * 2)}"
                isHolding -> "HOLDING\n${String.format("%.1fs", (1.0f - holdProgress) * 2)}"
                isActive -> "HOLD TO\nCANCEL"
                else -> "SOS\nHOLD 2s"
            },
            color = Color.White,
            fontSize = if (isHolding) 24.sp else 28.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 32.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
