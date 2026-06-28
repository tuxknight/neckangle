package com.neckangle.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.AirlineSeatFlat
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neckangle.app.ui.theme.StatusGreen
import com.neckangle.app.ui.theme.TextSecondary

@Composable
fun PostureIndicator(
    postureMode: String,
    badPostureDuration: Int,
    isBadPosture: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, label) = when (postureMode) {
        "站立" -> Icons.Outlined.DirectionsWalk to "站立"
        "坐姿" -> Icons.Outlined.AccessibilityNew to "坐姿"
        "平躺" -> Icons.Outlined.AirlineSeatFlat to "平躺"
        "侧卧" -> Icons.Outlined.Bed to "侧卧"
        else -> Icons.Outlined.AccessibilityNew to "未知"
    }

    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextSecondary.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)

        if (isBadPosture && badPostureDuration > 0) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "已低头", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(
                text = " $badPostureDuration ",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(text = "秒", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        } else if (!isBadPosture) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "姿势良好",
                style = MaterialTheme.typography.bodyMedium,
                color = StatusGreen
            )
        }
    }
}
