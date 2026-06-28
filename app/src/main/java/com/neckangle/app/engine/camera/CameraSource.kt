package com.neckangle.app.engine.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

data class Frame(
    val data: ByteArray,
    val rotation: Int,
    val timestamp: Long
)

class CameraSource {

    private val _frameFlow = MutableSharedFlow<Frame>(replay = 0, extraBufferCapacity = 1)
    val frameFlow: SharedFlow<Frame> = _frameFlow

    private var imageAnalysis: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var frameRateMode: String = "balanced"
    private var lastFrameTime: Long = 0
    private var isActive: Boolean = false
    private var scope: CoroutineScope? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null

    private val targetResolution = android.util.Size(640, 480)

    fun start(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (isActive) return
        isActive = true
        this.lifecycleOwner = lifecycleOwner
        this.previewView = previewView
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindUseCases()
        }, ContextCompat.getMainExecutor(previewView.context))
    }

    private fun bindUseCases() {
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        provider.unbindAll()

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        val preview = Preview.Builder()
            .setTargetResolution(targetResolution)
            .build()
            .also { it.setSurfaceProvider(previewView!!.surfaceProvider) }

        val fps = when (frameRateMode) {
            "power_save" -> 3
            "precise" -> 30
            else -> 15
        }

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(targetResolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(ContextCompat.getMainExecutor(previewView!!.context)) { imageProxy ->
                    if (!isActive) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val now = System.currentTimeMillis()
                    val minInterval = 1000L / fps
                    if (now - lastFrameTime < minInterval) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    lastFrameTime = now

                    val frame = imageProxyToFrame(imageProxy)
                    imageProxy.close()

                    if (frame != null) {
                        scope?.launch { _frameFlow.emit(frame) }
                    }
                }
            }

        try {
            provider.bindToLifecycle(owner, cameraSelector, preview, imageAnalysis)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        isActive = false
        cameraProvider?.unbindAll()
        scope?.cancel()
        scope = null
        lifecycleOwner = null
        previewView = null
    }

    fun setFrameRateMode(mode: String) {
        frameRateMode = mode
        if (isActive) bindUseCases()
    }

    fun getPreviewView(): PreviewView? = previewView

    @androidx.camera.core.ExperimentalGetImage
    private fun imageProxyToFrame(imageProxy: ImageProxy): Frame? {
        val bitmap = imageProxyToBitmap(imageProxy) ?: return null
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Frame(
            data = stream.toByteArray(),
            rotation = imageProxy.imageInfo.rotationDegrees,
            timestamp = imageProxy.imageInfo.timestamp
        )
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val planes = imageProxy.planes
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]
        val yBuffer: ByteBuffer = yPlane.buffer
        val uBuffer: ByteBuffer = uPlane.buffer
        val vBuffer: ByteBuffer = vPlane.buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 80, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val matrix = Matrix()
        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        if (imageProxy.imageInfo.rotationDegrees == 0 || imageProxy.imageInfo.rotationDegrees == 180) {
            matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        } else {
            matrix.postScale(-1f, 1f, bitmap.height / 2f, bitmap.width / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    companion object {
        @Volatile
        private var INSTANCE: CameraSource? = null
        val instance: CameraSource
            get() = INSTANCE ?: synchronized(this) { INSTANCE ?: CameraSource().also { INSTANCE = it } }
    }
}
