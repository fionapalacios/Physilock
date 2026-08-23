package com.example.physi_lock.ml

import android.content.Context
import com.example.physi_lock.data.PhysiLockDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pulls the current hour's Module 2 Logistic Regression II feature vector from real
 * Room DB data (AppUsageLog) — see ExcessiveUsageDetector.kt for the model itself.
 */
class ExcessiveUsageFeatureExtractor(context: Context) {
    private val db = PhysiLockDatabase.getInstance(context.applicationContext)
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // How far back "avgUsageThisHourMin" looks for a per-hour behavioral baseline.
    private val lookbackWindowMs = 30L * 24 * 60 * 60 * 1000L

    suspend fun extractCurrentHourFeatures(): ExcessiveUsageInputs {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val isWeekend = now.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
            now.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

        val todayKey = dateFormatter.format(Date())
        val cumulativeTodayMs = db.appUsageLogDao().getTotalDurationByDateOnce(todayKey) ?: 0L

        val avgThisHourMs = db.appUsageLogDao().getAvgDurationForHourOfDay(
            hour = currentHour,
            sinceMillis = System.currentTimeMillis() - lookbackWindowMs
        ) ?: 0.0

        val prevHourStart = (now.clone() as Calendar).apply {
            add(Calendar.HOUR_OF_DAY, -1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val currentHourStart = (now.clone() as Calendar).apply {
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val usagePrevHourMs = db.appUsageLogDao().getDurationInRange(prevHourStart, currentHourStart)

        return ExcessiveUsageInputs(
            avgUsageThisHourMin = avgThisHourMs / 60_000.0,
            cumulativeUsageTodayMin = cumulativeTodayMs / 60_000.0,
            usagePrevHourMin = usagePrevHourMs / 60_000.0,
            isWeekend = isWeekend
        )
    }
}
