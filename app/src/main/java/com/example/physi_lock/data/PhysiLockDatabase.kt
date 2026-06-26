package com.example.physi_lock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UsageSession::class, AppLockRule::class],
    version = 1,
    exportSchema = false
)
abstract class PhysiLockDatabase : RoomDatabase() {
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun appLockRuleDao(): AppLockRuleDao

    companion object {
        @Volatile private var INSTANCE: PhysiLockDatabase? = null

        fun getInstance(context: Context): PhysiLockDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PhysiLockDatabase::class.java,
                    "physilock_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}