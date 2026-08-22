package com.example.physi_lock.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCategoryDao {
    @Upsert
    suspend fun upsert(category: AppCategory)

    @Query("SELECT * FROM app_categories")
    fun getAll(): Flow<List<AppCategory>>

    @Query("DELETE FROM app_categories WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}