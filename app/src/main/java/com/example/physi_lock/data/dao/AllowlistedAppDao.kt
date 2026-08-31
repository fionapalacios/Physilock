package com.example.physi_lock.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.example.physi_lock.data.entity.AllowlistedApp

@Dao
interface AllowlistedAppDao {
    @Upsert
    suspend fun upsert(app: AllowlistedApp)

    @Query("SELECT * FROM allowlisted_apps")
    fun getAll(): Flow<List<AllowlistedApp>>

    @Query("DELETE FROM allowlisted_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
