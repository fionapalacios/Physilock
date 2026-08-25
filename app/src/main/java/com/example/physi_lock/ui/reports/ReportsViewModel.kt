package com.example.physi_lock.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.AppCategoryType
import com.example.physi_lock.data.AppUsageTotal
import com.example.physi_lock.data.ExcessiveUsagePredictionLog
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.UsageStatsRepository
import com.example.physi_lock.data.categoryTotals
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayUsage(val dayLabel: String, val minutes: Int, val isToday: Boolean)

/** Ported concept from the teammate's ReportsScreen "Category Breakdown" card — real data
 *  here, not their static mock: durations come from the same 7-day UsageStatsManager totals
 *  as [ReportsViewModel.topApps], bucketed by each app's Admin-curated category (see
 *  AdminCategoriesSection). Apps never categorized by Admin fall into "Other", same as
 *  AppCategoryType's own default — not fabricated, just uncategorized. */
data class CategoryUsage(
    val category: String,
    val label: String,
    val emoji: String,
    val durationMs: Long,
    val percent: Int
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val userConfigDao = db.userConfigurationDao()
    private val usageStatsRepository = UsageStatsRepository(application)

    // Module 2 (AI-Based Behavior Analysis): Logistic Regression II's real output log
    // (see AppMonitorService.checkExcessiveUsagePrediction / ml/README.md) — unlike
    // `insights` below, this IS a genuine ML prediction, not a rule-based observation.
    val todaysPredictions: StateFlow<List<ExcessiveUsagePredictionLog>> =
        db.excessiveUsagePredictionLogDao().getPredictionsByDate(LocalDate.now().toString())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _weeklyUsage = MutableStateFlow<List<DayUsage>>(emptyList())
    val weeklyUsage: StateFlow<List<DayUsage>> = _weeklyUsage.asStateFlow()

    private val _topApps = MutableStateFlow<List<AppUsageTotal>>(emptyList())
    val topApps: StateFlow<List<AppUsageTotal>> = _topApps.asStateFlow()

    private val _categoryBreakdown = MutableStateFlow<List<CategoryUsage>>(emptyList())
    val categoryBreakdown: StateFlow<List<CategoryUsage>> = _categoryBreakdown.asStateFlow()

    private val _insights = MutableStateFlow<List<String>>(emptyList())
    val insights: StateFlow<List<String>> = _insights.asStateFlow()

    private val _dailyLimitMinutes = MutableStateFlow(480)
    val dailyLimitMinutes: StateFlow<Int> = _dailyLimitMinutes.asStateFlow()

    init {
        viewModelScope.launch {
            val today = LocalDate.now()
            // Oldest to newest, ending with today
            val last7Dates = (6 downTo 0).map { today.minusDays(it.toLong()) }

            // Same UsageStatsManager source Home uses (see HomeViewModel), not AppUsageLog —
            // that only reflects usage since AppMonitorService started watching, not real
            // historical totals. One query per day rather than a single 7-day range query,
            // since UsageStatsManager's multi-day aggregation isn't reliable enough to trust
            // (see UsageStatsRepository.getUsageForRange kdoc); totals are folded client-side.
            val packageTotals = mutableMapOf<String, Long>()
            val days = last7Dates.map { date ->
                val (dayStart, dayEnd) = dayBoundsMillis(date, today)
                val usage = try {
                    usageStatsRepository.getUsageForRange(dayStart, dayEnd)
                } catch (e: Exception) {
                    emptyList()
                }
                usage.forEach { packageTotals[it.packageName] = (packageTotals[it.packageName] ?: 0L) + it.totalTimeMs }
                DayUsage(
                    dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    minutes = (usage.sumOf { it.totalTimeMs } / 60_000L).toInt(),
                    isToday = date == today
                )
            }
            _weeklyUsage.value = days
            _insights.value = buildInsights(days)

            _topApps.value = packageTotals.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { (packageName, totalMs) ->
                    AppUsageTotal(
                        packageName = packageName,
                        appName = usageStatsRepository.getAppLabel(packageName),
                        totalDurationMs = totalMs
                    )
                }

            _categoryBreakdown.value = buildCategoryBreakdown(packageTotals)
        }
    }

    private suspend fun buildCategoryBreakdown(packageTotals: Map<String, Long>): List<CategoryUsage> {
        val totalsByCategory = categoryTotals(packageTotals, db.appCategoryDao())
        val grandTotal = totalsByCategory.values.sum()
        if (grandTotal <= 0) return emptyList()

        return totalsByCategory.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { (category, ms) ->
                CategoryUsage(
                    category = category,
                    label = AppCategoryType.label(category),
                    emoji = emojiForCategory(category),
                    durationMs = ms,
                    percent = ((ms.toFloat() / grandTotal) * 100).roundToInt()
                )
            }
    }

    private fun emojiForCategory(category: String): String = when (category) {
        AppCategoryType.SOCIAL_MEDIA -> "📱"
        AppCategoryType.ENTERTAINMENT -> "🎬"
        AppCategoryType.GAMES -> "🎮"
        AppCategoryType.PRODUCTIVITY -> "📋"
        else -> "📦"
    }

    /** Midnight-to-midnight for past days; midnight-to-now for today. */
    private fun dayBoundsMillis(date: LocalDate, today: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = if (date == today) {
            System.currentTimeMillis()
        } else {
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        }
        return startMillis to endMillis
    }

    // Simple rule-based observations from real logged usage — not an ML prediction.
    private suspend fun buildInsights(days: List<DayUsage>): List<String> {
        val cfg = try { userConfigDao.getActiveConfigurationOnce() } catch (e: Exception) { null }
        val limitMinutes = ((cfg?.dailyScreenTimeThresholdMs ?: (480 * 60_000L)) / 60_000L).toInt()
        _dailyLimitMinutes.value = limitMinutes

        if (days.isEmpty() || days.all { it.minutes == 0 }) return emptyList()

        val todayMinutes = days.lastOrNull { it.isToday }?.minutes ?: 0
        val avgMinutes = days.map { it.minutes }.average()
        val peakDay = days.maxByOrNull { it.minutes }

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
