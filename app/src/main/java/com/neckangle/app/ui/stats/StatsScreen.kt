package com.neckangle.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neckangle.app.ui.components.StatusCard
import com.neckangle.app.ui.theme.Primary
import com.neckangle.app.ui.theme.SurfaceCard
import com.neckangle.app.ui.theme.TextSecondary

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.ChevronLeft, "前一天")
            }
            Text(uiState.selectedDate, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { }) {
                Icon(Icons.Default.ChevronRight, "后一天")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!uiState.hasData) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "开始监测后，这里将展示你的颈椎姿势数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusCard(
                    Icons.Outlined.AccessTime, "总低头时长",
                    "${uiState.totalBadMinutes} 分钟",
                    Modifier.weight(1f)
                )
                StatusCard(
                    Icons.Outlined.ShowChart, "平均角度",
                    String.format("%.1f°", uiState.avgAngle),
                    Modifier.weight(1f)
                )
                StatusCard(
                    Icons.Outlined.Timer, "最长连续",
                    "${uiState.maxConsecutiveSeconds}s",
                    Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "7日趋势",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (uiState.dailyTrend.isNotEmpty()) {
                WeeklyTrendChart(
                    dailyTrend = uiState.dailyTrend,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun WeeklyTrendChart(
    dailyTrend: List<DayStat>,
    modifier: Modifier = Modifier
) {
    val surfaceCard = SurfaceCard
    Canvas(modifier = modifier) {
        if (dailyTrend.isEmpty()) return@Canvas

        val padding = 40f
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding * 2

        val maxAngle = dailyTrend.maxOfOrNull { it.avgAngle }?.coerceAtLeast(10f) ?: 10f
        val minAngle = 0f

        val points = dailyTrend.mapIndexed { index, stat ->
            val x = padding + (index.toFloat() / (dailyTrend.size - 1).coerceAtLeast(1)) * chartWidth
            val y = padding + chartHeight - ((stat.avgAngle - minAngle) / (maxAngle - minAngle)) * chartHeight
            Offset(x, y)
        }

        if (points.size >= 2) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val cx = (prev.x + curr.x) / 2f
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
            }
            drawPath(path, color = Primary, style = Stroke(width = 3f, cap = StrokeCap.Round))
        }

        points.forEach { point ->
            drawCircle(color = Primary, radius = 5f, center = point)
            drawCircle(color = Color.White, radius = 3f, center = point)
        }

        dailyTrend.forEachIndexed { index, stat ->
            val x = padding + (index.toFloat() / (dailyTrend.size - 1).coerceAtLeast(1)) * chartWidth
            drawContext.canvas.nativeCanvas.drawText(
                stat.dayLabel,
                x - 16f,
                size.height - 4f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#B0B0C0")
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.LEFT
                }
            )
        }
    }
}
