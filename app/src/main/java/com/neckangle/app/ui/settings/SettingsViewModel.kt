package com.neckangle.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neckangle.app.NeckAngleApp
import com.neckangle.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SettingsRepository(NeckAngleApp.instance.database)

    val angleThreshold: StateFlow<Float> = repo.angleThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25f)
    val durationThreshold: StateFlow<Int> = repo.durationThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)
    val cooldownSeconds: StateFlow<Int> = repo.cooldownSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)
    val vibrationPattern: StateFlow<String> = repo.vibrationPattern
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "double_short")
    val frameRateMode: StateFlow<String> = repo.frameRateMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "balanced")

    fun updateAngleThreshold(value: Float) = viewModelScope.launch { repo.updateAngleThreshold(value) }
    fun updateDurationThreshold(value: Int) = viewModelScope.launch { repo.updateDurationThreshold(value) }
    fun updateCooldown(value: Int) = viewModelScope.launch { repo.updateCooldown(value) }
    fun updateVibrationPattern(value: String) = viewModelScope.launch { repo.updateVibrationPattern(value) }
    fun updateFrameRateMode(value: String) = viewModelScope.launch { repo.updateFrameRateMode(value) }
    fun clearToday() = viewModelScope.launch { repo.clearToday() }
    fun clearAll() = viewModelScope.launch { repo.clearAll() }
}
