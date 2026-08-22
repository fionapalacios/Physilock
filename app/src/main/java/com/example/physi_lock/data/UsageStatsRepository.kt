package com.example.physi_lock.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import java.util.Calendar

data class AppUsageSummary(val packageName: String, val totalTimeMs: Long)

class UsageStatsRepository(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getTodayUsage(): List<AppUsageSummary> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        return getUsageForRange(cal.timeInMillis, System.currentTimeMillis())
    }

    /** [startMillis]/[endMillis] should bound a single day — UsageStatsManager's
     *  multi-day aggregation behavior isn't reliable enough to trust across a wider
     *  range, so callers wanting a multi-day total should call this once per day
     *  and sum the results themselves (see ReportsViewModel). */
    fun getUsageForRange(startMillis: Long, endMillis: Long): List<AppUsageSummary> {
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startMillis, endMillis
        )
        return stats
            .filter { it.totalTimeInForeground > 0 }
            .map { AppUsageSummary(it.packageName, it.totalTimeInForeground) }
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