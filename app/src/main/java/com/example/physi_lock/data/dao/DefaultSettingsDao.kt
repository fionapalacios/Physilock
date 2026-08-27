package com.example.physi_lock.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.example.physi_lock.data.entity.DefaultSettings

@Dao
interface DefaultSettingsDao {
    @Upsert
    suspend fun upsert(settings: DefaultSettings)

    @Query("SELECT * FROM default_settings WHERE id = 1")
    fun getDefaults(): Flow<DefaultSettings?>
}