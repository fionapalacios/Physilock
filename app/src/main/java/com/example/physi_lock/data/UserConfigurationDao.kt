package com.example.physi_lock.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserConfigurationDao {
    @Upsert
    suspend fun upsert(config: UserConfiguration)

    @Query("SELECT * FROM user_configuration WHERE configId = 1")
    fun getActiveConfiguration(): Flow<UserConfiguration?>

    @Query("SELECT * FROM user_configuration WHERE configId = 1 LIMIT 1")
    suspend fun getActiveConfigurationOnce(): UserConfiguration?
}
