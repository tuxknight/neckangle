package com.neckangle.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neckangle.app.ui.theme.Primary
import com.neckangle.app.ui.theme.SurfaceCard
import com.neckangle.app.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val angleThreshold by viewModel.angleThreshold.collectAsState()
    val durationThreshold by viewModel.durationThreshold.collectAsState()
    val cooldownSeconds by viewModel.cooldownSeconds.collectAsState()
    val vibrationPattern by viewModel.vibrationPattern.collectAsState()
    val frameRateMode by viewModel.frameRateMode.collectAsState()

    var showClearTodayDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val vibrationOptions = listOf(
        "single" to "单次",
        "double_short" to "两次短震",
        "long" to "长震",
        "pulse" to "脉冲"
    )

    val frameRateOptions = listOf(
        "power_save" to "省电",
        "balanced" to "均衡",
        "precise" to "精准"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SectionHeader("提醒设置")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingSlider("角度阈值", angleThreshold, 10f..45f, 34, "°") {
                    viewModel.updateAngleThreshold(it)
                }
                SettingSlider("持续时长", durationThreshold.toFloat(), 5f..120f, 22, "秒") {
                    viewModel.updateDurationThreshold(it.roundToInt())
                }
                SettingSlider("冷却时间", cooldownSeconds.toFloat(), 10f..120f, 21, "秒") {
                    viewModel.updateCooldown(it.roundToInt())
                }
                SettingLabel("震动模式")
                SegmentedChoice(vibrationOptions, vibrationPattern) {
                    viewModel.updateVibrationPattern(it)
                }
            }
        }

        SectionHeader("性能")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingLabel("采集频率")
                SegmentedChoice(frameRateOptions, frameRateMode) {
                    viewModel.updateFrameRateMode(it)
                }
            }
        }

        SectionHeader("数据")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingButton("清除今天数据") { showClearTodayDialog = true }
                SettingButton("清除所有数据") { showClearAllDialog = true }
            }
        }

        SectionHeader("关于")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingRow("版本号", "v1.0")
                Text(
                    "本应用所有数据均在本地处理，不会上传或保存您的面部图像",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showClearTodayDialog) {
        AlertDialog(
            onDismissRequest = { showClearTodayDialog = false },
            title = { Text("清除今天数据") },
            text = { Text("确定要清除今天的所有数据吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearToday()
                    showClearTodayDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showClearTodayDialog = false }) { Text("取消") }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("清除所有数据") },
            text = { Text("确定要清除所有数据吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearAllDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineMedium,
        color = Primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueSuffix: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${value.roundToInt()}$valueSuffix",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun SegmentedChoice(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = selected == key
            Button(
                onClick = { onSelect(key) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Primary else SurfaceCard.copy(alpha = 0.5f)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp, vertical = 8.dp
                )
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SettingButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard.copy(alpha = 0.5f))
    ) {
        Text(label, color = TextSecondary)
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
