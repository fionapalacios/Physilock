package com.example.physi_lock.data.dao

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.physi_lock.data.entity.AppLockRule

@Dao
interface AppLockRuleDao {
    @Upsert
    suspend fun upsert(rule: AppLockRule)

    @Query("SELECT * FROM app_lock_rules WHERE packageName = :packageName")
    fun getRule(packageName: String): Flow<AppLockRule?>

    @Query("SELECT * FROM app_lock_rules WHERE packageName = :packageName")
    suspend fun getRuleOnce(packageName: String): AppLockRule?

    @Query("SELECT * FROM app_lock_rules")
    fun getAllRules(): Flow<List<AppLockRule>>
}