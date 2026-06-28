package com.neckangle.app.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neckangle.app.NeckAngleApp
import com.neckangle.app.data.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatsUiState(
    val totalBadMinutes: Int = 0,
    val avgAngle: Float = 0f,
    val maxConsecutiveSeconds: Int = 0,
    val dailyTrend: List<DayStat> = emptyList(),
    val hasData: Boolean = false,
    val selectedDate: String = "今天"
)

data class DayStat(
    val dayLabel: String,
    val avgAngle: Float,
    val totalMinutes: Int
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = StatsRepository(NeckAngleApp.instance.database)
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadTodayStats()
    }

    private fun loadTodayStats() {
        viewModelScope.launch {
            repo.getTodayStats().collect { stats ->
                _uiState.update {
                    it.copy(
                        totalBadMinutes = stats.first,
                        avgAngle = stats.second,
                        hasData = stats.first > 0
                    )
                }
            }
        }
        viewModelScope.launch {
            repo.getWeeklyTrend().collect { trend ->
                _uiState.update { it.copy(dailyTrend = trend) }
            }
        }
    }
}
