package com.neckangle.app.engine.angle

import com.neckangle.app.engine.camera.Frame
import com.neckangle.app.engine.facedetect.FaceResult
import kotlin.math.abs

data class AngleResult(
    val angleDeg: Float,
    val confidence: Float
)

class AngleCalculator {

    fun calculate(faceResult: FaceResult): AngleResult {
        val facePitchDeg = computeFacePitch(faceResult)
        val confidence = computeConfidence(faceResult)
        val clampedAngle = abs(facePitchDeg).coerceIn(0f, 60f)
        return AngleResult(angleDeg = clampedAngle, confidence = confidence)
    }

    fun calculate(faceResult: FaceResult, phonePitchDeg: Float): AngleResult {
        val facePitchDeg = computeFacePitch(faceResult)
        val confidence = computeConfidence(faceResult)
        val headAngle = abs(facePitchDeg - phonePitchDeg)
        val clampedAngle = headAngle.coerceIn(0f, 60f)
        return AngleResult(angleDeg = clampedAngle, confidence = confidence)
    }

    private fun computeFacePitch(faceResult: FaceResult): Float {
        val nose = faceResult.noseTip
        val leftEye = faceResult.leftEye
        val rightEye = faceResult.rightEye

        if (leftEye != null && rightEye != null && nose != null) {
            val eyeY = (leftEye.second + rightEye.second) / 2f
            val noseY = nose.second
            val eyeNoseRatio = noseY - eyeY
            return if (eyeNoseRatio > 0) {
                ((eyeNoseRatio / 0.15f) * 45f).coerceIn(-60f, 60f)
            } else 0f
        }

        if (nose != null && (faceResult.leftEar != null || faceResult.rightEar != null)) {
            val ear = faceResult.leftEar ?: faceResult.rightEar!!
            val offset = nose.second - ear.second
            return ((offset / 0.1f) * 30f).coerceIn(-60f, 60f)
        }

        if (nose != null) {
            val centered = (nose.second - 0.45f) / 0.1f
            return (centered * 30f).coerceIn(-60f, 60f)
        }

        return 0f
    }

    private fun computeConfidence(faceResult: FaceResult): Float {
        var score = 0f
        var total = 0f
        if (faceResult.noseTip != null) { score += 1f; total += 1f }
        if (faceResult.leftEye != null) { score += 1f; total += 1f }
        if (faceResult.rightEye != null) { score += 1f; total += 1f }
        if (faceResult.leftEar != null) { score += 0.5f; total += 0.5f }
        if (faceResult.rightEar != null) { score += 0.5f; total += 0.5f }
        return if (total > 0) (score / total).coerceIn(0f, 1f) else 0f
    }
}
