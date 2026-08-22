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
import com.example.physi_lock.data.AppUsageLog
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.data.UsageStatsRepository
import com.example.physi_lock.ui.lock.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppMonitorService : AccessibilityService() {

    companion object {
        var lastUnlockTime = 0L

        fun triggerGlobalUnlock() {
            lastUnlockTime = System.currentTimeMillis()
        }

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
    }

    @Volatile private var lockedPackages: Set<String> = emptySet()
    @Volatile private var breakReminderEnabled: Boolean = true
    @Volatile private var breakReminderIntervalMs: Long = 30 * 60 * 1000L
    @Volatile private var overuseAlertsEnabled: Boolean = true
    @Volatile private var dailyScreenTimeThresholdMs: Long = 480 * 60 * 1000L
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: PhysiLockDatabase
    private lateinit var usageStatsRepository: UsageStatsRepository
    private var currentForegroundPackage: String? = null
    private var currentSessionStartTime: Long? = null
    private var continuousUsageStartTime: Long? = null
    private var lastActivityEventTime: Long? = null
    private var lastOveruseCheckTime: Long = 0L
    private var lastOveruseAlertDateKey: String? = null
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate() {
        super.onCreate()
        database = PhysiLockDatabase.getInstance(this)
        usageStatsRepository = UsageStatsRepository(this)
        createBreakReminderNotificationChannel()
        createOveruseAlertNotificationChannel()

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
        }
        lastActivityEventTime = currentTime

        if (!breakReminderEnabled) return
        val startTime = continuousUsageStartTime ?: return
        val elapsed = currentTime - startTime
        if (elapsed >= breakReminderIntervalMs) {
            postBreakReminderNotification(elapsed)
            continuousUsageStartTime = currentTime
        }
    }

    private fun postBreakReminderNotification(elapsedMs: Long) {
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
    }

    private fun handleWindowStateChange(packageName: String, currentTime: Long) {
        // Log the previous app session if it changed
        if (currentForegroundPackage != null && currentForegroundPackage != packageName) {
            logAppSession(currentForegroundPackage!!, currentTime)
        }

        // Update current foreground app
        currentForegroundPackage = packageName
        currentSessionStartTime = currentTime

        // Check if app should be locked
        if (packageName in lockedPackages && !isTemporarilyUnlocked(packageName, currentTime)) {
            val intent = Intent(this, LockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        }
    }

    private fun logAppSession(packageName: String, endTime: Long) {
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
                    dateKey = dateKey
                )
                database.appUsageLogDao().insert(log)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun logScrollEvent(packageName: String, currentTime: Long) {
        // Module 2 (AI-Based Behavior Analysis): increment scroll counter
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
            logAppSession(it, System.currentTimeMillis())
        }
    }
}