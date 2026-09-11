package com.example.physi_lock.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import java.util.Calendar

data class AppUsageSummary(val packageName: String, val totalTimeMs: Long)

class UsageStatsRepository(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // UsageStatsManager reports the device's home screen/launcher as just another
    // "app" with real foreground time (visible between app switches, widget/search
    // use, etc.) -- resolved once and excluded below so it doesn't show up as noise
    // in Usage Today / Reports / the daily-limit total, matching the convention
    // every comparable screen-time app (Digital Wellbeing included) follows despite
    // reading the same underlying API (2026-09-04).
    private val launcherPackageName: String? by lazy {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.packageName
        } catch (e: Exception) {
            null
        }
    }

    fun getTodayUsage(): List<AppUsageSummary> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        return getUsageForRange(cal.timeInMillis, System.currentTimeMillis())
    }

    /** [startMillis]/[endMillis] should bound a single day — reconstructed from raw
     *  [UsageEvents] (MOVE_TO_FOREGROUND/MOVE_TO_BACKGROUND pairs) rather than
     *  `queryUsageStats(INTERVAL_DAILY, ...)`'s pre-aggregated OS buckets, which lag
     *  until the OS flushes them and don't strictly bound to the requested range —
     *  the root cause of screen-time totals not matching Digital Wellbeing/Settings
     *  (2026-09-10). Callers wanting a multi-day total should still call this once
     *  per day and sum the results themselves (see ReportsViewModel). */
    fun getUsageForRange(startMillis: Long, endMillis: Long): List<AppUsageSummary> {
        val events = usageStatsManager.queryEvents(startMillis, endMillis)
        val event = UsageEvents.Event()
        val foregroundSince = mutableMapOf<String, Long>()
        val totals = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    foregroundSince[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val since = foregroundSince.remove(event.packageName)
                    if (since != null) {
                        val duration = (event.timeStamp - since).coerceAtLeast(0)
                        totals[event.packageName] = (totals[event.packageName] ?: 0) + duration
                    }
                }
            }
        }

        // Any package still foreground when the range ends (e.g. "today" queried
        // mid-session) gets credited up to now, not silently dropped.
        val rangeEnd = minOf(endMillis, System.currentTimeMillis())
        foregroundSince.forEach { (packageName, since) ->
            val duration = (rangeEnd - since).coerceAtLeast(0)
            totals[packageName] = (totals[packageName] ?: 0) + duration
        }

        return totals
            .filterKeys { it != launcherPackageName }
            .filterValues { it > 0 }
            .map { (packageName, totalTimeMs) -> AppUsageSummary(packageName, totalTimeMs) }
            .sortedByDescending { it.totalTimeMs }
    }

    fun getAppLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    fun hasUsageAccessPermission(): Boolean {
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - 60000, System.currentTimeMillis()
        )
        return stats.isNotEmpty()
    }
}

fun openUsageAccessSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
}