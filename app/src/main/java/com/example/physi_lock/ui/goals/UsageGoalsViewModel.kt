package com.example.physi_lock.ui.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.CategoryGoal
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
 * Ported from the teammate's sprint-2-ui-navigation branch. As of 2026-08-25, every goal
 * *target* is real and persisted, not just "current usage": "Total Daily Screen Time" maps
 * onto UserConfiguration.dailyScreenTimeThresholdMs (already existed), the weekly goal maps
 * onto the new UserConfiguration.weeklyScreenTimeGoalMs column, and the 3 per-category goals
 * (Social Media, Entertainment, Gaming) map onto the new CategoryGoal table, keyed by
 * AppCategoryType. A missing CategoryGoal row just means the user hasn't customized that
 * category's goal yet -- [categoryGoalHours] falls back to the same preset defaults the
 * ported UI originally hardcoded. "Current usage" numbers are computed the same way
 * ReportsViewModel's Category Breakdown card is (7-day UsageStatsManager totals bucketed by
 * each app's Admin-curated category, see [categoryTotals]).
 */
class UsageGoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val userConfigDao = PhysiLockDatabase.getInstance(application).userConfigurationDao()
    private val appCategoryDao = PhysiLockDatabase.getInstance(application).appCategoryDao()
    private val categoryGoalDao = PhysiLockDatabase.getInstance(application).categoryGoalDao()
    private val usageStatsRepository = UsageStatsRepository(application)

    val dailyLimitHours: StateFlow<Float> = userConfigDao.getActiveConfiguration()
        .map { (it?.dailyScreenTimeThresholdMs ?: (7 * MS_PER_HOUR)) / MS_PER_HOUR.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 7f)

    val weeklyGoalHours: StateFlow<Float> = userConfigDao.getActiveConfiguration()
        .map { (it?.weeklyScreenTimeGoalMs ?: (35 * MS_PER_HOUR)) / MS_PER_HOUR.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 35f)

    // Real persisted per-category targets, keyed by AppCategoryType. A category missing
    // from this map hasn't been customized yet -- callers should fall back to their own
    // preset default (see CategoryGoalItem.initialGoalHours in UsageGoalsScreen).
    val categoryGoalHours: StateFlow<Map<String, Float>> = categoryGoalDao.getAll()
        .map { goals -> goals.associate { it.categoryType to it.targetMs / MS_PER_HOUR.toFloat() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

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

    fun setWeeklyGoalHours(hours: Float) {
        viewModelScope.launch {
            val current = userConfigDao.getActiveConfigurationOnce() ?: UserConfiguration()
            userConfigDao.upsert(
                current.copy(
                    weeklyScreenTimeGoalMs = (hours * MS_PER_HOUR).toLong(),
                    lastUpdatedTime = System.currentTimeMillis()
                )
            )
        }
    }

    fun setCategoryGoalHours(categoryType: String, hours: Float) {
        viewModelScope.launch {
            categoryGoalDao.upsert(CategoryGoal(categoryType = categoryType, targetMs = (hours * MS_PER_HOUR).toLong()))
        }
    }
}
