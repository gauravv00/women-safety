package com.womensafety.sos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.womensafety.sos.ui.theme.JhumkaBlushBackground
import com.womensafety.sos.ui.theme.JhumkaPinkPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders a subtle, elegant backdrop with low-opacity pink line-art watermarks
 * of traditional Indian jhumka earrings and sacred geometric mandalas.
 */
@Composable
fun JhumkaWatermarkBackground(
    modifier: Modifier = Modifier,
    backgroundColor: Color = JhumkaBlushBackground,
    motifColor: Color = JhumkaPinkPrimary.copy(alpha = 0.055f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke1 = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
            val strokeFine = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round)

            // Draw Top-Right Traditional Mandala Watermark
            drawMandalaWatermark(
                center = Offset(size.width * 0.92f, size.height * 0.12f),
                radius = size.width * 0.38f,
                color = motifColor,
                stroke = stroke1,
                fineStroke = strokeFine
            )

            // Draw Subtle Left-Center Jhumka Earring Motif
            drawJhumkaEarringWatermark(
                anchor = Offset(size.width * 0.10f, size.height * 0.38f),
                scale = 1.25f,
                color = motifColor,
                stroke = stroke1
            )

            // Draw Subtle Bottom-Right Jhumka Earring Motif
            drawJhumkaEarringWatermark(
                anchor = Offset(size.width * 0.88f, size.height * 0.76f),
                scale = 1.4f,
                color = motifColor,
                stroke = stroke1
            )

            // Draw Subtle Bottom-Left Mini Mandala Motif
            drawMandalaWatermark(
                center = Offset(size.width * 0.05f, size.height * 0.88f),
                radius = size.width * 0.30f,
                color = motifColor,
                stroke = stroke1,
                fineStroke = strokeFine
            )
        }

        // Foreground Screen Content
        content()
    }
}

/**
 * Draws the vector line-art of a traditional Indian Jhumka:
 * - Upper floral stud (karanphool)
 * - Connecting link
 * - Bell dome (chhatri) with filigree bands & lattice
 * - Hanging beads / ghunghroos and central hanging pearl
 */
private fun DrawScope.drawJhumkaEarringWatermark(
    anchor: Offset,
    scale: Float,
    color: Color,
    stroke: Stroke
) {
    val cx = anchor.x
    val cy = anchor.y

    // 1. Upper Stud (Karanphool / Floral Chakra)
    val studRadius = 14f * scale
    drawCircle(color = color, radius = studRadius, center = Offset(cx, cy), style = stroke)
    drawCircle(color = color, radius = studRadius * 0.45f, center = Offset(cx, cy), style = stroke)

    // Stud petals (8 radial petals)
    for (i in 0 until 8) {
        val angle = (i * 45) * (PI / 180.0)
        val px = cx + (studRadius * 1.35f * cos(angle)).toFloat()
        val py = cy + (studRadius * 1.35f * sin(angle)).toFloat()
        drawCircle(color = color, radius = 2.5f * scale, center = Offset(px, py), style = stroke)
    }

    // 2. Connecting Loop Ring
    val loopY = cy + (studRadius * 1.6f)
    drawCircle(color = color, radius = 4f * scale, center = Offset(cx, loopY), style = stroke)

    // 3. Bell Dome (Chhatri)
    val domeTopY = loopY + (6f * scale)
    val domeBaseY = domeTopY + (48f * scale)
    val domeHalfWidth = 38f * scale

    val domePath = Path().apply {
        moveTo(cx - (8f * scale), domeTopY)
        // Left bell curve
        cubicTo(
            cx - (16f * scale), domeTopY + (14f * scale),
            cx - domeHalfWidth, domeBaseY - (10f * scale),
            cx - domeHalfWidth, domeBaseY
        )
        // Bottom scalloped edge
        lineTo(cx + domeHalfWidth, domeBaseY)
        // Right bell curve
        cubicTo(
            cx + domeHalfWidth, domeBaseY - (10f * scale),
            cx + (16f * scale), domeTopY + (14f * scale),
            cx + (8f * scale), domeTopY
        )
        close()
    }
    drawPath(path = domePath, color = color, style = stroke)

    // Dome horizontal filigree decorative bands
    val band1Y = domeTopY + (18f * scale)
    val band2Y = domeTopY + (32f * scale)
    drawLine(
        color = color,
        start = Offset(cx - (domeHalfWidth * 0.55f), band1Y),
        end = Offset(cx + (domeHalfWidth * 0.55f), band1Y),
        strokeWidth = stroke.width
    )
    drawLine(
        color = color,
        start = Offset(cx - (domeHalfWidth * 0.82f), band2Y),
        end = Offset(cx + (domeHalfWidth * 0.82f), band2Y),
        strokeWidth = stroke.width
    )

    // Dome vertical filigree ribs
    for (step in -2..2) {
        val ribX = cx + (step * (domeHalfWidth / 3.2f))
        val ribTopX = cx + (step * (4f * scale))
        drawLine(
            color = color,
            start = Offset(ribTopX, domeTopY + (6f * scale)),
            end = Offset(ribX, domeBaseY - (3f * scale)),
            strokeWidth = stroke.width * 0.8f
        )
    }

    // 4. Scalloped rim and hanging beads (ghunghroos)
    val beadCount = 7
    val beadSpacing = (domeHalfWidth * 2f) / (beadCount + 1)
    for (i in 1..beadCount) {
        val bx = (cx - domeHalfWidth) + (i * beadSpacing)
        val by = domeBaseY + (7f * scale)
        // Hanging line
        drawLine(
            color = color,
            start = Offset(bx, domeBaseY),
            end = Offset(bx, by),
            strokeWidth = stroke.width
        )
        // Little ghunghroo bead
        drawCircle(color = color, radius = 2.2f * scale, center = Offset(bx, by + (3f * scale)), style = stroke)
    }

    // 5. Central hanging elongated pearl drop
    val dropStartY = domeBaseY
    val dropEndY = domeBaseY + (24f * scale)
    drawLine(
        color = color,
        start = Offset(cx, dropStartY),
        end = Offset(cx, dropEndY - (6f * scale)),
        strokeWidth = stroke.width
    )
    // Teardrop pearl
    val pearlPath = Path().apply {
        moveTo(cx, dropEndY - (8f * scale))
        cubicTo(
            cx - (5f * scale), dropEndY - (2f * scale),
            cx - (4f * scale), dropEndY + (6f * scale),
            cx, dropEndY + (6f * scale)
        )
        cubicTo(
            cx + (4f * scale), dropEndY + (6f * scale),
            cx + (5f * scale), dropEndY - (2f * scale),
            cx, dropEndY - (8f * scale)
        )
        close()
    }
    drawPath(path = pearlPath, color = color, style = stroke)
}

/**
 * Draws a circular geometric mandala with concentric petal rings and radial harmony.
 */
private fun DrawScope.drawMandalaWatermark(
    center: Offset,
    radius: Float,
    color: Color,
    stroke: Stroke,
    fineStroke: Stroke
) {
    // Concentric guideline rings
    drawCircle(color = color, radius = radius * 0.95f, center = center, style = stroke)
    drawCircle(color = color, radius = radius * 0.72f, center = center, style = fineStroke)
    drawCircle(color = color, radius = radius * 0.48f, center = center, style = stroke)
    drawCircle(color = color, radius = radius * 0.24f, center = center, style = fineStroke)

    // Outer 12-petal lotus arcs
    val petals = 12
    for (i in 0 until petals) {
        val angle = (i * (360.0 / petals)) * (PI / 180.0)
        val nextAngle = ((i + 1) * (360.0 / petals)) * (PI / 180.0)

        val p1x = center.x + (radius * 0.72f * cos(angle)).toFloat()
        val p1y = center.y + (radius * 0.72f * sin(angle)).toFloat()
        val p2x = center.x + (radius * 0.72f * cos(nextAngle)).toFloat()
        val p2y = center.y + (radius * 0.72f * sin(nextAngle)).toFloat()

        val midAngle = ((angle + nextAngle) / 2.0)
        val tipX = center.x + (radius * 0.95f * cos(midAngle)).toFloat()
        val tipY = center.y + (radius * 0.95f * sin(midAngle)).toFloat()

        val petalPath = Path().apply {
            moveTo(p1x, p1y)
            quadraticBezierTo(tipX, tipY, p2x, p2y)
        }
        drawPath(path = petalPath, color = color, style = fineStroke)
    }

    // Inner 8-ray diamonds
    for (i in 0 until 8) {
        val angle = (i * 45.0) * (PI / 180.0)
        val x1 = center.x + (radius * 0.24f * cos(angle)).toFloat()
        val y1 = center.y + (radius * 0.24f * sin(angle)).toFloat()
        val x2 = center.x + (radius * 0.48f * cos(angle)).toFloat()
        val y2 = center.y + (radius * 0.48f * sin(angle)).toFloat()
        drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = fineStroke.width)
    }
}

