package com.example.physi_lock.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryGoalDao {
    @Upsert
    suspend fun upsert(goal: CategoryGoal)

    @Query("SELECT * FROM category_goals")
    fun getAll(): Flow<List<CategoryGoal>>
}
