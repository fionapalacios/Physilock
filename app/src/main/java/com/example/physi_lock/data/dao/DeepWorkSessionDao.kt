package com.example.physi_lock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.physi_lock.data.entity.DeepWorkSession

@Dao
interface DeepWorkSessionDao {
    @Insert
    suspend fun insert(session: DeepWorkSession): Long

    @Query("SELECT * FROM deep_work_sessions WHERE endTimeMillis IS NULL ORDER BY startTimeMillis DESC LIMIT 1")
    fun getActiveSession(): Flow<DeepWorkSession?>

    @Query("UPDATE deep_work_sessions SET endTimeMillis = :endTimeMillis, endedEarly = :endedEarly, creditMinutesEarned = :creditMinutesEarned WHERE id = :id")
    suspend fun endSession(id: Long, endTimeMillis: Long, endedEarly: Boolean, creditMinutesEarned: Int)
}
