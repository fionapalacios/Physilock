package com.example.physi_lock.ml

import android.content.Context
import com.example.physi_lock.data.AppCategoryType
import com.example.physi_lock.data.PhysiLockDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class RiskFeatures(
    val dailyScreenTimeMin: Double,
    val avgSessionLengthMin: Double,
    val appLaunchFrequency: Double,
    val socialMediaFraction: Double,
    val bypassAttemptCount: Double,
    val doomscrollEpisodeCount: Double
)

/**
 * Pulls today's Module 2 feature vector from real logged data — see AppMonitorService's
 * session/scroll/doomscroll logging and LockActivity's bypass-attempt logging.
 */
class RiskFeatureExtractor(context: Context) {
    private val db = PhysiLockDatabase.getInstance(context.applicationContext)
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun extractTodayFeatures(): RiskFeatures {
        val todayKey = dateFormatter.format(Date())
        val dayStartMillis = todayStartMillis()

        val totalDurationMs = db.appUsageLogDao().getTotalDurationByDateOnce(todayKey) ?: 0L
        val sessionCount = db.appUsageLogDao().getSessionCountByDate(todayKey)
        val socialMediaDurationMs =
            db.appUsageLogDao().getDurationByCategoryAndDate(AppCategoryType.SOCIAL_MEDIA, todayKey)
        val bypassAttempts = db.motionInterventionLogDao().countBypassAttemptsAfter(dayStartMillis)
        val doomscrollEpisodes = db.motionInterventionLogDao().countDoomscrollAlertsAfter(dayStartMillis)

        val dailyScreenTimeMin = totalDurationMs / 60_000.0
        val avgSessionLengthMin = if (sessionCount > 0) dailyScreenTimeMin / sessionCount else 0.0
        val socialMediaFraction = if (totalDurationMs > 0) {
            (socialMediaDurationMs.toDouble() / totalDurationMs).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        return RiskFeatures(
            dailyScreenTimeMin = dailyScreenTimeMin,
            avgSessionLengthMin = avgSessionLengthMin,
            appLaunchFrequency = sessionCount.toDouble(),
            socialMediaFraction = socialMediaFraction,
            bypassAttemptCount = bypassAttempts.toDouble(),
            doomscrollEpisodeCount = doomscrollEpisodes.toDouble()
        )
    }

    private fun todayStartMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
