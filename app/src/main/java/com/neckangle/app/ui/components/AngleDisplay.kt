package com.neckangle.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.neckangle.app.ui.theme.StatusGreen
import com.neckangle.app.ui.theme.StatusRed
import com.neckangle.app.ui.theme.StatusYellow
import com.neckangle.app.ui.theme.TextSecondary

@Composable
fun AngleDisplay(
    angle: Float?,
    modifier: Modifier = Modifier
) {
    val targetColor = when {
        angle == null -> TextSecondary
        angle <= 15f -> StatusGreen
        angle <= 30f -> StatusYellow
        else -> StatusRed
    }

    val animatedColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(400))
    val displayAngle by animateFloatAsState(targetValue = angle ?: 0f, animationSpec = tween(300))

    Column(
        modifier = modifier.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (angle != null) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format("%.0f", displayAngle),
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = animatedColor,
                        fontFamily = FontFamily.Monospace
                    ),
                    maxLines = 1
                )
                Text(
                    text = "°",
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = animatedColor,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        } else {
            Text(
                text = "--°",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}
