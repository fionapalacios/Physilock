package com.example.physi_lock.ui.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.UsageStatsRepository
import com.example.physi_lock.data.UserConfiguration
import com.example.physi_lock.data.categoryTotals
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MS_PER_HOUR = 3_600_000L

/**
 * Ported from the teammate's sprint-2-ui-navigation branch. Only "Total Daily Screen Time"'s
 * goal maps onto a real column (UserConfiguration.dailyScreenTimeThresholdMs); per-category
 * goal targets (Social Media, Entertainment, Gaming) still have no schema field to persist a
 * user-chosen target, so those stay UI-local (same as the total goal's slider-only presets
 * before a target is saved). What changed 2026-08-25: the "current usage" numbers next to
 * every goal — previously hardcoded (5.55h, 3.23h, 41.2h/week, etc.) — are now real, computed
 * the same way ReportsViewModel's Category Breakdown card is (7-day UsageStatsManager totals
 * bucketed by each app's Admin-curated category, see [categoryTotals]).
 */
class UsageGoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val userConfigDao = PhysiLockDatabase.getInstance(application).userConfigurationDao()
    private val appCategoryDao = PhysiLockDatabase.getInstance(application).appCategoryDao()
    private val usageStatsRepository = UsageStatsRepository(application)

    val dailyLimitHours: StateFlow<Float> = userConfigDao.getActiveConfiguration()
        .map { (it?.dailyScreenTimeThresholdMs ?: (7 * MS_PER_HOUR)) / MS_PER_HOUR.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 7f)

    // Weekly Screen Time Goal card — real 7-day total.
    private val _weeklyTotalHours = MutableStateFlow(0f)
    val weeklyTotalHours: StateFlow<Float> = _weeklyTotalHours.asStateFlow()

    // Total Daily Screen Time card — real today-only total (comparable against a daily goal).
    private val _todayTotalHours = MutableStateFlow(0f)
    val todayTotalHours: StateFlow<Float> = _todayTotalHours.asStateFlow()

    // Per-category goal cards (Social Media/Entertainment/Gaming) — real today-only usage,
    // keyed by AppCategoryType.SOCIAL_MEDIA etc.
    private val _todayCategoryHours = MutableStateFlow<Map<String, Float>>(emptyMap())
    val todayCategoryHours: StateFlow<Map<String, Float>> = _todayCategoryHours.asStateFlow()

    init {
        viewModelScope.launch {
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val weeklyPackageTotals = mutableMapOf<String, Long>()
            var todayPackageTotals: Map<String, Long> = emptyMap()
            (6 downTo 0).forEach { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = if (date == today) {
                    System.currentTimeMillis()
                } else {
                    date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                }
                val usage = try {
                    usageStatsRepository.getUsageForRange(startMillis, endMillis)
                } catch (e: Exception) {
                    emptyList()
                }
                usage.forEach { weeklyPackageTotals[it.packageName] = (weeklyPackageTotals[it.packageName] ?: 0L) + it.totalTimeMs }
                if (date == today) {
                    todayPackageTotals = usage.associate { it.packageName to it.totalTimeMs }
                }
            }

            _weeklyTotalHours.value = weeklyPackageTotals.values.sum() / MS_PER_HOUR.toFloat()
            _todayTotalHours.value = todayPackageTotals.values.sum() / MS_PER_HOUR.toFloat()

            val byCategory = categoryTotals(todayPackageTotals, appCategoryDao)
            _todayCategoryHours.value = byCategory.mapValues { (_, ms) -> ms / MS_PER_HOUR.toFloat() }
        }
    }

    fun setDailyLimitHours(hours: Float) {
        viewModelScope.launch {
            val current = userConfigDao.getActiveConfigurationOnce() ?: UserConfiguration()
            userConfigDao.upsert(
                current.copy(
                    dailyScreenTimeThresholdMs = (hours * MS_PER_HOUR).toLong(),
                    lastUpdatedTime = System.currentTimeMillis()
                )
            )
        }
    }
}
