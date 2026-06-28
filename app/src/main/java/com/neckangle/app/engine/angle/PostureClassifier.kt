package com.neckangle.app.engine.angle

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

enum class PostureMode {
    STANDING, SITTING, LYING_SUPINE, LYING_SIDE, UNKNOWN
}

class PostureClassifier(private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _postureMode = MutableStateFlow(PostureMode.UNKNOWN)
    val postureMode: StateFlow<PostureMode> = _postureMode

    private val history = ArrayDeque<FloatArray>(10)
    private var sampleCount = 0

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val alpha = 0.8f
        val last = history.lastOrNull()
        val gravity = floatArrayOf(
            alpha * (last?.get(0) ?: 0f) + (1 - alpha) * event.values[0],
            alpha * (last?.get(1) ?: 0f) + (1 - alpha) * event.values[1],
            alpha * (last?.get(2) ?: 0f) + (1 - alpha) * event.values[2]
        )

        history.addLast(gravity)
        if (history.size > 10) history.removeFirst()

        sampleCount++
        if (sampleCount % 10 == 0) {
            val avg = floatArrayOf(
                history.map { it[0] }.average().toFloat(),
                history.map { it[1] }.average().toFloat(),
                history.map { it[2] }.average().toFloat()
            )
            _postureMode.value = classify(avg)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun classify(gravity: FloatArray): PostureMode {
        val ax = abs(gravity[0])
        val ay = abs(gravity[1])
        val az = abs(gravity[2])

        return when {
            az > 8 && ax < 3 && ay < 3 -> PostureMode.STANDING
            ay > 8 && az < 3 -> PostureMode.LYING_SUPINE
            ax > 8 && ay < 3 && az < 3 -> PostureMode.LYING_SIDE
            else -> PostureMode.UNKNOWN
        }
    }

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        history.clear()
    }
}
