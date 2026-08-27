package com.example.physi_lock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.physi_lock.data.entity.ScheduleBlock

@Dao
interface ScheduleBlockDao {
    @Insert
    suspend fun insert(block: ScheduleBlock): Long

    @Query("SELECT * FROM schedule_blocks")
    fun getAll(): Flow<List<ScheduleBlock>>

    @Query("SELECT * FROM schedule_blocks WHERE mode = :mode ORDER BY dayOfWeek, startMinute")
    fun getByMode(mode: String): Flow<List<ScheduleBlock>>

    @Query("DELETE FROM schedule_blocks WHERE id = :id")
    suspend fun delete(id: Long)
}
