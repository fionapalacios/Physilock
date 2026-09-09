package com.example.physi_lock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.physi_lock.data.entity.PomodoroSession
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroSessionDao {
    @Insert
    suspend fun insert(session: PomodoroSession): Long

    @Query("SELECT * FROM pomodoro_sessions WHERE endedAt IS NULL ORDER BY startTimeMillis DESC LIMIT 1")
    fun getActiveSession(): Flow<PomodoroSession?>

    @Query("SELECT * FROM pomodoro_sessions WHERE endedAt IS NULL ORDER BY startTimeMillis DESC LIMIT 1")
    suspend fun getActiveSessionOnce(): PomodoroSession?

    @Query("UPDATE pomodoro_sessions SET phase = :phase, phaseStartTimeMillis = :phaseStartTimeMillis, sessionNumber = :sessionNumber, pausedAt = NULL WHERE id = :id")
    suspend fun advancePhase(id: Long, phase: String, phaseStartTimeMillis: Long, sessionNumber: Int)

    @Query("UPDATE pomodoro_sessions SET pausedAt = :pausedAt WHERE id = :id")
    suspend fun setPaused(id: Long, pausedAt: Long)

    @Query("UPDATE pomodoro_sessions SET pausedAt = NULL, phaseStartTimeMillis = :phaseStartTimeMillis WHERE id = :id")
    suspend fun resume(id: Long, phaseStartTimeMillis: Long)

    @Query("UPDATE pomodoro_sessions SET endedAt = :endedAt WHERE id = :id")
    suspend fun endSession(id: Long, endedAt: Long)
}
