package com.example.physi_lock.ui.home

import android.app.Application
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.AppUsageTotal
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.UsageStatsRepository
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val appUsageDao = db.appUsageLogDao()
    private val userConfigDao = db.userConfigurationDao()
    private val appLockRuleDao = db.appLockRuleDao()
    private val usageStatsRepository = UsageStatsRepository(application)
    private val notificationManager = NotificationManagerCompat.from(application)
    private val today = java.time.LocalDate.now().toString()

    private val _todayScreenTimeMinutes = MutableStateFlow(0)
    val todayScreenTimeMinutes: StateFlow<Int> = _todayScreenTimeMinutes.asStateFlow()

    private val _dailyLimitMinutes = MutableStateFlow(480)
    val dailyLimitMinutes: StateFlow<Int> = _dailyLimitMinutes.asStateFlow()

    // Reflects the configured shake-to-unlock difficulty (Settings > Motion Lock
    // Sensitivity), not a computed risk score — real ML risk scoring is Module 2
    // (AI-Based Behavior Analysis).
    private val _lockSensitivity = MutableStateFlow("Moderate")
    val lockSensitivity: StateFlow<String> = _lockSensitivity.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _hasAccessibilityAccess = MutableStateFlow(false)
    val hasAccessibilityAccess: StateFlow<Boolean> = _hasAccessibilityAccess.asStateFlow()

    val lockedAppsToday: StateFlow<List<AppUsageTotal>> = combine(
        appLockRuleDao.getAllRules(),
        appUsageDao.getAppTotalsByDate(today)
    ) { rules, totals ->
        val locked = rules.filter { it.isLocked }.map { it.packageName }.toSet()
        totals.filter { it.packageName in locked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshPermissionState()

        viewModelScope.launch {
            val cfg = try { userConfigDao.getActiveConfigurationOnce() } catch (e: Exception) { null }
            _lockSensitivity.value = cfg?.riskSensitivity ?: "Moderate"
            _dailyLimitMinutes.value = ((cfg?.dailyScreenTimeThresholdMs ?: (480 * 60_000L)) / 60_000L).toInt()
        }
    }

    fun refreshPermissionState() {
        _hasUsageAccess.value = usageStatsRepository.hasUsageAccessPermission()
        _notificationsEnabled.value = notificationManager.areNotificationsEnabled()
        _hasOverlayPermission.value = Settings.canDrawOverlays(getApplication())
        _hasAccessibilityAccess.value = isAccessibilityServiceEnabled(getApplication())
        refreshTodayScreenTime()
    }

    // Today's screen time comes from Android's own UsageStatsManager (same source
    // Settings/Digital Wellbeing use), not AppMonitorService's AccessibilityService
    // logs — those only start accumulating once the service is actively running, so
    // they'd read 0 (or far under actual usage) rather than the real full-day total.
    // Refreshed on every ON_RESUME via refreshPermissionState() rather than a live
    // Flow, since UsageStatsManager only offers a point-in-time query.
    private fun refreshTodayScreenTime() {
        viewModelScope.launch {
            val totalMs = try {
                usageStatsRepository.getTodayUsage().sumOf { it.totalTimeMs }
            } catch (e: Exception) {
                0L
            }
            _todayScreenTimeMinutes.value = (totalMs / 60_000L).toInt()
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
    }
}
