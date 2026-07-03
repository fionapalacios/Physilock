package com.prototype.physi_lock.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getTodayDateKey(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

suspend fun syncTodayUsage(db: PhysiLockDatabase, usageRepo: UsageStatsRepository) {
    val dateKey = getTodayDateKey()
    val now = System.currentTimeMillis()
    db.usageSessionDao().clearForDate(dateKey)
    usageRepo.getTodayUsage().forEach { summary ->
        db.usageSessionDao().insert(
            UsageSession(
                packageName = summary.packageName,
                appName = summary.packageName,
                startTime = now - summary.totalTimeMs,
                endTime = now,
                durationMs = summary.totalTimeMs,
                dateKey = dateKey
            )
        )
    }
}
