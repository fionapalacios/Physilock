package com.example.physi_lock.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.physi_lock.data.AppUsageTotal
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.UsageStatsRepository
import com.example.physi_lock.ml.RiskFeatureExtractor
import com.example.physi_lock.ml.RiskScoringEngine
import com.example.physi_lock.service.AppMonitorService
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ported from the teammate's DashboardScreen "Doomscrolling detected" banner — real data
 *  instead of their hardcoded "23 min continuous session on TikTok": which app the most
 *  recent real DOOMSCROLL_ALERT fired on today, and how long ago. Only surfaced while still
 *  fresh (see [HomeViewModel.refreshBanners]) so it doesn't sit stale all day. */
data class DoomscrollAlertUi(val appName: String, val minutesAgo: Int)

/** Ported from the teammate's DashboardScreen "Predictive Overuse" banner — real data instead
 *  of their hardcoded "you'll hit your 7h limit by 8:22 PM": the next upcoming hour today
 *  Module 2's Logistic Regression II has already flagged excessive (see
 *  AppMonitorService.checkExcessiveUsagePrediction), not a fabricated clock-time projection. */
data class PredictiveOveruseUi(val hour: Int, val predictedMinutes: Double)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PhysiLockDatabase.getInstance(application)
    private val userConfigDao = db.userConfigurationDao()
    private val usageStatsRepository = UsageStatsRepository(application)
    private val riskFeatureExtractor = RiskFeatureExtractor(application)

    private val _todayScreenTimeMinutes = MutableStateFlow(0)
    val todayScreenTimeMinutes: StateFlow<Int> = _todayScreenTimeMinutes.asStateFlow()

    private val _dailyLimitMinutes = MutableStateFlow(480)
    val dailyLimitMinutes: StateFlow<Int> = _dailyLimitMinutes.asStateFlow()

    // Module 2 (AI-Based Behavior Analysis): real Random Forest risk classification
    // over today's actual logged usage (see RiskFeatureExtractor/RiskScoringEngine).
    // Replaces the old UserConfiguration.riskSensitivity placeholder, which just
    // mirrored the motion-lock-sensitivity setting.
    private val _riskLevel = MutableStateFlow("Moderate")
    val riskLevel: StateFlow<String> = _riskLevel.asStateFlow()

    private val _riskScorePercent = MutableStateFlow(0.5f)
    val riskScorePercent: StateFlow<Float> = _riskScorePercent.asStateFlow()

    // Ported concept from the teammate's DashboardScreen "App Usage Today" card — general
    // top-apps-by-usage, not filtered to locked apps (that's redundant with the "Lock Apps"
    // tile above, which already opens App Lock Rules directly). Same UsageStatsManager source
    // as ReportsViewModel's topApps, not AppUsageLog (see refreshTodayScreenTime kdoc).
    private val _appUsageToday = MutableStateFlow<List<AppUsageTotal>>(emptyList())
    val appUsageToday: StateFlow<List<AppUsageTotal>> = _appUsageToday.asStateFlow()

    private val _doomscrollAlert = MutableStateFlow<DoomscrollAlertUi?>(null)
    val doomscrollAlert: StateFlow<DoomscrollAlertUi?> = _doomscrollAlert.asStateFlow()

    private val _predictiveOveruse = MutableStateFlow<PredictiveOveruseUi?>(null)
    val predictiveOveruse: StateFlow<PredictiveOveruseUi?> = _predictiveOveruse.asStateFlow()

    // Ported concept from the teammate's DashboardScreen "You've been online 47 min" banner
    // -- previously dropped as fake (2026-08-25 audit: no live-session-duration plumbing
    // existed). Now real: AppMonitorService.getContinuousUsageStartTime() mirrors the same
    // in-memory continuous-usage clock the real Break Reminder notification already fires
    // off, read here at refresh time (point-in-time snapshot, same pattern as the other
    // banners below -- not a live-ticking timer). A 5-minute floor avoids flashing this for
    // trivial usage blips; not derived from the manuscript, a proposed default.
    private val _continuousUsageMinutes = MutableStateFlow<Int?>(null)
    val continuousUsageMinutes: StateFlow<Int?> = _continuousUsageMinutes.asStateFlow()

    // Module 5 (Mental Health & Awareness): backs the "Daily Reflection" GoalCard's
    // subtitle with a real answered/not-answered state instead of the old "Coming soon"
    // placeholder, now that ReflectionScreen exists.
    private val todayDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val hasReflectedToday: StateFlow<Boolean> = db.reflectionEntryDao().getForDate(todayDateKey)
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        refreshOnResume()

        viewModelScope.launch {
            val cfg = try { userConfigDao.getActiveConfigurationOnce() } catch (e: Exception) { null }
            _dailyLimitMinutes.value = ((cfg?.dailyScreenTimeThresholdMs ?: (480 * 60_000L)) / 60_000L).toInt()
        }
    }

    private fun refreshRiskScore() {
        viewModelScope.launch {
            try {
                val features = riskFeatureExtractor.extractTodayFeatures()
                val assessment = RiskScoringEngine.score(features)
                _riskLevel.value = assessment.level.label
                _riskScorePercent.value = assessment.score.toFloat()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Home's permission-status banner was dropped (Onboarding already gates all of
    // these before a user reaches Home) — this keeps the same on-resume refresh hook
    // for screen time + risk score alone, no longer also tracking permission state.
    fun refreshOnResume() {
        refreshTodayScreenTime()
        refreshRiskScore()
        refreshBanners()
    }

    /** Real data behind both of the teammate's Home banners — only set (and thus only
     *  rendered) when a real signal actually exists today, never an empty/fabricated
     *  placeholder state. See [DoomscrollAlertUi]/[PredictiveOveruseUi] kdoc. */
    private fun refreshBanners() {
        viewModelScope.launch {
            val breakReminderEnabled = try {
                userConfigDao.getActiveConfigurationOnce()?.breakReminderEnabled ?: true
            } catch (e: Exception) {
                true
            }
            val continuousStart = AppMonitorService.getContinuousUsageStartTime()
            _continuousUsageMinutes.value = if (breakReminderEnabled && continuousStart != null) {
                val minutes = ((System.currentTimeMillis() - continuousStart) / 60_000L).toInt()
                minutes.takeIf { it >= 5 }
            } else {
                null
            }

            val todayStartMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val recentAlert = try {
                db.motionInterventionLogDao().getMostRecentDoomscrollAlertAfter(todayStartMillis)
            } catch (e: Exception) {
                null
            }
            _doomscrollAlert.value = recentAlert
                ?.takeIf { System.currentTimeMillis() - it.interventionTimestamp <= 30 * 60_000L }
                ?.let {
                    DoomscrollAlertUi(
                        appName = usageStatsRepository.getAppLabel(it.packageName),
                        minutesAgo = ((System.currentTimeMillis() - it.interventionTimestamp) / 60_000L).toInt()
                    )
                }

            val currentHour = LocalTime.now().hour
            val predictions = try {
                db.excessiveUsagePredictionLogDao().getPredictionsByDate(LocalDate.now().toString()).first()
            } catch (e: Exception) {
                emptyList()
            }
            _predictiveOveruse.value = predictions
                .filter { it.hour > currentHour && it.isExcessive }
                .minByOrNull { it.hour }
                ?.let { PredictiveOveruseUi(hour = it.hour, predictedMinutes = it.predictedUsageMinutes) }
        }
    }

    // Today's screen time comes from Android's own UsageStatsManager (same source
    // Settings/Digital Wellbeing use), not AppMonitorService's AccessibilityService
    // logs — those only start accumulating once the service is actively running, so
    // they'd read 0 (or far under actual usage) rather than the real full-day total.
    // Refreshed on every ON_RESUME via refreshOnResume() rather than a live Flow,
    // since UsageStatsManager only offers a point-in-time query.
    private fun refreshTodayScreenTime() {
        viewModelScope.launch {
            val usage = try {
                usageStatsRepository.getTodayUsage()
            } catch (e: Exception) {
                emptyList()
            }
            _todayScreenTimeMinutes.value = (usage.sumOf { it.totalTimeMs } / 60_000L).toInt()
            _appUsageToday.value = usage
                .sortedByDescending { it.totalTimeMs }
                .take(5)
                .map { AppUsageTotal(it.packageName, usageStatsRepository.getAppLabel(it.packageName), it.totalTimeMs) }
        }
    }
}
