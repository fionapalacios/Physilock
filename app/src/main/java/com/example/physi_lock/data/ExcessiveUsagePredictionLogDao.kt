package com.example.physi_lock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcessiveUsagePredictionLogDao {
    @Insert
    suspend fun insert(log: ExcessiveUsagePredictionLog)

    @Query("SELECT * FROM excessive_usage_predictions WHERE dateKey = :dateKey ORDER BY hour DESC")
    fun getPredictionsByDate(dateKey: String): Flow<List<ExcessiveUsagePredictionLog>>

    @Query("SELECT COUNT(*) FROM excessive_usage_predictions WHERE dateKey = :dateKey AND hour = :hour")
    suspend fun countForHour(dateKey: String, hour: Int): Int

    @Query("DELETE FROM excessive_usage_predictions WHERE julianday(datetime(createdAt / 1000, 'unixepoch')) < julianday('now', '-90 days')")
    suspend fun deleteOldPredictions()
}
