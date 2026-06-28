package com.neckangle.app.engine.angle

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.neckangle.app.engine.camera.CameraSource
import com.neckangle.app.engine.facedetect.FaceDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MonitorState(
    val angle: Float? = null,
    val postureMode: PostureMode = PostureMode.UNKNOWN,
    val isFaceDetected: Boolean = false,
    val badPostureDuration: Int = 0,
    val isBadPosture: Boolean = false,
    val isMonitoring: Boolean = false
) {
    val postureModeDisplay: String
        get() = when (postureMode) {
            PostureMode.STANDING, PostureMode.SITTING -> "站立"
            PostureMode.LYING_SUPINE -> "平躺"
            PostureMode.LYING_SIDE -> "侧卧"
            PostureMode.UNKNOWN -> "未知"
        }
}

class MonitorEngine private constructor() {

    private val _monitorState = MutableStateFlow(MonitorState())
    val monitorState: StateFlow<MonitorState> = _monitorState.asStateFlow()

    private var cameraSource: CameraSource = CameraSource.instance
    private var faceDetector: FaceDetector? = null
    private val angleCalculator = AngleCalculator()
    private var postureClassifier: PostureClassifier? = null

    private var scope: CoroutineScope? = null
    var isRunning: Boolean = false
        private set
    private var faceLostTimer: Long = 0

    fun start(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (isRunning) return
        isRunning = true

        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        postureClassifier = PostureClassifier(context).also { it.start() }
        faceDetector = FaceDetector(context)
        cameraSource = CameraSource.instance

        _monitorState.value = _monitorState.value.copy(isMonitoring = true)

        scope!!.launch { faceDetector?.initialize() }

        scope!!.launch {
            postureClassifier?.postureMode?.collect { mode ->
                _monitorState.value = _monitorState.value.copy(postureMode = mode)
            }
        }

        scope!!.launch {
            cameraSource.frameFlow.collect { frame ->
                val result = faceDetector?.detect(frame)
                if (result != null) {
                    faceLostTimer = 0
                    val angleResult = angleCalculator.calculate(result)
                    _monitorState.value = _monitorState.value.copy(
                        angle = angleResult.angleDeg,
                        isFaceDetected = true
                    )
                } else {
                    val now = System.currentTimeMillis()
                    if (faceLostTimer == 0L) faceLostTimer = now
                    if (now - faceLostTimer > 3000L) {
                        _monitorState.value = _monitorState.value.copy(
                            angle = null,
                            isFaceDetected = false
                        )
                    }
                }
            }
        }

        cameraSource.start(lifecycleOwner, previewView)
    }

    fun stop() {
        isRunning = false
        cameraSource.stop()
        faceDetector?.close()
        faceDetector = null
        postureClassifier?.stop()
        postureClassifier = null
        scope?.cancel()
        scope = null
        _monitorState.value = MonitorState()
    }

    fun updateBadPostureDuration(duration: Int) {
        _monitorState.value = _monitorState.value.copy(
            badPostureDuration = duration,
            isBadPosture = duration > 0
        )
    }

    fun setFrameRateMode(mode: String) {
        cameraSource.setFrameRateMode(mode)
    }

    companion object {
        val instance: MonitorEngine by lazy { MonitorEngine() }
    }
}
