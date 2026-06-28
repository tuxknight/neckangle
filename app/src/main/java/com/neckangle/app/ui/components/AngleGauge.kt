package com.neckangle.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.neckangle.app.ui.theme.StatusGreen
import com.neckangle.app.ui.theme.StatusRed
import com.neckangle.app.ui.theme.StatusYellow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AngleGauge(
    currentAngle: Float,
    maxAngle: Float = 60f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 24.dp.toPx()
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = (canvasWidth.coerceAtMost(canvasHeight) / 2) - strokeWidth
        val centerX = canvasWidth / 2
        val centerY = canvasHeight * 0.85f

        val arcSize = Size(radius * 2, radius * 2)
        val arcTopLeft = Offset(centerX - radius, centerY - radius * 0.7f)

        val startAngle = 135f
        val sweepAngle = 270f

        val greenEnd = startAngle + (15f / maxAngle) * sweepAngle
        val yellowEnd = startAngle + (30f / maxAngle) * sweepAngle

        drawArc(StatusGreen, startAngle, greenEnd - startAngle, false, arcTopLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
        drawArc(StatusYellow, greenEnd, yellowEnd - greenEnd, false, arcTopLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Butt))
        drawArc(StatusRed, yellowEnd, startAngle + sweepAngle - yellowEnd, false, arcTopLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))

        val needleAngle = startAngle + (currentAngle / maxAngle.coerceAtLeast(1f)) * sweepAngle
        val needleRad = Math.toRadians(needleAngle.toDouble())
        val needleLength = radius * 0.7f
        val needleX = centerX + (needleLength * cos(needleRad)).toFloat()
        val needleY = centerY + (needleLength * sin(needleRad)).toFloat()

        val needleColor = when {
            currentAngle <= 15f -> StatusGreen
            currentAngle <= 30f -> StatusYellow
            else -> StatusRed
        }

        drawCircle(needleColor, 6.dp.toPx(), Offset(centerX, centerY))
        drawLine(needleColor, Offset(centerX, centerY), Offset(needleX, needleY), 4.dp.toPx(), cap = StrokeCap.Round)
    }
}
