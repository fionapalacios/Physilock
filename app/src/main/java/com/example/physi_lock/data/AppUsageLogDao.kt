package com.example.physi_lock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class AppUsageTotal(
    val packageName: String,
    val appName: String,
    val totalDurationMs: Long
)

@Dao
interface AppUsageLogDao {
    @Insert
    suspend fun insert(log: AppUsageLog)

    @Query("SELECT * FROM app_usage_logs WHERE packageName = :packageName AND dateKey = :dateKey ORDER BY sessionStartTime DESC")
    fun getLogsByPackageAndDate(packageName: String, dateKey: String): Flow<List<AppUsageLog>>

    @Query("SELECT * FROM app_usage_logs WHERE dateKey = :dateKey ORDER BY sessionStartTime DESC")
    fun getLogsByDate(dateKey: String): Flow<List<AppUsageLog>>

    @Query("SELECT * FROM app_usage_logs ORDER BY sessionStartTime DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<AppUsageLog>>

    @Query("SELECT SUM(foregroundDurationMs) as totalDuration FROM app_usage_logs WHERE dateKey = :dateKey")
    fun getTotalDurationByDate(dateKey: String): Flow<Long?>

    @Query("SELECT SUM(scrollEventCount) as totalScrolls FROM app_usage_logs WHERE packageName = :packageName AND dateKey = :dateKey")
    fun getTotalScrollsByPackageAndDate(packageName: String, dateKey: String): Flow<Int?>

    @Query("SELECT SUM(foregroundDurationMs) FROM app_usage_logs WHERE dateKey = :dateKey")
    suspend fun getTotalDurationByDateOnce(dateKey: String): Long?

    // Module 2 (AI-Based Behavior Analysis) feature extraction — one row per app-open
    // session, so a row count is exactly "application launch frequency" for the day.
    @Query("SELECT COUNT(*) FROM app_usage_logs WHERE dateKey = :dateKey")
    suspend fun getSessionCountByDate(dateKey: String): Int

    @Query(
        "SELECT COALESCE(SUM(u.foregroundDurationMs), 0) FROM app_usage_logs u " +
        "INNER JOIN app_categories c ON u.packageName = c.packageName " +
        "WHERE c.category = :category AND u.dateKey = :dateKey"
    )
    suspend fun getDurationByCategoryAndDate(category: String, dateKey: String): Long

    // Module 2 Logistic Regression II (hourly excessive-usage prediction) feature:
    // average usage during this specific hour-of-day (local time), across days since
    // sinceMillis where there was any usage in that hour. Days with zero usage in the
    // hour don't contribute a 0 row, which slightly overestimates the "typical" value —
    // an accepted simplification, see ml/README.md.
    @Query(
        "SELECT AVG(hourlyTotal) FROM (" +
        "  SELECT dateKey, SUM(foregroundDurationMs) as hourlyTotal FROM app_usage_logs" +
        "  WHERE CAST(strftime('%H', datetime(sessionStartTime / 1000, 'unixepoch', 'localtime')) AS INTEGER) = :hour" +
        "    AND sessionStartTime >= :sinceMillis" +
        "  GROUP BY dateKey" +
        ")"
    )
    suspend fun getAvgDurationForHourOfDay(hour: Int, sinceMillis: Long): Double?

    @Query("SELECT COALESCE(SUM(foregroundDurationMs), 0) FROM app_usage_logs WHERE sessionStartTime >= :startMillis AND sessionStartTime < :endMillis")
    suspend fun getDurationInRange(startMillis: Long, endMillis: Long): Long

    @Query(
        "SELECT packageName, appName, SUM(foregroundDurationMs) as totalDurationMs " +
        "FROM app_usage_logs WHERE dateKey = :dateKey " +
        "GROUP BY packageName ORDER BY totalDurationMs DESC"
    )
    fun getAppTotalsByDate(dateKey: String): Flow<List<AppUsageTotal>>

    @Query(
        "SELECT packageName, appName, SUM(foregroundDurationMs) as totalDurationMs " +
        "FROM app_usage_logs WHERE dateKey BETWEEN :startDateKey AND :endDateKey " +
        "GROUP BY packageName ORDER BY totalDurationMs DESC"
    )
    fun getAppTotalsByDateRange(startDateKey: String, endDateKey: String): Flow<List<AppUsageTotal>>

    @Query("DELETE FROM app_usage_logs WHERE julianday(datetime(sessionStartTime / 1000, 'unixepoch')) < julianday('now', '-90 days')")
    suspend fun deleteOldLogs()
}
