package com.example.physi_lock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UsageSession::class,
        AppLockRule::class,
        AppUsageLog::class,
        UserConfiguration::class,
        MotionInterventionLog::class,
        AppCategory::class,
        DefaultSettings::class,
        CachedAccount::class
    ],
    version = 8,
    exportSchema = false
)
abstract class PhysiLockDatabase : RoomDatabase() {
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun appLockRuleDao(): AppLockRuleDao
    abstract fun appUsageLogDao(): AppUsageLogDao
    abstract fun userConfigurationDao(): UserConfigurationDao
    abstract fun motionInterventionLogDao(): MotionInterventionLogDao
    abstract fun appCategoryDao(): AppCategoryDao
    abstract fun defaultSettingsDao(): DefaultSettingsDao
    abstract fun cachedAccountDao(): CachedAccountDao

    companion object {
        @Volatile private var INSTANCE: PhysiLockDatabase? = null

        fun getInstance(context: Context): PhysiLockDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PhysiLockDatabase::class.java,
                    "physilock_db"
                )
                    .fallbackToDestructiveMigration() // Only for dev/test
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}