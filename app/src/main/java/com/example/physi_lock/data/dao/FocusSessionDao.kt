package com.example.physi_lock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.physi_lock.data.entity.FocusSession

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSession): Long

    // At most one row should ever have endTimeMillis IS NULL at a time (start() only
    // inserts a new one when this is null), but LIMIT 1 keeps the query well-defined
    // even if that invariant is ever violated.
    @Query("SELECT * FROM focus_sessions WHERE endTimeMillis IS NULL ORDER BY startTimeMillis DESC LIMIT 1")
    fun getActiveSession(): Flow<FocusSession?>

    @Query("UPDATE focus_sessions SET endTimeMillis = :endTimeMillis, creditMinutesEarned = :creditMinutesEarned WHERE id = :id")
    suspend fun endSession(id: Long, endTimeMillis: Long, creditMinutesEarned: Int)
}
