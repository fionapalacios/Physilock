package com.example.physi_lock.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReflectionEntryDao {
    @Upsert
    suspend fun upsert(entry: ReflectionEntry)

    @Query("SELECT * FROM reflection_entries WHERE dateKey = :dateKey")
    fun getForDate(dateKey: String): Flow<ReflectionEntry?>

    @Query("SELECT * FROM reflection_entries ORDER BY dateKey DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ReflectionEntry>>
}
