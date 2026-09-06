package com.example.physi_lock.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
///import androidx.lifecycle.LifecycleService
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.physi_lock.MainActivity
import com.example.physi_lock.R
import com.example.physi_lock.data.entity.AppCategoryType
import com.example.physi_lock.data.entity.AppUsageLog
import com.example.physi_lock.data.entity.DeepWorkSchedule
import com.example.physi_lock.data.entity.DeepWorkSession
import com.example.physi_lock.data.entity.ExcessiveUsagePredictionLog
import com.example.physi_lock.data.entity.MotionInterventionLog
import com.example.physi_lock.data.entity.NotificationLog
import com.example.physi_lock.data.db.PhysiLockDatabase
import com.example.physi_lock.data.repository.UsageStatsRepository
import com.example.physi_lock.data.context.currentWifiSsid
import com.example.physi_lock.data.context.lastKnownLocation
import com.example.physi_lock.ml.DoomscrollDetector
import com.example.physi_lock.ml.DoomscrollInputs
import com.example.physi_lock.ml.ExcessiveUsageDetector
import com.example.physi_lock.ml.ExcessiveUsageFeatureExtractor
import com.example.physi_lock.ml.RiskFeatureExtractor
import com.example.physi_lock.ml.RiskLevel
import com.example.physi_lock.ml.RiskScoringEngine
import com.example.physi_lock.ui.challenge.OveruseInterventionActivity
import com.example.physi_lock.ui.lock.BedtimeLockActivity
import com.example.physi_lock.ui.lock.LockActivity
import com.example.physi_lock.ui.lock.ModeLockActivity
import com.example.physi_lock.ui.reflection.DoomscrollReflectionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.physi_lock.data.entity.FocusSession
import com.example.physi_lock.data.entity.UserConfiguration

class AppMonitorService : AccessibilityService() {

    companion object {
        var lastUnlockTime = 0L

        fun triggerGlobalUnlock() {
            lastUnlockTime = System.currentTimeMillis()
        }

        // Break Reminder (Module 4): mirrors the instance-level continuousUsageStartTime
        // below so HomeViewModel can read the live in-progress session length for a real
        // "you've been online N min" banner, without any IPC -- everything runs in the same
        // process. Null means no continuous session is currently tracked (service not
        // running yet, or the last accessibility event was long enough ago to count as idle).
        @Volatile private var continuousUsageStartTimeShared: Long? = null

        fun getContinuousUsageStartTime(): Long? = continuousUsageStartTimeShared

        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_BEDTIME_END_MINUTE = "extra_bedtime_end_minute"
        const val CHALLENGE_UNLOCK_DURATION_MS = 20 * 60 * 1000L // 20 minutes

        // packageName -> unlock expiry epoch ms. Reassigned (not mutated) on every
        // change, same safe-without-locking pattern as the existing @Volatile
        // lockedPackages below — separate from, and additive to, lastUnlockTime's
        // blanket 8-second re-lock-loop suppression (still needed regardless of
        // which mechanism granted the unlock).
        @Volatile private var temporaryUnlocks: Map<String, Long> = emptyMap()

        fun grantTemporaryUnlock(packageName: String, durationMs: Long) {
            temporaryUnlocks = temporaryUnlocks + (packageName to System.currentTimeMillis() + durationMs)
            triggerGlobalUnlock()
        }

        private fun isTemporarilyUnlocked(packageName: String, currentTime: Long): Boolean {
            val expiry = temporaryUnlocks[packageName] ?: return false
            if (currentTime >= expiry) {
                temporaryUnlocks = temporaryUnlocks - packageName
                return false
            }
            return true
        }

        // Doomscroll "5 more minutes" (2026-09-06): unlike temporaryUnlocks above, this
        // doesn't grant access to anything -- doomscrolling was never a blocking mechanism,
        // just a notification. This only suppresses the reflection-prompt overlay from
        // firing again for a while, separate from checkDoomscrolling's own
        // DOOMSCROLL_ALERT_COOLDOWN_MS (which guards a *positive* detection from re-alerting
        // on the same continuing binge; this guards against re-alerting at all after the
        // user explicitly asked for more time). Same companion-object pattern as
        // temporaryUnlocks, for the same reason: DoomscrollReflectionActivity runs in a
        // different context and needs to reach this live service state.
        @Volatile private var doomscrollSnoozeUntil: Long = 0L

        fun snoozeDoomscrollAlerts(durationMs: Long) {
            doomscrollSnoozeUntil = System.currentTimeMillis() + durationMs
        }

        // Not derived from the manuscript or existing code — a break in accessibility
        // events longer than this is treated as the user having stepped away, which
        // resets the continuous-usage clock for the break reminder.
        private const val IDLE_RESET_THRESHOLD_MS = 2 * 60 * 1000L
        private const val BREAK_REMINDER_CHANNEL_ID = "break_reminder_channel"
        private const val BREAK_REMINDER_NOTIFICATION_ID = 1001
        private const val OVERUSE_ALERT_CHANNEL_ID = "overuse_alert_channel"
        private const val OVERUSE_ALERT_NOTIFICATION_ID = 1002
        // queryUsageStats is a real IPC call, not free — throttle how often
        // checkOveruseAlert actually queries it rather than on every event.
        private const val OVERUSE_CHECK_THROTTLE_MS = 60 * 1000L

        // Doomscroll checks run against in-memory session state (cheap), so this only
        // needs to throttle how chatty the model calls are, not IPC cost.
        private const val DOOMSCROLL_CHECK_THROTTLE_MS = 15 * 1000L
        // Once flagged, don't re-alert for the same continuing scroll binge.
        private const val DOOMSCROLL_ALERT_COOLDOWN_MS = 10 * 60 * 1000L
        // Matches the "every 5 minutes" cadence PROJECT_DOCUMENTATION.md's risk
        // scoring pipeline describes — the doomscroll "recipe" selector (see
        // DoomscrollDetector.kt) doesn't need every-scroll-event freshness.
        private const val RISK_REFRESH_INTERVAL_MS = 5 * 60 * 1000L

        // Deep Work Blocks (Work Mode redesign): checked once a minute -- frequent enough
        // that a scheduled window starts/ends close to on-time, cheap enough (in-memory
        // list scan + at most one DB read/write) to not matter running constantly.
        private const val DEEP_WORK_SCHEDULE_CHECK_INTERVAL_MS = 60 * 1000L

        private const val EXCESSIVE_USAGE_PREDICTION_CHANNEL_ID = "excessive_usage_prediction_channel"
        private const val EXCESSIVE_USAGE_PREDICTION_NOTIFICATION_ID = 1004
        // One EXCESSIVE_USAGE_PREDICTION row per hour is enough — checking more
        // often than this wouldn't change the hour-bucketed prediction anyway.
        private const val EXCESSIVE_USAGE_CHECK_INTERVAL_MS = 15 * 60 * 1000L

        // Persistent "session running" notification (2026-08-29, per YPT/Digital Wellbeing
        // comparison) -- Focus Mode lets the user leave to use non-blocked apps, so the
        // in-app countdown alone is invisible once they do. This keeps the live elapsed time
        // visible via the notification shade the whole session, same purpose YPT's floating
        // timer overlay serves, without building a full always-on-top overlay widget.
        private const val FOCUS_SESSION_CHANNEL_ID = "focus_session_channel"
        private const val FOCUS_SESSION_NOTIFICATION_ID = 1008
        private const val FOCUS_SESSION_UPDATE_INTERVAL_MS = 60 * 1000L

        private const val CONTEXT_ALERT_CHANNEL_ID = "context_alert_channel"
        private const val CONTEXT_ALERT_NOTIFICATION_ID = 1006
        // Context Alerts are informational only (no block), so a longer cooldown than
        // Focus Mode's block notification is appropriate -- no need to re-alert every
        // minute while the user stays connected to the same watched network.
        private const val CONTEXT_ALERT_NOTIFICATION_THROTTLE_MS = 5 * 60 * 1000L
        // WifiManager reads are cheap (local, no IPC), but there's no reason to call it
        // on every accessibility event either -- a cached value refreshed this often is
        // fresh enough for a "which network am I on" check.
        private const val WIFI_SSID_REFRESH_INTERVAL_MS = 30 * 1000L

        private const val SCHEDULE_BLOCK_CHANNEL_ID = "schedule_block_channel"
        private const val SCHEDULE_BLOCK_NOTIFICATION_ID = 1007
        // Same reasoning as Focus Block's throttle — an app can be repeatedly
        // relaunched during an active schedule block; only the notification/log is
        // throttled, the home-kick itself always fires on every attempt.
        private const val SCHEDULE_BLOCK_NOTIFICATION_THROTTLE_MS = 60 * 1000L
    }

    @Volatile private var lockedPackages: Set<String> = emptySet()
    @Volatile private var breakReminderEnabled: Boolean = true
    @Volatile private var breakReminderIntervalMs: Long = 30 * 60 * 1000L
    @Volatile private var overuseAlertsEnabled: Boolean = true
    @Volatile private var dailyScreenTimeThresholdMs: Long = 480 * 60 * 1000L
    @Volatile private var doomscrollingDetectionEnabled: Boolean = true
    // Doomscroll Sensitivity (2026-08-27): Admin-set bias layered on the risk-level
    // recipe below -- see DoomscrollDetector.kt's SENSITIVITY_BIAS kdoc.
    @Volatile private var doomscrollingSensitivity: String = "MODERATE"
    // Refreshed periodically (RISK_REFRESH_INTERVAL_MS), not on every check — see
    // startRiskRefreshLoop(). Selects the doomscroll detection threshold "recipe";
    // is NOT fed into DoomscrollModel as a feature (see DoomscrollDetector.kt).
    @Volatile private var cachedRiskLevel: RiskLevel = RiskLevel.MODERATE
    @Volatile private var cachedRiskScore: Double = 0.5
    // Module 6 (Personalization & User Control): Focus Mode. focusModeActive mirrors
    // whether a FocusSession row is currently open; focusBlockedPackages mirrors the
    // User's own FocusBlockedApp selection (see FocusModeViewModel/FocusBlockedAppsScreen —
    // same source of truth, so the UI's "blocked" chips and the actual enforcement here
    // can never drift apart). Deliberately not Admin's app categories -- Admin curates
    // categories for tracking/reporting, the User decides what gets blocked.
    @Volatile private var focusModeActive: Boolean = false
    @Volatile private var focusBlockedPackages: Set<String> = emptySet()
    // Deep Work Mode (2026-09-04): a stricter tier than Focus Mode -- see DeepWorkSession
    // kdoc. deepWorkBlockedPackages is Admin-category-derived (ALL Social Media/
    // Entertainment apps), deliberately not the User-owned focusBlockedPackages set above,
    // so it's a genuinely broader/uncustomizable block during a Deep Work session.
    @Volatile private var deepWorkActive: Boolean = false
    @Volatile private var deepWorkBlockedPackages: Set<String> = emptySet()
    // Student Mode Pomodoro (2026-09-07): pomodoroPhase is null when no session is active,
    // "WORK" or "BREAK" otherwise -- only "WORK" blocks (see the priority-chain branch
    // below). pomodoroBlockedPackages is User-owned (PomodoroBlockedApp), same reasoning
    // as focusBlockedPackages above.
    @Volatile private var pomodoroPhase: String? = null
    @Volatile private var pomodoroBlockedPackages: Set<String> = emptySet()
    // Module 7 (Context-Aware AI): Wi-Fi-network-matched Context Alerts. Not GPS
    // geofencing -- real location-based locking is a Future Enhancement per the
    // manuscript, out of MVP scope; this matches by Wi-Fi network name instead, which
    // needs no new location SDK dependency. Reuses focusBlockedPackages (Admin-curated
    // Social Media / Entertainment categories) as the same "distracting apps" set Focus
    // Mode blocks -- alerting here is passive (a notification), not an enforced block.
    @Volatile private var contextAlertsEnabled: Boolean = false
    @Volatile private var contextAlertWifiSsid: String? = null
    @Volatile private var cachedWifiSsid: String? = null
    @Volatile private var contextAlertLatitude: Double? = null
    @Volatile private var contextAlertLongitude: Double? = null
    @Volatile private var contextAlertRadiusMeters: Int = 100
    @Volatile private var cachedLocation: android.location.Location? = null
    private var lastContextAlertNotifyTime: Long = 0L
    // userMode drives both Work Mode's Work Hours window and (historically) Student
    // Mode's class-schedule blocking; allowlistedPackages is Bedtime Mode's "stays
    // reachable during the window" set (Student Mode's old allowlist-inversion model was
    // replaced by Pomodoro's blocklist 2026-09-07, but the table/set is shared with Bedtime
    // so it stays). Same live-Flow-collector pattern as lockedPackages/focusBlockedPackages.
    @Volatile private var userMode: String = "STUDENT_MODE"
    @Volatile private var allowlistedPackages: Set<String> = emptySet()
    @Volatile private var bedtimeStartMinute: Int = 23 * 60
    @Volatile private var bedtimeEndMinute: Int = 7 * 60
    @Volatile private var workHoursStartMinute: Int = 9 * 60
    @Volatile private var workHoursEndMinute: Int = 17 * 60
    // Work Mode redesign: named windows that auto-start/end a real DeepWorkSession --
    // see checkDeepWorkSchedules(). Same live-Flow-collector pattern as scheduleBlocks.
    @Volatile private var deepWorkSchedules: List<DeepWorkSchedule> = emptyList()
    private var lastScheduleBlockNotifyTime: Long = 0L
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: PhysiLockDatabase
    private lateinit var usageStatsRepository: UsageStatsRepository
    private lateinit var riskFeatureExtractor: RiskFeatureExtractor
    private lateinit var excessiveUsageFeatureExtractor: ExcessiveUsageFeatureExtractor
    private var lastExcessiveUsageAlertHourKey: String? = null
    private var currentForegroundPackage: String? = null
    private var currentSessionStartTime: Long? = null
    // Module 2 (AI-Based Behavior Analysis): raw scroll signal for the doomscroll
    // classifier's "scroll speed"/"pause pattern" inputs, scoped to the current
    // foreground session and flushed into that session's AppUsageLog row when it ends.
    private var currentSessionScrollCount = 0
    private var currentSessionLastScrollTime: Long? = null
    private var currentSessionMaxScrollGapMs: Long = 0
    private var continuousUsageStartTime: Long? = null
    private var lastActivityEventTime: Long? = null
    private var lastOveruseCheckTime: Long = 0L
    private var lastOveruseAlertDateKey: String? = null
    private var lastDoomscrollCheckTime: Long = 0L
    private var lastDoomscrollAlertTime: Long = 0L
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate() {
        super.onCreate()
        database = PhysiLockDatabase.getInstance(this)
        usageStatsRepository = UsageStatsRepository(this)
        riskFeatureExtractor = RiskFeatureExtractor(this)
        excessiveUsageFeatureExtractor = ExcessiveUsageFeatureExtractor(this)
        createBreakReminderNotificationChannel()
        createOveruseAlertNotificationChannel()
        createExcessiveUsagePredictionNotificationChannel()
        createContextAlertNotificationChannel()
        createScheduleBlockNotificationChannel()
        createFocusSessionNotificationChannel()

        // Live-reload the locked app set from Settings > App Lock Rules; Room's
        // Flow re-emits automatically whenever the table changes, so toggles
        // made while the service is running take effect without a restart.
        serviceScope.launch {
            database.appLockRuleDao().getAllRules().collect { rules ->
                lockedPackages = rules.filter { it.isLocked }.map { it.packageName }.toSet()
            }
        }

        // Same live-reload pattern for the break reminder's Settings toggle/interval.
        serviceScope.launch {
            database.userConfigurationDao().getActiveConfiguration().collect { config ->
                breakReminderEnabled = config?.breakReminderEnabled ?: true
                breakReminderIntervalMs = config?.breakReminderIntervalMs ?: (30 * 60 * 1000L)
                overuseAlertsEnabled = config?.overuseAlertsEnabled ?: true
                dailyScreenTimeThresholdMs = config?.dailyScreenTimeThresholdMs ?: (480 * 60 * 1000L)
                doomscrollingDetectionEnabled = config?.doomscrollingDetectionEnabled ?: true
                doomscrollingSensitivity = config?.doomscrollingSensitivity ?: "MODERATE"
                contextAlertsEnabled = config?.contextAlertsEnabled ?: false
                contextAlertWifiSsid = config?.contextAlertWifiSsid
                contextAlertLatitude = config?.contextAlertLatitude
                contextAlertLongitude = config?.contextAlertLongitude
                contextAlertRadiusMeters = config?.contextAlertRadiusMeters ?: 100
                userMode = config?.userMode ?: "STUDENT_MODE"
                bedtimeStartMinute = config?.bedtimeStartMinute ?: (23 * 60)
                bedtimeEndMinute = config?.bedtimeEndMinute ?: (7 * 60)
                workHoursStartMinute = config?.workHoursStartMinute ?: (9 * 60)
                workHoursEndMinute = config?.workHoursEndMinute ?: (17 * 60)
            }
        }

        // Work Mode redesign: live-reload Deep Work Blocks, same pattern as scheduleBlocks.
        serviceScope.launch {
            database.deepWorkScheduleDao().getAll().collect { schedules ->
                deepWorkSchedules = schedules
            }
        }

        // Auto-start/stop a real Deep Work session when the current time enters/exits an
        // active DeepWorkSchedule window -- decoupled from accessibility events (a window
        // can start even if the user isn't switching apps), same periodic-loop shape as
        // the risk-score refresh below.
        serviceScope.launch {
            while (true) {
                checkDeepWorkSchedules()
                delay(DEEP_WORK_SCHEDULE_CHECK_INTERVAL_MS)
            }
        }

        // Bedtime Mode's "stays reachable during the window" allowlist, same pattern as
        // lockedPackages/focusBlockedPackages.
        serviceScope.launch {
            database.allowlistedAppDao().getAll().collect { apps ->
                allowlistedPackages = apps.map { it.packageName }.toSet()
            }
        }

        // Module 6 (Personalization & User Control): live-reload Focus Mode's active
        // session and its real blocked-app set, same pattern as lockedPackages above.
        // The blocked set is a User-owned selection (see FocusBlockedAppsScreen), not
        // derived from Admin's app categories.
        serviceScope.launch {
            database.focusSessionDao().getActiveSession().collectLatest { session ->
                focusModeActive = session != null
                if (session == null) {
                    NotificationManagerCompat.from(this@AppMonitorService).cancel(FOCUS_SESSION_NOTIFICATION_ID)
                    return@collectLatest
                }
                // Ticks for as long as this exact session stays active; collectLatest
                // cancels this loop the moment the session ends or a new one starts.
                while (true) {
                    postFocusSessionNotification(session.startTimeMillis)
                    delay(FOCUS_SESSION_UPDATE_INTERVAL_MS)
                }
            }
        }
        serviceScope.launch {
            database.focusBlockedAppDao().getAll().collect { apps ->
                focusBlockedPackages = apps.map { it.packageName }.toSet()
            }
        }

        // Deep Work Mode (2026-09-04): same live-reload pattern as Focus Mode above.
        serviceScope.launch {
            database.deepWorkSessionDao().getActiveSession().collectLatest { session ->
                deepWorkActive = session != null
            }
        }
        serviceScope.launch {
            database.appCategoryDao().getAll().collect { categories ->
                deepWorkBlockedPackages = categories
                    .filter { it.category == AppCategoryType.SOCIAL_MEDIA || it.category == AppCategoryType.ENTERTAINMENT }
                    .map { it.packageName }
                    .toSet()
            }
        }

        // Student Mode Pomodoro (2026-09-07): same live-reload pattern as Deep Work above.
        serviceScope.launch {
            database.pomodoroSessionDao().getActiveSession().collectLatest { session ->
                pomodoroPhase = session?.phase
            }
        }
        serviceScope.launch {
            database.pomodoroBlockedAppDao().getAll().collect { apps ->
                pomodoroBlockedPackages = apps.map { it.packageName }.toSet()
            }
        }

        startRiskRefreshLoop()
        startExcessiveUsagePredictionLoop()
        startWifiSsidRefreshLoop()
    }

    // Module 7 (Context-Aware AI): periodically caches the connected Wi-Fi SSID (see
    // WifiSsidReader.kt) so the per-event Context Alert check below is a cheap in-memory
    // comparison instead of hitting WifiManager on every accessibility event. Runs
    // unconditionally (the read itself is cheap, local, no IPC) rather than gating it on
    // contextAlertsEnabled, avoiding extra start/stop lifecycle complexity.
    private fun startWifiSsidRefreshLoop() {
        serviceScope.launch {
            while (true) {
                try {
                    cachedWifiSsid = currentWifiSsid(this@AppMonitorService)
                    // Context Alert's GPS trigger (2026-09-04): same cadence/rationale as the
                    // Wi-Fi read above -- a cheap periodic cache read backing the per-event
                    // check below, not a live location request.
                    cachedLocation = lastKnownLocation(this@AppMonitorService)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(WIFI_SSID_REFRESH_INTERVAL_MS)
            }
        }
    }

    // Module 2 (AI-Based Behavior Analysis): periodically runs Logistic Regression II
    // and logs one EXCESSIVE_USAGE_PREDICTION-equivalent row per hour (see
    // ExcessiveUsagePredictionLog / ml/README.md). Reuses the existing Overuse Alerts
    // toggle rather than adding a brand-new Settings toggle for this specific
    // notification — it's still fundamentally an overuse alert, just predictive
    // instead of reactive-on-today's-total.
    private fun startExcessiveUsagePredictionLoop() {
        serviceScope.launch {
            while (true) {
                try {
                    checkExcessiveUsagePrediction()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(EXCESSIVE_USAGE_CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkExcessiveUsagePrediction() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dateKey = dateFormatter.format(calendar.time)

        // One prediction row per hour — re-checking within the same hour wouldn't
        // change the bucketed prediction.
        if (database.excessiveUsagePredictionLogDao().countForHour(dateKey, hour) > 0) return

        val inputs = excessiveUsageFeatureExtractor.extractCurrentHourFeatures()
        val prediction = ExcessiveUsageDetector.predict(inputs)

        database.excessiveUsagePredictionLogDao().insert(
            ExcessiveUsagePredictionLog(
                dateKey = dateKey,
                hour = hour,
                predictedUsageMinutes = inputs.avgUsageThisHourMin,
                excessiveProbability = prediction.probability,
                isExcessive = prediction.isExcessive
            )
        )

        val hourKey = "$dateKey-$hour"
        // Deferred, not skipped, during quiet hours -- see checkOveruseAlert's isQuietHours
        // comment; the hour-dedupe key is left unset so a later check this same hour
        // (once the schedule block ends) can still post it.
        if (prediction.isExcessive && overuseAlertsEnabled && lastExcessiveUsageAlertHourKey != hourKey && !isQuietHours()) {
            lastExcessiveUsageAlertHourKey = hourKey
            postExcessiveUsagePredictionNotification()
        }
    }

    // Module 2 (AI-Based Behavior Analysis): periodically recomputes the Random
    // Forest risk level so the doomscroll detector always has a reasonably fresh
    // threshold "recipe" to check against, without recomputing it (several Room
    // queries) on every scroll event.
    private fun startRiskRefreshLoop() {
        serviceScope.launch {
            while (true) {
                try {
                    val features = riskFeatureExtractor.extractTodayFeatures()
                    val assessment = RiskScoringEngine.score(features)
                    cachedRiskLevel = assessment.level
                    cachedRiskScore = assessment.score
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(RISK_REFRESH_INTERVAL_MS)
            }
        }
    }

    private fun createBreakReminderNotificationChannel() {
        val channel = NotificationChannel(
            BREAK_REMINDER_CHANNEL_ID,
            "Break Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders to take a break after continuous device use"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createOveruseAlertNotificationChannel() {
        val channel = NotificationChannel(
            OVERUSE_ALERT_CHANNEL_ID,
            "Overuse Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when today's total screen time exceeds your daily limit"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createExcessiveUsagePredictionNotificationChannel() {
        val channel = NotificationChannel(
            EXCESSIVE_USAGE_PREDICTION_CHANNEL_ID,
            "Excessive Usage Predictions",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Warns when this hour is predicted to be an excessive-usage hour"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createContextAlertNotificationChannel() {
        val channel = NotificationChannel(
            CONTEXT_ALERT_CHANNEL_ID,
            "Context Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when you open a distracting app on your watched Wi-Fi network"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createScheduleBlockNotificationChannel() {
        val channel = NotificationChannel(
            SCHEDULE_BLOCK_CHANNEL_ID,
            "Schedule Blocks",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when an app is blocked during a Student/Work Mode schedule block"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createFocusSessionNotificationChannel() {
        val channel = NotificationChannel(
            FOCUS_SESSION_CHANNEL_ID,
            "Focus Session Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the live elapsed time while a Focus Mode session is running"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        val currentTime = System.currentTimeMillis()

        // IGNORE EVERYTHING FOR 8 SECONDS AFTER UNLOCKING (Prevents close-app loop)
        if (currentTime - lastUnlockTime < 8000L) {
            return
        }

        // Ignore Physi-Lock's own events
        if (packageName == "com.example.physi_lock" || packageName.contains("physi_lock")) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChange(packageName, currentTime)
                trackContinuousUsage(currentTime)
                checkOveruseAlert(currentTime)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // Module 2 (AI-Based Behavior Analysis): doomscrolling detection
                logScrollEvent(packageName, currentTime)
                checkDoomscrolling(packageName, currentTime)
                trackContinuousUsage(currentTime)
            }
        }
    }

    // Break Reminder (Module 4): "continuous usage" accumulates as long as
    // accessibility events keep arriving without a gap longer than
    // IDLE_RESET_THRESHOLD_MS; a longer gap is treated as the user having taken
    // a break, and the clock restarts. Independent of doomscroll detection above.
    private fun trackContinuousUsage(currentTime: Long) {
        val lastEvent = lastActivityEventTime
        if (lastEvent == null || currentTime - lastEvent > IDLE_RESET_THRESHOLD_MS) {
            continuousUsageStartTime = currentTime
            continuousUsageStartTimeShared = currentTime
        }
        lastActivityEventTime = currentTime

        if (!breakReminderEnabled) return
        val startTime = continuousUsageStartTime ?: return
        val elapsed = currentTime - startTime
        if (elapsed >= breakReminderIntervalMs) {
            postBreakReminderNotification(elapsed)
            continuousUsageStartTime = currentTime
            continuousUsageStartTimeShared = currentTime
        }
    }

    private fun postBreakReminderNotification(elapsedMs: Long) {
        if (isQuietHours()) return
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val elapsedMinutes = elapsedMs / 60_000L
        val notification = NotificationCompat.Builder(this, BREAK_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Break reminder")
            .setContentText("You've been active for $elapsedMinutes min — stretch or take a walk 🚶")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(BREAK_REMINDER_NOTIFICATION_ID, notification)
        logNotification(
            type = "BREAK_REMINDER",
            title = "Break reminder",
            description = "You've been active for $elapsedMinutes min — stretch or take a walk"
        )
    }

    // Overuse Alert (Module 4/1): a passive, at-most-once-per-day notification when
    // today's real aggregate screen time crosses the configured daily limit —
    // separate from Break Reminder (repeating, continuous-usage-based) and from
    // Adaptive Locking enforcement (blocked on Module 2). Throttled since
    // UsageStatsManager queries are a real IPC call, not free.
    private fun checkOveruseAlert(currentTime: Long) {
        if (!overuseAlertsEnabled) return
        if (currentTime - lastOveruseCheckTime < OVERUSE_CHECK_THROTTLE_MS) return
        lastOveruseCheckTime = currentTime

        val todayKey = dateFormatter.format(Date(currentTime))
        if (lastOveruseAlertDateKey == todayKey) return
        // Deferred, not skipped: don't consume today's dedupe key during quiet hours,
        // so the alert can still fire once the Work Mode schedule block ends.
        if (isQuietHours()) return

        serviceScope.launch {
            try {
                val totalTodayMs = usageStatsRepository.getTodayUsage().sumOf { it.totalTimeMs }
                if (totalTodayMs >= dailyScreenTimeThresholdMs) {
                    postOveruseAlertNotification(totalTodayMs, dailyScreenTimeThresholdMs)
                    lastOveruseAlertDateKey = todayKey
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun postOveruseAlertNotification(totalMs: Long, thresholdMs: Long) {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val overMinutes = ((totalMs - thresholdMs) / 60_000L).coerceAtLeast(0)
        val notification = NotificationCompat.Builder(this, OVERUSE_ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Daily limit exceeded")
            .setContentText("You've used your device $overMinutes min over your daily limit today")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(OVERUSE_ALERT_NOTIFICATION_ID, notification)
        logNotification(
            type = "OVERUSE_ALERT",
            title = "Daily limit exceeded",
            description = "You've used your device $overMinutes min over your daily limit today"
        )
    }

    // Doomscroll Detection (Module 2): checks the live, in-progress session's scroll
    // rate/pause pattern against Logistic Regression I, using cachedRiskLevel as the
    // detection threshold "recipe" (see DoomscrollDetector.kt — risk score is NOT a
    // model feature). Runs off in-memory state only (cheap), throttled just to avoid
    // calling the model on every single scroll event, plus a longer alert cooldown so
    // one continuing scroll binge doesn't spam repeat notifications.
    private fun checkDoomscrolling(packageName: String, currentTime: Long) {
        if (!doomscrollingDetectionEnabled) return
        if (currentTime < doomscrollSnoozeUntil) return
        if (currentTime - lastDoomscrollCheckTime < DOOMSCROLL_CHECK_THROTTLE_MS) return
        lastDoomscrollCheckTime = currentTime
        if (currentTime - lastDoomscrollAlertTime < DOOMSCROLL_ALERT_COOLDOWN_MS) return

        val sessionStart = currentSessionStartTime ?: return
        val elapsedMs = currentTime - sessionStart
        // Too small a sample for a live per-session rate to mean anything yet.
        if (currentSessionScrollCount < 5 || elapsedMs < 5_000L) return

        val inputs = DoomscrollInputs(
            scrollSpeedPerMin = currentSessionScrollCount / (elapsedMs / 60_000.0),
            maxPauseGapSec = currentSessionMaxScrollGapMs / 1000.0,
            hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY).toDouble()
        )

        val isDoomscrolling = try {
            DoomscrollDetector.detect(inputs, cachedRiskLevel, doomscrollingSensitivity)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

        if (!isDoomscrolling) return
        lastDoomscrollAlertTime = currentTime

        // Real full-screen reflection prompt (2026-09-06) -- was a passive notification
        // only (postDoomscrollAlertNotification, now removed). See DoomscrollReflectionActivity.
        val intent = Intent(this, DoomscrollReflectionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(OveruseInterventionActivity.EXTRA_RISK_TIER, cachedRiskLevel.name)
        }
        startActivity(intent)

        serviceScope.launch {
            try {
                database.motionInterventionLogDao().insert(
                    MotionInterventionLog(
                        packageName = packageName,
                        triggerType = "DOOMSCROLL_ALERT",
                        interventionTimestamp = currentTime,
                        riskScore = cachedRiskScore
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun postExcessiveUsagePredictionNotification() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, EXCESSIVE_USAGE_PREDICTION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("This hour looks like a heavy usage hour")
            .setContentText("Your usual pattern suggests you're about to use your device a lot this hour")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(EXCESSIVE_USAGE_PREDICTION_NOTIFICATION_ID, notification)
        logNotification(
            type = "EXCESSIVE_USAGE_PREDICTION",
            title = "This hour looks like a heavy usage hour",
            description = "Your usual pattern suggests you're about to use your device a lot this hour"
        )
    }

    private fun logNotification(type: String, title: String, description: String) {
        serviceScope.launch {
            try {
                database.notificationLogDao().insert(
                    NotificationLog(
                        type = type,
                        title = title,
                        description = description,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleWindowStateChange(packageName: String, currentTime: Long) {
        // Log the previous app session if it changed
        if (currentForegroundPackage != null && currentForegroundPackage != packageName) {
            logAppSession(
                currentForegroundPackage!!,
                currentTime,
                scrollEventCount = currentSessionScrollCount,
                maxScrollGapMs = currentSessionMaxScrollGapMs
            )
        }

        // Update current foreground app; reset per-session scroll tracking
        currentForegroundPackage = packageName
        currentSessionStartTime = currentTime
        currentSessionScrollCount = 0
        currentSessionLastScrollTime = null
        currentSessionMaxScrollGapMs = 0

        // Check if app should be locked (Module 3/4's challenge-based lock takes
        // priority over Focus Mode's plain block for apps that are both).
        if (packageName in lockedPackages && !isTemporarilyUnlocked(packageName, currentTime)) {
            val intent = Intent(this, LockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        } else if (deepWorkActive && packageName in deepWorkBlockedPackages) {
            // Real full-screen overlay (2026-09-06) -- was routed through handleScheduleBlock
            // (performGlobalAction(HOME) + notification, still used by Work Mode below).
            // Purely informational: shaking the device from the Deep Work screen's exit gate
            // stays the only real way out, this just replaces the silent home-kick.
            val intent = Intent(this, ModeLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(ModeLockActivity.EXTRA_TITLE, "Deep Work Active")
                putExtra(ModeLockActivity.EXTRA_MESSAGE, "Blocked for the rest of this session")
            }
            startActivity(intent)
        } else if (pomodoroPhase == "WORK" && packageName in pomodoroBlockedPackages) {
            // Student Mode Pomodoro (2026-09-07): a real committed session like Deep Work,
            // not a passive schedule -- same full-screen overlay treatment. Only the WORK
            // phase blocks; the BREAK phase deliberately doesn't, matching real Pomodoro
            // technique (a break is meant to be an actual break). Ending the session (or
            // waiting for the WORK phase to end) stays the only real way in.
            val intent = Intent(this, ModeLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(ModeLockActivity.EXTRA_TITLE, "Study Session Active")
                putExtra(ModeLockActivity.EXTRA_MESSAGE, "Blocked during your Pomodoro study session")
            }
            startActivity(intent)
        } else if (isWithinBedtimeWindow() && packageName !in allowlistedPackages && !isSystemPackage(packageName)) {
            // Real hard-block lock screen (2026-09-06) -- was performGlobalAction(HOME) +
            // a notification only (handleScheduleBlock, still used by Work Mode below).
            // Same startActivity(NEW_TASK|CLEAR_TASK) mechanism LockActivity uses.
            val intent = Intent(this, BedtimeLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_BEDTIME_END_MINUTE, bedtimeEndMinute)
            }
            startActivity(intent)
        } else if (userMode == "WORK_MODE" && isWithinWorkHours() && packageName in focusBlockedPackages) {
            // Work Mode redesign (2026-09-07): condition changed from the old per-day
            // ScheduleBlock check to isWithinWorkHours() (single Mon-Fri window) -- the
            // home-kick+notification mechanism itself (handleScheduleBlock) is unchanged.
            handleScheduleBlock(
                currentTime,
                title = "Work Hours active",
                description = "${getAppName(packageName)} is blocked during Work Hours"
            )
        } else if (focusModeActive && packageName in focusBlockedPackages) {
            // Real full-screen overlay (2026-09-06) -- was performGlobalAction(HOME) + a
            // throttled notification (handleFocusBlock/postFocusBlockNotification, now
            // removed). Purely informational: ending the Focus session stays the only real
            // way out, this just replaces the silent home-kick.
            val intent = Intent(this, ModeLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(ModeLockActivity.EXTRA_TITLE, "Focus Mode Active")
                putExtra(ModeLockActivity.EXTRA_MESSAGE, "Blocked while your focus session is running")
            }
            startActivity(intent)
        } else if (contextAlertsEnabled && packageName in focusBlockedPackages &&
            (isOnWatchedWifi() || isNearWatchedLocation())
        ) {
            handleContextAlert(packageName, currentTime)
        }
    }

    // Bedtime Mode (real, 2026-09-04): a daily minutes-since-midnight window, so it can
    // (and by default does, 11 PM-7 AM) wrap past midnight. Cheap synchronous
    // cached-fields-only check -- no DB access on the accessibility-event thread.
    private fun isWithinBedtimeWindow(): Boolean {
        val calendar = Calendar.getInstance()
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return if (bedtimeStartMinute <= bedtimeEndMinute) {
            minuteOfDay >= bedtimeStartMinute && minuteOfDay < bedtimeEndMinute
        } else {
            minuteOfDay >= bedtimeStartMinute || minuteOfDay < bedtimeEndMinute
        }
    }

    // Work Mode redesign (2026-09-07): replaces the old per-day ScheduleBlock check for
    // Work Mode with a single daily window applied Mon-Fri only (matching the comparison
    // mockup's static "Monday - Friday" label -- it has no day picker). Same
    // cached-fields-only cheap check as isWithinBedtimeWindow.
    private fun isWithinWorkHours(): Boolean {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) return false
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return if (workHoursStartMinute <= workHoursEndMinute) {
            minuteOfDay >= workHoursStartMinute && minuteOfDay < workHoursEndMinute
        } else {
            minuteOfDay >= workHoursStartMinute || minuteOfDay < workHoursEndMinute
        }
    }

    // Work Mode redesign: auto-starts a real DeepWorkSession (triggeredBy="SCHEDULE") when
    // the current time enters an active DeepWorkSchedule window, and auto-ends it (crediting
    // the same 1-min-per-5 rate DeepWorkViewModel.endSession uses) once no window is active
    // anymore -- but only for a session this checker itself started. A session the user
    // started by hand (triggeredBy="MANUAL", e.g. from Home) is never touched here, and a
    // schedule window never starts a second session on top of either kind already running.
    private suspend fun checkDeepWorkSchedules() {
        if (userMode != "WORK_MODE") return
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) return
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val activeWindow = deepWorkSchedules.firstOrNull { schedule ->
            schedule.active && minuteOfDay >= schedule.startMinute && minuteOfDay < schedule.endMinute
        }

        try {
            val currentSession = database.deepWorkSessionDao().getActiveSessionOnce()
            if (activeWindow != null) {
                if (currentSession == null) {
                    database.deepWorkSessionDao().insert(
                        DeepWorkSession(
                            startTimeMillis = System.currentTimeMillis(),
                            durationSecs = (activeWindow.endMinute - minuteOfDay) * 60,
                            triggeredBy = "SCHEDULE"
                        )
                    )
                }
            } else if (currentSession != null && currentSession.triggeredBy == "SCHEDULE") {
                val endTime = System.currentTimeMillis()
                val creditMinutes = ((endTime - currentSession.startTimeMillis) / 300_000L).toInt()
                database.deepWorkSessionDao().endSession(currentSession.id, endTime, endedEarly = false, creditMinutesEarned = creditMinutes)
                if (creditMinutes > 0) {
                    val cfg = database.userConfigurationDao().getActiveConfigurationOnce() ?: UserConfiguration()
                    database.userConfigurationDao().upsert(
                        cfg.copy(
                            focusCreditBalanceMinutes = cfg.focusCreditBalanceMinutes + creditMinutes,
                            lastUpdatedTime = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Work Mode "quiet hours": passive nudge notifications (Break Reminder, Overuse
    // Alert, Excessive Usage Prediction) are held while Work Hours is active --
    // Doomscroll Alert, Context Alert, and the schedule block notification itself are
    // unaffected, since those are active-intervention signals, not FYI nudges.
    private fun isQuietHours(): Boolean = userMode == "WORK_MODE" && isWithinWorkHours()

    // Safety filter for Student Mode's allowlist-inverted blocking (block everything
    // NOT allowlisted) -- must never kick the launcher, dialer, Settings, or system UI
    // to home. Work Mode doesn't need this: it only ever targets Admin-curated Social
    // Media/Entertainment apps (focusBlockedPackages), already inherently safe.
    private fun isSystemPackage(packageName: String): Boolean {
        return try {
            val flags = packageManager.getApplicationInfo(packageName, 0).flags
            (flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }

    // Context Alert (Module 7): unlike Focus Mode's block, this never redirects the user
    // away -- it's a passive notification ("Receive Context Alerts"), matching a network
    // the user has flagged as a context worth being mindful in (e.g. a study space), not
    // an enforced restriction.
    private fun isOnWatchedWifi(): Boolean {
        val watched = contextAlertWifiSsid ?: return false
        return cachedWifiSsid != null && cachedWifiSsid == watched
    }

    // Context Alert's GPS trigger (2026-09-04, see UserConfiguration.contextAlertLatitude
    // kdoc): same passive-notification-only semantics as isOnWatchedWifi above, just a
    // distance check instead of a name match.
    private fun isNearWatchedLocation(): Boolean {
        val lat = contextAlertLatitude ?: return false
        val lng = contextAlertLongitude ?: return false
        val here = cachedLocation ?: return false
        val results = FloatArray(1)
        android.location.Location.distanceBetween(here.latitude, here.longitude, lat, lng, results)
        return results[0] <= contextAlertRadiusMeters
    }

    private fun handleContextAlert(packageName: String, currentTime: Long) {
        if (currentTime - lastContextAlertNotifyTime < CONTEXT_ALERT_NOTIFICATION_THROTTLE_MS) return
        lastContextAlertNotifyTime = currentTime
        postContextAlertNotification(packageName)
    }

    private fun postContextAlertNotification(packageName: String) {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val appName = getAppName(packageName)
        val ssid = contextAlertWifiSsid ?: return
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CONTEXT_ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Context Alert")
            .setContentText("You opened $appName while connected to $ssid")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(CONTEXT_ALERT_NOTIFICATION_ID, notification)
        logNotification(
            type = "CONTEXT_ALERT",
            title = "Context Alert",
            description = "You opened $appName while connected to $ssid"
        )
    }

    private fun postFocusSessionNotification(startTimeMillis: Long) {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val elapsedMinutes = ((System.currentTimeMillis() - startTimeMillis) / 60_000L).coerceAtLeast(0)
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, FOCUS_SESSION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Focus session running")
            .setContentText("${elapsedMinutes}m focused so far · tap to return")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(this).notify(FOCUS_SESSION_NOTIFICATION_ID, notification)
    }

    // Student/Work Mode (real schedule-based enforcement): same "kick to home, no
    // challenge bypass" shape as Focus Mode's block -- the only way in is waiting out
    // the schedule block (or, for Student Mode, being on the study allowlist).
    private fun handleScheduleBlock(currentTime: Long, title: String, description: String) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (currentTime - lastScheduleBlockNotifyTime < SCHEDULE_BLOCK_NOTIFICATION_THROTTLE_MS) return
        lastScheduleBlockNotifyTime = currentTime
        postScheduleBlockNotification(title, description)
    }

    private fun postScheduleBlockNotification(title: String, description: String) {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, SCHEDULE_BLOCK_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(SCHEDULE_BLOCK_NOTIFICATION_ID, notification)
        logNotification(type = "SCHEDULE_BLOCK", title = title, description = description)
    }

    private fun logAppSession(
        packageName: String,
        endTime: Long,
        scrollEventCount: Int = 0,
        maxScrollGapMs: Long = 0
    ) {
        val startTime = currentSessionStartTime ?: return
        val duration = endTime - startTime
        if (duration <= 0) return

        serviceScope.launch {
            try {
                val appName = getAppName(packageName)
                val dateKey = dateFormatter.format(Date(startTime))
                val log = AppUsageLog(
                    packageName = packageName,
                    appName = appName,
                    sessionStartTime = startTime,
                    sessionEndTime = endTime,
                    foregroundDurationMs = duration,
                    scrollEventCount = scrollEventCount,
                    maxScrollGapMs = maxScrollGapMs,
                    dateKey = dateKey
                )
                database.appUsageLogDao().insert(log)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Module 2 (AI-Based Behavior Analysis): doomscroll classifier's raw scroll signal.
    // Only counts scroll events for the app currently in the foreground, since a stray
    // TYPE_VIEW_SCROLLED can arrive from a different window mid-transition.
    private fun logScrollEvent(packageName: String, currentTime: Long) {
        if (packageName != currentForegroundPackage) return
        currentSessionScrollCount++
        val lastScrollTime = currentSessionLastScrollTime
        if (lastScrollTime != null) {
            val gap = currentTime - lastScrollTime
            if (gap > currentSessionMaxScrollGapMs) {
                currentSessionMaxScrollGapMs = gap
            }
        }
        currentSessionLastScrollTime = currentTime
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    override fun onInterrupt() {
        // Service interrupted by system
    }

    override fun onDestroy() {
        super.onDestroy()
        // Log final session if any
        currentForegroundPackage?.let {
            logAppSession(
                it,
                System.currentTimeMillis(),
                scrollEventCount = currentSessionScrollCount,
                maxScrollGapMs = currentSessionMaxScrollGapMs
            )
        }
    }
}