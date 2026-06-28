package com.neckangle.app.data.repository

import com.neckangle.app.data.db.AppDatabase
import com.neckangle.app.ui.stats.DayStat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class StatsRepository(database: AppDatabase) {
    private val dao = database.postureRecordDao()

    fun getTodayStats(): Flow<Pair<Int, Float>> {
        val todayStart = getDayStartMillis(0)
        val todayEnd = getDayEndMillis(0)
        return dao.getRecordsInRange(todayStart, todayEnd).map { records ->
            if (records.isEmpty()) return@map Pair(0, 0f)
            val badRecords = records.filter { it.isBadPosture }
            val totalMinutes = badRecords.sumOf { it.durationSeconds } / 60
            val avgAngle = if (records.isNotEmpty()) records.map { it.angle }.average().toFloat() else 0f
            Pair(totalMinutes, avgAngle)
        }
    }

    fun getWeeklyTrend(): Flow<List<DayStat>> {
        val sevenDaysAgo = getDayStartMillis(-6)
        val todayEnd = getDayEndMillis(0)
        return dao.getRecordsInRange(sevenDaysAgo, todayEnd).map { records ->
            (0..6).map { daysAgo ->
                val start = getDayStartMillis(-daysAgo)
                val end = getDayEndMillis(-daysAgo)
                val dayRecords = records.filter { it.timestamp in start..end }
                DayStat(
                    dayLabel = when (daysAgo) {
                        0 -> "今天"
                        1 -> "昨天"
                        else -> {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
                            "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
                        }
                    },
                    avgAngle = if (dayRecords.isNotEmpty()) dayRecords.map { it.angle }.average().toFloat() else 0f,
                    totalMinutes = if (dayRecords.isNotEmpty()) dayRecords.filter { it.isBadPosture }.sumOf { it.durationSeconds } / 60 else 0
                )
            }.reversed()
        }
    }

    suspend fun clearToday() {
        val start = getDayStartMillis(0)
        val end = getDayEndMillis(0)
        dao.deleteInRange(start, end)
    }

    suspend fun clearAll() = dao.deleteAll()

    private fun getDayStartMillis(daysAgo: Int): Long {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, daysAgo) }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getDayEndMillis(daysAgo: Int): Long {
        return getDayStartMillis(daysAgo) + 24 * 60 * 60 * 1000 - 1
    }
}
