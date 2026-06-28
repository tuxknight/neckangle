package com.neckangle.app.ui.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neckangle.app.engine.angle.MonitorEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonitorViewModel : ViewModel() {
    private val engine = MonitorEngine.instance

    val angle: StateFlow<Float?> = engine.monitorState
        .map { it.angle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val postureMode: StateFlow<String> = engine.monitorState
        .map { it.postureModeDisplay }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "未知")

    val isMonitoring: StateFlow<Boolean> = engine.monitorState
        .map { it.isMonitoring }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val badPostureDuration: StateFlow<Int> = engine.monitorState
        .map { it.badPostureDuration }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isFaceDetected: StateFlow<Boolean> = engine.monitorState
        .map { it.isFaceDetected }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBadPosture: StateFlow<Boolean> = engine.monitorState
        .map { it.isBadPosture }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleMonitoring() {
        viewModelScope.launch {
            if (engine.isRunning) engine.stop() else engine.start()
        }
    }
}
