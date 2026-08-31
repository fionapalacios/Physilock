package com.example.physi_lock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MotionInterventionLogDao {
    @Insert
    suspend fun insert(log: MotionInterventionLog)

    @Query("SELECT * FROM motion_intervention_logs ORDER BY interventionTimestamp DESC LIMIT :limit")
    fun getRecentInterventions(limit: Int = 100): Flow<List<MotionInterventionLog>>

    @Query("SELECT * FROM motion_intervention_logs WHERE packageName = :packageName ORDER BY interventionTimestamp DESC LIMIT :limit")
    fun getInterventionsByPackage(packageName: String, limit: Int = 50): Flow<List<MotionInterventionLog>>

    @Query("SELECT COUNT(*) FROM motion_intervention_logs WHERE userResponse = 'UNLOCKED' AND interventionTimestamp > :afterTimestamp")
    fun getSuccessfulUnlocksCount(afterTimestamp: Long): Flow<Int>

    // A "bypass attempt" is a lock challenge the user was shown but left without completing
    // (back button, home button, recents-swipe — anything that stops LockActivity before
    // onComplete fires). See LockActivity.onStop(). Module 2 Random Forest feature.
    @Query("SELECT COUNT(*) FROM motion_intervention_logs WHERE userResponse = 'DISMISSED' AND interventionTimestamp > :afterTimestamp")
    suspend fun countBypassAttemptsAfter(afterTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM motion_intervention_logs WHERE userResponse = 'DISMISSED' AND packageName = :packageName AND interventionTimestamp > :afterTimestamp")
    suspend fun countBypassAttemptsByPackageAfter(packageName: String, afterTimestamp: Long): Int

    // Module 2 Random Forest feature — the count of DOOMSCROLL_ALERT rows Logistic
    // Regression I has logged today (see AppMonitorService.checkDoomscrolling).
    @Query("SELECT COUNT(*) FROM motion_intervention_logs WHERE triggerType = 'DOOMSCROLL_ALERT' AND interventionTimestamp > :afterTimestamp")
    suspend fun countDoomscrollAlertsAfter(afterTimestamp: Long): Int

    // Home's Doomscrolling banner (ported from the teammate's DashboardScreen, real data
    // instead of their static mock) — most recent real alert, so the banner can name which
    // app it fired on and how long ago.
    @Query(
        "SELECT * FROM motion_intervention_logs WHERE triggerType = 'DOOMSCROLL_ALERT' " +
        "AND interventionTimestamp > :afterTimestamp ORDER BY interventionTimestamp DESC LIMIT 1"
    )
    suspend fun getMostRecentDoomscrollAlertAfter(afterTimestamp: Long): MotionInterventionLog?

    @Query("SELECT AVG(riskScore) FROM motion_intervention_logs WHERE interventionTimestamp > :afterTimestamp")
    fun getAverageRiskScore(afterTimestamp: Long): Flow<Double?>

    @Query("SELECT * FROM motion_intervention_logs WHERE interventionTimestamp > :afterTimestamp ORDER BY interventionTimestamp DESC")
    fun getInterventionsSince(afterTimestamp: Long): Flow<List<MotionInterventionLog>>

    @Query("SELECT COUNT(DISTINCT date(datetime(interventionTimestamp / 1000, 'unixepoch'))) FROM motion_intervention_logs WHERE userResponse = 'UNLOCKED' AND interventionTimestamp > :afterTimestamp")
    suspend fun countSuccessfulInterventionDaysAfter(afterTimestamp: Long): Int

    // Admin Analytics "Motion Challenges Completed" chart — per-day count of successful
    // unlocks since afterTimestamp, same day-bucketing as [countSuccessfulInterventionDaysAfter].
    @Query(
        "SELECT date(datetime(interventionTimestamp / 1000, 'unixepoch')) as dateKey, COUNT(*) as count " +
        "FROM motion_intervention_logs WHERE userResponse = 'UNLOCKED' AND interventionTimestamp > :afterTimestamp " +
        "GROUP BY dateKey"
    )
    suspend fun getDailyCompletedCounts(afterTimestamp: Long): List<DailyCount>

    @Query("SELECT COALESCE(SUM(xpEarned), 0) FROM motion_intervention_logs WHERE userResponse = 'UNLOCKED'")
    fun getTotalXp(): Flow<Int>

    // Move hub's per-challenge "Completed" badge (see MoveScreen/MoveViewModel) —
    // which of today's voluntary challenges are already done.
    @Query("SELECT DISTINCT challengeType FROM motion_intervention_logs WHERE triggerType = 'VOLUNTARY_CHALLENGE' AND userResponse = 'UNLOCKED' AND interventionTimestamp > :afterTimestamp AND challengeType IS NOT NULL")
    fun getCompletedChallengeTypesAfter(afterTimestamp: Long): Flow<List<String>>

    @Query("DELETE FROM motion_intervention_logs WHERE julianday(datetime(interventionTimestamp / 1000, 'unixepoch')) < julianday('now', '-90 days')")
    suspend fun deleteOldInterventions()
}
