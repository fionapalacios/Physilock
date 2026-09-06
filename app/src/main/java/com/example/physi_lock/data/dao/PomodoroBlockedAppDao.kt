package com.example.physi_lock.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.physi_lock.data.entity.PomodoroBlockedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroBlockedAppDao {
    @Upsert
    suspend fun upsert(app: PomodoroBlockedApp)

    @Query("SELECT * FROM pomodoro_blocked_apps")
    fun getAll(): Flow<List<PomodoroBlockedApp>>

    @Query("DELETE FROM pomodoro_blocked_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
