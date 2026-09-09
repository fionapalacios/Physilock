package com.example.physi_lock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.physi_lock.data.entity.DeepWorkSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface DeepWorkScheduleDao {
    @Insert
    suspend fun insert(schedule: DeepWorkSchedule): Long

    @Query("SELECT * FROM deep_work_schedules ORDER BY startMinute")
    fun getAll(): Flow<List<DeepWorkSchedule>>

    @Query("UPDATE deep_work_schedules SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM deep_work_schedules WHERE id = :id")
    suspend fun delete(id: Long)
}
