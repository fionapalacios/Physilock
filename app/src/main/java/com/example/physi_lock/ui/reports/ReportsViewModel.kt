package com.example.physi_lock.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.AppUsageTotal
import com.example.physi_lock.data.PhysiLockDatabase
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DayUsage(val dayLabel: String, val minutes: Int, val isToday: Boolean)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val appUsageDao = db.appUsageLogDao()
    private val userConfigDao = db.userConfigurationDao()

    private val _weeklyUsage = MutableStateFlow<List<DayUsage>>(emptyList())
    val weeklyUsage: StateFlow<List<DayUsage>> = _weeklyUsage.asStateFlow()

    private val _topApps = MutableStateFlow<List<AppUsageTotal>>(emptyList())
    val topApps: StateFlow<List<AppUsageTotal>> = _topApps.asStateFlow()

    private val _insights = MutableStateFlow<List<String>>(emptyList())
    val insights: StateFlow<List<String>> = _insights.asStateFlow()

    init {
        viewModelScope.launch {
            val today = java.time.LocalDate.now()
            // Oldest to newest, ending with today
            val last7Dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val days = last7Dates.map { date ->
                val totalMs = try { appUsageDao.getTotalDurationByDateOnce(date.toString()) } catch (e: Exception) { 0L }
                DayUsage(
                    dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                    minutes = ((totalMs ?: 0L) / 60_000L).toInt(),
                    isToday = date == today
                )
            }
            _weeklyUsage.value = days
            _insights.value = buildInsights(days)

            try {
                appUsageDao.getAppTotalsByDateRange(last7Dates.first().toString(), last7Dates.last().toString())
                    .collect { totals -> _topApps.value = totals.take(5) }
            } catch (e: Exception) {
                _topApps.value = emptyList()
            }
        }
    }

    // Simple rule-based observations from real logged usage — not an ML prediction.
    private suspend fun buildInsights(days: List<DayUsage>): List<String> {
        if (days.isEmpty() || days.all { it.minutes == 0 }) return emptyList()

        val todayMinutes = days.lastOrNull { it.isToday }?.minutes ?: 0
        val avgMinutes = days.map { it.minutes }.average()
        val peakDay = days.maxByOrNull { it.minutes }
        val cfg = try { userConfigDao.getActiveConfigurationOnce() } catch (e: Exception) { null }
        val limitMinutes = ((cfg?.dailyScreenTimeThresholdMs ?: (480 * 60_000L)) / 60_000L).toInt()

        val insights = mutableListOf<String>()

        if (avgMinutes > 0) {
            val diffPct = (((todayMinutes - avgMinutes) / avgMinutes) * 100).roundToInt()
            insights += if (diffPct >= 0) {
                "Today's usage is $diffPct% above your 7-day average."
            } else {
                "Today's usage is ${-diffPct}% below your 7-day average."
            }
        }

        if (peakDay != null && peakDay.minutes > 0) {
            insights += "Highest usage this week: ${peakDay.dayLabel} at ${peakDay.minutes} min."
        }

        val diffFromLimit = todayMinutes - limitMinutes
        insights += if (diffFromLimit > 0) {
            "You're $diffFromLimit min over today's daily limit."
        } else {
            "You're ${-diffFromLimit} min under today's daily limit."
        }

        return insights
    }
}
