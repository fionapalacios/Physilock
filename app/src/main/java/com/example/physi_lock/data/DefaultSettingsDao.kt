package com.example.physi_lock.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DefaultSettingsDao {
    @Upsert
    suspend fun upsert(settings: DefaultSettings)

    @Query("SELECT * FROM default_settings WHERE id = 1")
    fun getDefaults(): Flow<DefaultSettings?>
}