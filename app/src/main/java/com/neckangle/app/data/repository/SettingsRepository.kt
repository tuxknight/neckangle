package com.neckangle.app.data.repository

import com.neckangle.app.data.db.AppDatabase
import com.neckangle.app.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(database: AppDatabase) {
    private val dao = database.settingsDao()

    val angleThreshold: Flow<Float> = dao.getSettings().map { it?.angleThreshold ?: 25f }
    val durationThreshold: Flow<Int> = dao.getSettings().map { it?.durationThreshold ?: 15 }
    val cooldownSeconds: Flow<Int> = dao.getSettings().map { it?.cooldownSeconds ?: 30 }
    val vibrationPattern: Flow<String> = dao.getSettings().map { it?.vibrationPattern ?: "double_short" }
    val frameRateMode: Flow<String> = dao.getSettings().map { it?.frameRateMode ?: "balanced" }

    suspend fun updateAngleThreshold(value: Float) {
        dao.insertOrUpdate(dao.getSettingsOnce()?.copy(angleThreshold = value) ?: AppSettings(angleThreshold = value))
    }
    suspend fun updateDurationThreshold(value: Int) {
        dao.insertOrUpdate(dao.getSettingsOnce()?.copy(durationThreshold = value) ?: AppSettings(durationThreshold = value))
    }
    suspend fun updateCooldown(value: Int) {
        dao.insertOrUpdate(dao.getSettingsOnce()?.copy(cooldownSeconds = value) ?: AppSettings(cooldownSeconds = value))
    }
    suspend fun updateVibrationPattern(value: String) {
        dao.insertOrUpdate(dao.getSettingsOnce()?.copy(vibrationPattern = value) ?: AppSettings(vibrationPattern = value))
    }
    suspend fun updateFrameRateMode(value: String) {
        dao.insertOrUpdate(dao.getSettingsOnce()?.copy(frameRateMode = value) ?: AppSettings(frameRateMode = value))
    }

    suspend fun clearToday() {}
    suspend fun clearAll() {}
}
