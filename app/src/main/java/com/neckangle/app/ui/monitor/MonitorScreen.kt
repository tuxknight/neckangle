package com.neckangle.app.ui.monitor

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neckangle.app.ui.components.AngleDisplay
import com.neckangle.app.ui.components.AngleGauge
import com.neckangle.app.ui.components.PostureIndicator
import com.neckangle.app.ui.theme.Primary
import com.neckangle.app.ui.theme.SurfaceCard
import com.neckangle.app.ui.theme.TextSecondary

@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel = viewModel()
) {
    val angle by viewModel.angle.collectAsState()
    val postureMode by viewModel.postureMode.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val badPostureDuration by viewModel.badPostureDuration.collectAsState()
    val isFaceDetected by viewModel.isFaceDetected.collectAsState()
    val isBadPosture by viewModel.isBadPosture.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_START
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val animatedAngle by animateFloatAsState(
        targetValue = angle ?: 0f,
        animationSpec = tween(300)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            AngleDisplay(angle = angle)

            AngleGauge(
                currentAngle = animatedAngle,
                modifier = Modifier.size(200.dp)
            )

            PostureIndicator(
                postureMode = postureMode,
                badPostureDuration = badPostureDuration,
                isBadPosture = isBadPosture
            )

            if (!isFaceDetected && isMonitoring) {
                Text(
                    text = "请将面部对准摄像头",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.3f))

            FloatingActionButton(
                onClick = { viewModel.toggleMonitoring(context, lifecycleOwner, previewView) },
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp),
                containerColor = if (isMonitoring) SurfaceCard else Primary
            ) {
                Icon(
                    imageVector = if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isMonitoring) "停止" else "开始",
                    tint = if (isMonitoring) TextSecondary else Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.3f))
        }

        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-16).dp, y = 48.dp)
                .size(if (isMonitoring) 120.dp else 0.dp)
                .clip(CircleShape)
                .background(SurfaceCard)
        )
    }
}
