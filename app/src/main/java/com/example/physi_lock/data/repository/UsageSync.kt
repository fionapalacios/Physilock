package com.example.physi_lock.data.repository

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.physi_lock.data.db.PhysiLockDatabase
import com.example.physi_lock.data.entity.UsageSession

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