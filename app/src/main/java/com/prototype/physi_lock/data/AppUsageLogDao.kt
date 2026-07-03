package com.prototype.physi_lock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class PackageUsageTotal(
    val packageName: String,
    val appName: String,
    val totalMs: Long
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

    @Query(
        "SELECT packageName, appName, SUM(foregroundDurationMs) as totalMs " +
            "FROM app_usage_logs WHERE dateKey = :dateKey " +
            "GROUP BY packageName ORDER BY totalMs DESC"
    )
    fun getUsageByPackageForDate(dateKey: String): Flow<List<PackageUsageTotal>>

    @Query("DELETE FROM app_usage_logs WHERE julianday(datetime(sessionStartTime / 1000, 'unixepoch')) < julianday('now', '-90 days')")
    suspend fun deleteOldLogs()
}
