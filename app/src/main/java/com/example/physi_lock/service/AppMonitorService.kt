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
import com.example.physi_lock.data.AppCategoryType
import com.example.physi_lock.data.AppUsageLog
import com.example.physi_lock.data.ExcessiveUsagePredictionLog
import com.example.physi_lock.data.MotionInterventionLog
import com.example.physi_lock.data.NotificationLog
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.ScheduleBlock
import com.example.physi_lock.data.UsageStatsRepository
import com.example.physi_lock.data.currentWifiSsid
import com.example.physi_lock.ml.DoomscrollDetector
import com.example.physi_lock.ml.DoomscrollInputs
import com.example.physi_lock.ml.ExcessiveUsageDetector
import com.example.physi_lock.ml.ExcessiveUsageFeatureExtractor
import com.example.physi_lock.ml.RiskFeatureExtractor
import com.example.physi_lock.ml.RiskLevel
import com.example.physi_lock.ml.RiskScoringEngine
import com.example.physi_lock.ui.lock.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

        private const val DOOMSCROLL_ALERT_CHANNEL_ID = "doomscroll_alert_channel"
        private const val DOOMSCROLL_ALERT_NOTIFICATION_ID = 1003
        // Doomscroll checks run against in-memory session state (cheap), so this only
        // needs to throttle how chatty the model calls are, not IPC cost.
        private const val DOOMSCROLL_CHECK_THROTTLE_MS = 15 * 1000L
        // Once flagged, don't re-alert for the same continuing scroll binge.
        private const val DOOMSCROLL_ALERT_COOLDOWN_MS = 10 * 60 * 1000L
        // Matches the "every 5 minutes" cadence PROJECT_DOCUMENTATION.md's risk
        // scoring pipeline describes — the doomscroll "recipe" selector (see
        // DoomscrollDetector.kt) doesn't need every-scroll-event freshness.
        private const val RISK_REFRESH_INTERVAL_MS = 5 * 60 * 1000L

        private const val EXCESSIVE_USAGE_PREDICTION_CHANNEL_ID = "excessive_usage_prediction_channel"
        private const val EXCESSIVE_USAGE_PREDICTION_NOTIFICATION_ID = 1004
        // One EXCESSIVE_USAGE_PREDICTION row per hour is enough — checking more
        // often than this wouldn't change the hour-bucketed prediction anyway.
        private const val EXCESSIVE_USAGE_CHECK_INTERVAL_MS = 15 * 60 * 1000L

        private const val FOCUS_BLOCK_CHANNEL_ID = "focus_block_channel"
        private const val FOCUS_BLOCK_NOTIFICATION_ID = 1005
        // A blocked app can be repeatedly relaunched (e.g. from a home-screen widget);
        // this throttles the notification/log spam without affecting the actual block,
        // which always fires on every attempt.
        private const val FOCUS_BLOCK_NOTIFICATION_THROTTLE_MS = 60 * 1000L

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
    // Admin-curated Social Media / Entertainment categories (see FocusModeViewModel —
    // same source of truth, so the UI's "blocked" chips and the actual enforcement here
    // can never drift apart).
    @Volatile private var focusModeActive: Boolean = false
    @Volatile private var focusBlockedPackages: Set<String> = emptySet()
    private var lastFocusBlockNotifyTime: Long = 0L
    // Module 7 (Context-Aware AI): Wi-Fi-network-matched Context Alerts. Not GPS
    // geofencing -- real location-based locking is a Future Enhancement per the
    // manuscript, out of MVP scope; this matches by Wi-Fi network name instead, which
    // needs no new location SDK dependency. Reuses focusBlockedPackages (Admin-curated
    // Social Media / Entertainment categories) as the same "distracting apps" set Focus
    // Mode blocks -- alerting here is passive (a notification), not an enforced block.
    @Volatile private var contextAlertsEnabled: Boolean = false
    @Volatile private var contextAlertWifiSsid: String? = null
    @Volatile private var cachedWifiSsid: String? = null
    private var lastContextAlertNotifyTime: Long = 0L
    // Student/Work Mode real schedule-based enforcement: userMode wasn't previously
    // mirrored here (nothing before this needed it at enforcement time). scheduleBlocks
    // holds every block for both modes (filtered by mode at check time, since the active
    // mode can change while the service keeps running); allowlistedPackages is Student
    // Mode's "stays reachable during a block" set. Same live-Flow-collector pattern as
    // lockedPackages/focusBlockedPackages above.
    @Volatile private var userMode: String = "STUDENT_MODE"
    @Volatile private var scheduleBlocks: List<ScheduleBlock> = emptyList()
    @Volatile private var allowlistedPackages: Set<String> = emptySet()
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
        createDoomscrollAlertNotificationChannel()
        createExcessiveUsagePredictionNotificationChannel()
        createFocusBlockNotificationChannel()
        createContextAlertNotificationChannel()
        createScheduleBlockNotificationChannel()

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
                userMode = config?.userMode ?: "STUDENT_MODE"
            }
        }

        // Student/Work Mode: live-reload schedule blocks (both modes) and Student
        // Mode's study app allowlist, same pattern as lockedPackages/focusBlockedPackages.
        serviceScope.launch {
            database.scheduleBlockDao().getAll().collect { blocks ->
                scheduleBlocks = blocks
            }
        }
        serviceScope.launch {
            database.allowlistedAppDao().getAll().collect { apps ->
                allowlistedPackages = apps.map { it.packageName }.toSet()
            }
        }

        // Module 6 (Personalization & User Control): live-reload Focus Mode's active
        // session and its real blocked-app set, same pattern as lockedPackages above.
        serviceScope.launch {
            database.focusSessionDao().getActiveSession().collect { session ->
                focusModeActive = session != null
            }
        }
        serviceScope.launch {
            database.appCategoryDao().getAll().collect { categories ->
                focusBlockedPackages = categories
                    .filter { it.category == AppCategoryType.SOCIAL_MEDIA || it.category == AppCategoryType.ENTERTAINMENT }
                    .map { it.packageName }
                    .toSet()
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

    private fun createDoomscrollAlertNotificationChannel() {
        val channel = NotificationChannel(
            DOOMSCROLL_ALERT_CHANNEL_ID,
            "Doomscrolling Detection",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Warns when scrolling patterns suggest doomscrolling"
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

    private fun createFocusBlockNotificationChannel() {
        val channel = NotificationChannel(
            FOCUS_BLOCK_CHANNEL_ID,
            "Focus Mode",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when an app is blocked during an active Focus Mode session"
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
        postDoomscrollAlertNotification()

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

    private fun postDoomscrollAlertNotification() {
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

        val notification = NotificationCompat.Builder(this, DOOMSCROLL_ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Doomscrolling detected")
            .setContentText("Your scrolling pattern looks like a doomscroll — maybe take a break?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(DOOMSCROLL_ALERT_NOTIFICATION_ID, notification)
        logNotification(
            type = "DOOMSCROLL_ALERT",
            title = "Doomscrolling detected",
            description = "Your scrolling pattern looks like a doomscroll — maybe take a break?"
        )
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
        val scheduleBlock = activeScheduleBlock()
        if (packageName in lockedPackages && !isTemporarilyUnlocked(packageName, currentTime)) {
            val intent = Intent(this, LockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        } else if (userMode == "STUDENT_MODE" && scheduleBlock != null &&
            packageName !in allowlistedPackages && !isSystemPackage(packageName)
        ) {
            handleScheduleBlock(
                currentTime,
                title = "Class Mode active",
                description = "${getAppName(packageName)} is blocked during ${scheduleBlock.label}"
            )
        } else if (userMode == "WORK_MODE" && scheduleBlock != null && packageName in focusBlockedPackages) {
            handleScheduleBlock(
                currentTime,
                title = "Work Hours active",
                description = "${getAppName(packageName)} is blocked during ${scheduleBlock.label}"
            )
        } else if (focusModeActive && packageName in focusBlockedPackages) {
            handleFocusBlock(packageName, currentTime)
        } else if (contextAlertsEnabled && packageName in focusBlockedPackages && isOnWatchedWifi()) {
            handleContextAlert(packageName, currentTime)
        }
    }

    // Student/Work Mode (real schedule-based enforcement): a ScheduleBlock is active
    // right now for the active mode, or null. Cheap synchronous check against the
    // cached list only -- no DB access on the accessibility-event thread, same
    // constraint as the rest of this priority chain.
    private fun activeScheduleBlock(): ScheduleBlock? {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return scheduleBlocks.firstOrNull { block ->
            block.mode == userMode && block.dayOfWeek == dayOfWeek &&
                minuteOfDay >= block.startMinute && minuteOfDay < block.endMinute
        }
    }

    // Work Mode "quiet hours": passive nudge notifications (Break Reminder, Overuse
    // Alert, Excessive Usage Prediction) are held while a Work Mode schedule block is
    // active -- Doomscroll Alert, Context Alert, and the schedule block notification
    // itself are unaffected, since those are active-intervention signals, not FYI nudges.
    private fun isQuietHours(): Boolean = userMode == "WORK_MODE" && activeScheduleBlock() != null

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

    // Focus Mode (Module 6): unlike Adaptive/App Lock's challenge-to-unlock, an app
    // blocked during Focus Mode is simply kicked back to the home screen — there's no
    // "solve a challenge to get in" bypass, the only way in is ending the session.
    private fun handleFocusBlock(packageName: String, currentTime: Long) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (currentTime - lastFocusBlockNotifyTime < FOCUS_BLOCK_NOTIFICATION_THROTTLE_MS) return
        lastFocusBlockNotifyTime = currentTime
        postFocusBlockNotification(packageName)
    }

    private fun postFocusBlockNotification(packageName: String) {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val appName = getAppName(packageName)
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, FOCUS_BLOCK_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("$appName blocked")
            .setContentText("$appName is blocked while Focus Mode is active")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(FOCUS_BLOCK_NOTIFICATION_ID, notification)
        logNotification(
            type = "FOCUS_BLOCK",
            title = "$appName blocked",
            description = "$appName is blocked while Focus Mode is active"
        )
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