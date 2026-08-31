package com.example.physi_lock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class DailyCount(val dateKey: String, val count: Int)

@Dao
interface LoginEventDao {
    // IGNORE relies on the (accountId, dateKey) unique index -- a second call for the same
    // account on the same day is a no-op, so callers don't need to check first.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: LoginEvent)

    // Admin Analytics "Daily Active Users" -- distinct accounts seen signed in per day.
    @Query(
        "SELECT dateKey, COUNT(DISTINCT accountId) as count FROM login_events " +
        "WHERE dateKey BETWEEN :startDateKey AND :endDateKey GROUP BY dateKey"
    )
    suspend fun getDailyActiveUserCounts(startDateKey: String, endDateKey: String): List<DailyCount>

    @Query("DELETE FROM login_events WHERE julianday(dateKey) < julianday('now', '-90 days')")
    suspend fun deleteOldEvents()
}
