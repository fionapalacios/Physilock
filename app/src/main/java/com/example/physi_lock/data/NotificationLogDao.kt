package com.example.physi_lock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {
    @Insert
    suspend fun insert(log: NotificationLog)

    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecent(): Flow<List<NotificationLog>>

    @Query("UPDATE notification_logs SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllRead()

    @Query("UPDATE notification_logs SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Int)
}
