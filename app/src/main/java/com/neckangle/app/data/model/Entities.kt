package com.neckangle.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class PostureRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val angle: Float,
    val postureMode: String,
    val isBadPosture: Boolean,
    val durationSeconds: Int
)

@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val angleThreshold: Float = 25f,
    val durationThreshold: Int = 15,
    val cooldownSeconds: Int = 30,
    val vibrationPattern: String = "double_short",
    val frameRateMode: String = "balanced"
)
