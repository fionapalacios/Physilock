package com.example.physi_lock.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.example.physi_lock.data.entity.FocusBlockedApp

@Dao
interface FocusBlockedAppDao {
    @Upsert
    suspend fun upsert(app: FocusBlockedApp)

    @Query("SELECT * FROM focus_blocked_apps")
    fun getAll(): Flow<List<FocusBlockedApp>>

    @Query("DELETE FROM focus_blocked_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
