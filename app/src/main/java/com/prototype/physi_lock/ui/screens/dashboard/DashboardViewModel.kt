package com.prototype.physi_lock.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prototype.physi_lock.data.AppLockRule
import com.prototype.physi_lock.data.PhysiLockDatabase
import com.prototype.physi_lock.data.UsageSession
import com.prototype.physi_lock.data.UsageStatsRepository
import com.prototype.physi_lock.data.getTodayDateKey
import com.prototype.physi_lock.data.syncTodayUsage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUsageUiItem(
    val packageName: String,
    val appName: String,
    val durationMs: Long,
    val dailyLimitMs: Long?,
    val isOverLimit: Boolean
)

data class DashboardUiState(
    val todayScreenTimeMs: Long = 0L,
    val yesterdayScreenTimeMs: Long = 0L,
    val dailyLimitMs: Long = 7 * 60 * 60 * 1000L,
    val riskLevelLabel: String = "Moderate",
    val riskScoreApprox: Int = 55,
    val topAppUsage: List<AppUsageUiItem> = emptyList()
)

private fun dateKeyDaysAgo(days: Int): String {
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
}

private fun appNameFor(packageName: String): String = when {
    packageName.contains("instagram") -> "Instagram"
    packageName.contains("musically") || packageName.contains("trill") || packageName.contains("tiktok") -> "TikTok"
    packageName.contains("youtube") -> "YouTube"
    packageName.contains("twitter") || packageName == "com.x.android" -> "Twitter/X"
    else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val usageStatsRepository = UsageStatsRepository(application)

    private val _hasUsageAccess = MutableStateFlow(usageStatsRepository.hasUsageAccessPermission())
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    private val todaySessions: StateFlow<List<UsageSession>> =
        db.usageSessionDao().getSessionsForDate(getTodayDateKey())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val yesterdayTotalMs: StateFlow<Long> =
        db.usageSessionDao().getTotalDurationForDate(dateKeyDaysAgo(1))
            .map { it ?: 0L }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val lockRules: StateFlow<List<AppLockRule>> =
        db.appLockRuleDao().getAllRules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val riskLabel: StateFlow<String> =
        db.userConfigurationDao().getActiveConfiguration()
            .map { it?.riskSensitivity ?: "Moderate" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Moderate")

    private val dailyLimitMs: StateFlow<Long> =
        db.userConfigurationDao().getActiveConfiguration()
            .map { it?.dailyScreenTimeThresholdMs ?: (7 * 60 * 60 * 1000L) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 7 * 60 * 60 * 1000L)

    val uiState: StateFlow<DashboardUiState> = combine(
        todaySessions, yesterdayTotalMs, lockRules, riskLabel, dailyLimitMs
    ) { sessions, yesterdayMs, rules, risk, limitMs ->
        val ruleByPackage = rules.associateBy { it.packageName }
        val items = sessions
            .sortedByDescending { it.durationMs }
            .take(4)
            .map { session ->
                val rule = ruleByPackage[session.packageName]
                AppUsageUiItem(
                    packageName = session.packageName,
                    appName = appNameFor(session.packageName),
                    durationMs = session.durationMs,
                    dailyLimitMs = rule?.dailyLimitMs,
                    isOverLimit = rule?.dailyLimitMs?.let { session.durationMs > it } ?: false
                )
            }
        DashboardUiState(
            todayScreenTimeMs = sessions.sumOf { it.durationMs },
            yesterdayScreenTimeMs = yesterdayMs,
            dailyLimitMs = limitMs,
            riskLevelLabel = risk,
            riskScoreApprox = when (risk) {
                "Low" -> 30
                "High" -> 80
                else -> 55
            },
            topAppUsage = items
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun refreshUsageAccessStatus() {
        val granted = usageStatsRepository.hasUsageAccessPermission()
        _hasUsageAccess.value = granted
        if (granted) syncUsage()
    }

    fun syncUsage() {
        viewModelScope.launch {
            syncTodayUsage(db, usageStatsRepository)
        }
    }
}
