package com.prototype.physi_lock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSessionDao {
    @Insert
    suspend fun insert(session: UsageSession)

    @Query("SELECT * FROM usage_sessions WHERE dateKey = :dateKey ORDER BY startTime DESC")
    fun getSessionsForDate(dateKey: String): Flow<List<UsageSession>>

    @Query("SELECT SUM(durationMs) FROM usage_sessions WHERE dateKey = :dateKey")
    fun getTotalDurationForDate(dateKey: String): Flow<Long?>

    @Query("SELECT SUM(durationMs) FROM usage_sessions WHERE packageName = :packageName AND dateKey = :dateKey")
    fun getTotalDurationForApp(packageName: String, dateKey: String): Flow<Long?>

    @Query("DELETE FROM usage_sessions WHERE dateKey = :dateKey")
    suspend fun clearForDate(dateKey: String)
}
