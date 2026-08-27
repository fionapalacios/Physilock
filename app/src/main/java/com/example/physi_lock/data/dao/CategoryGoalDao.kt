package com.example.physi_lock.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.example.physi_lock.data.entity.CategoryGoal

@Dao
interface CategoryGoalDao {
    @Upsert
    suspend fun upsert(goal: CategoryGoal)

    @Query("SELECT * FROM category_goals")
    fun getAll(): Flow<List<CategoryGoal>>
}
