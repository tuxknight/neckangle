package com.neckangle.app.engine.alert

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.neckangle.app.data.repository.SettingsRepository
import com.neckangle.app.engine.angle.MonitorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

data class AlertEvent(
    val angle: Float,
    val duration: Int,
    val timestamp: Long
)

class AlertEngine(private val context: Context) {

    private val _alertFlow = MutableSharedFlow<AlertEvent>(replay = 0, extraBufferCapacity = 2)
    val alertFlow: SharedFlow<AlertEvent> = _alertFlow

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var scope: CoroutineScope? = null
    private var angleThreshold: Float = 25f
    private var durationThreshold: Int = 15
    private var cooldownSeconds: Int = 30
    private var vibrationPattern: String = "double_short"
    private var consecutiveBadSeconds: Int = 0
    private var lastAlertTime: Long = 0
    private var lastTickTime: Long = 0
    private var isActive: Boolean = false

    fun start(settingsRepo: SettingsRepository) {
        if (isActive) return
        isActive = true
        consecutiveBadSeconds = 0
        lastAlertTime = 0
        lastTickTime = System.currentTimeMillis()

        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope!!.launch { settingsRepo.angleThreshold.collect { angleThreshold = it } }
        scope!!.launch { settingsRepo.durationThreshold.collect { durationThreshold = it } }
        scope!!.launch { settingsRepo.cooldownSeconds.collect { cooldownSeconds = it } }
        scope!!.launch { settingsRepo.vibrationPattern.collect { vibrationPattern = it } }
        scope!!.launch {
            MonitorEngine.instance.monitorState.collect { state -> evaluate(state) }
        }
    }

    fun stop() {
        isActive = false
        scope?.cancel()
        scope = null
    }

    private fun evaluate(state: com.neckangle.app.engine.angle.MonitorState) {
        val angle = state.angle ?: run { consecutiveBadSeconds = 0; return }
        val now = System.currentTimeMillis()
        val elapsed = ((now - lastTickTime) / 1000f).toInt().coerceAtLeast(1)
        lastTickTime = now

        if (angle > angleThreshold) {
            consecutiveBadSeconds += elapsed
        } else {
            consecutiveBadSeconds = 0
        }

        MonitorEngine.instance.updateBadPostureDuration(consecutiveBadSeconds)

        if (consecutiveBadSeconds >= durationThreshold) {
            val cooldownMs = cooldownSeconds * 1000L
            if (now - lastAlertTime >= cooldownMs) {
                lastAlertTime = now
                fireAlert(state)
            }
        }
    }

    private fun fireAlert(state: com.neckangle.app.engine.angle.MonitorState) {
        scope?.launch {
            _alertFlow.emit(AlertEvent(
                angle = state.angle ?: 0f,
                duration = consecutiveBadSeconds,
                timestamp = System.currentTimeMillis()
            ))
        }
        vibrate(vibrationPattern)
    }

    private fun vibrate(pattern: String) {
        val effect: VibrationEffect = when (pattern) {
            "single" -> VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
            "double_short" -> VibrationEffect.createWaveform(
                longArrayOf(0, 200, 200, 200),
                intArrayOf(0, 255, 0, 255), -1
            )
            "long" -> VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
            "pulse" -> VibrationEffect.createWaveform(
                longArrayOf(0, 100, 100, 100, 100, 100),
                intArrayOf(0, 255, 0, 255, 0, 255), -1
            )
            else -> VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        try { vibrator.vibrate(effect) } catch (_: Exception) {}
    }

    fun dispose() {
        stop()
        vibrator.cancel()
    }
}
