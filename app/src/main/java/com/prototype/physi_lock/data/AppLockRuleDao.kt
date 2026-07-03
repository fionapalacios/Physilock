package com.prototype.physi_lock.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLockRuleDao {
    @Upsert
    suspend fun upsert(rule: AppLockRule)

    @Query("SELECT * FROM app_lock_rules WHERE packageName = :packageName")
    fun getRule(packageName: String): Flow<AppLockRule?>

    @Query("SELECT * FROM app_lock_rules")
    fun getAllRules(): Flow<List<AppLockRule>>

    @Query("SELECT packageName FROM app_lock_rules WHERE isLocked = 1")
    fun getLockedPackageNames(): Flow<List<String>>
}
