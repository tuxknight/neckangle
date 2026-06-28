package com.neckangle.app.engine.facedetect

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions
import com.neckangle.app.engine.camera.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FaceResult(
    val noseTip: Pair<Float, Float>?,
    val leftEye: Pair<Float, Float>?,
    val rightEye: Pair<Float, Float>?,
    val mouthLeft: Pair<Float, Float>?,
    val mouthRight: Pair<Float, Float>?
) {
    var leftEar: Pair<Float, Float>? = null
        private set
    var rightEar: Pair<Float, Float>? = null
        private set

    constructor(
        noseTip: Pair<Float, Float>?,
        leftEye: Pair<Float, Float>?,
        rightEye: Pair<Float, Float>?,
        mouthLeft: Pair<Float, Float>?,
        mouthRight: Pair<Float, Float>?,
        leftEar: Pair<Float, Float>?,
        rightEar: Pair<Float, Float>?
    ) : this(noseTip, leftEye, rightEye, mouthLeft, mouthRight) {
        this.leftEar = leftEar
        this.rightEar = rightEar
    }
}

class FaceDetector(private val context: Context) {

    private var faceLandmarker: FaceLandmarker? = null
    var isAvailable: Boolean = false
        private set

    companion object {
        private const val TAG = "FaceDetector"
    }

    suspend fun initialize() {
        if (faceLandmarker != null) return
        withContext(Dispatchers.IO) {
            try {
                val filesDir = File(context.filesDir, "models/face_landmarker.task")
                if (!filesDir.exists()) {
                    try {
                        context.assets.open("face_landmarker.task").use { input ->
                            filesDir.parentFile?.mkdirs()
                            filesDir.outputStream().use { output -> input.copyTo(output) }
                        }
                    } catch (_: Exception) {}
                }
                if (!filesDir.exists()) {
                    Log.w(TAG, "Face landmarker model not found in assets or filesDir")
                    return@withContext
                }

                val baseOptions = BaseOptions.builder()
                    .setModelAssetPath("face_landmarker.task")
                    .setDelegate(BaseOptions.Delegate.CPU)
                    .build()

                val options = FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumFaces(1)
                    .setMinFaceDetectionConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .build()

                faceLandmarker = FaceLandmarker.createFromOptions(context, options)
                isAvailable = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize face landmarker", e)
            }
        }
    }

    suspend fun detect(frame: Frame): FaceResult? = withContext(Dispatchers.Default) {
        try {
            if (!isAvailable || faceLandmarker == null) return@withContext null
            val bitmap = BitmapFactory.decodeByteArray(frame.data, 0, frame.data.size) ?: return@withContext null
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = faceLandmarker!!.detect(mpImage)
            if (result.faceLandmarks().isEmpty()) {
                mpImage.close()
                return@withContext null
            }
            val landmarks = result.faceLandmarks()[0]
            FaceResult(
                noseTip = landmarks.getOrNull(1)?.let { Pair(it.x(), it.y()) },
                leftEye = landmarks.getOrNull(4)?.let { Pair(it.x(), it.y()) },
                rightEye = landmarks.getOrNull(7)?.let { Pair(it.x(), it.y()) },
                mouthLeft = landmarks.getOrNull(57)?.let { Pair(it.x(), it.y()) },
                mouthRight = landmarks.getOrNull(287)?.let { Pair(it.x(), it.y()) },
                leftEar = landmarks.getOrNull(234)?.let { Pair(it.x(), it.y()) },
                rightEar = landmarks.getOrNull(454)?.let { Pair(it.x(), it.y()) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Face detection failed", e)
            null
        }
    }

    fun close() {
        faceLandmarker?.close()
        faceLandmarker = null
        isAvailable = false
    }
}
