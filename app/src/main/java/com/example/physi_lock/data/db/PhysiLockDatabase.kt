package com.example.physi_lock.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.physi_lock.data.dao.AllowlistedAppDao
import com.example.physi_lock.data.dao.AppCategoryDao
import com.example.physi_lock.data.dao.AppLockRuleDao
import com.example.physi_lock.data.dao.AppUsageLogDao
import com.example.physi_lock.data.dao.CachedAccountDao
import com.example.physi_lock.data.dao.CategoryGoalDao
import com.example.physi_lock.data.dao.DeepWorkScheduleDao
import com.example.physi_lock.data.dao.DeepWorkSessionDao
import com.example.physi_lock.data.dao.DefaultSettingsDao
import com.example.physi_lock.data.dao.ExcessiveUsagePredictionLogDao
import com.example.physi_lock.data.dao.FocusBlockedAppDao
import com.example.physi_lock.data.dao.FocusSessionDao
import com.example.physi_lock.data.dao.LoginEventDao
import com.example.physi_lock.data.dao.MotionInterventionLogDao
import com.example.physi_lock.data.dao.NotificationLogDao
import com.example.physi_lock.data.dao.PomodoroBlockedAppDao
import com.example.physi_lock.data.dao.PomodoroSessionDao
import com.example.physi_lock.data.dao.ReflectionEntryDao
import com.example.physi_lock.data.dao.UsageSessionDao
import com.example.physi_lock.data.dao.UserConfigurationDao
import com.example.physi_lock.data.entity.AllowlistedApp
import com.example.physi_lock.data.entity.AppCategory
import com.example.physi_lock.data.entity.AppLockRule
import com.example.physi_lock.data.entity.AppUsageLog
import com.example.physi_lock.data.entity.CachedAccount
import com.example.physi_lock.data.entity.CategoryGoal
import com.example.physi_lock.data.entity.DeepWorkSchedule
import com.example.physi_lock.data.entity.DeepWorkSession
import com.example.physi_lock.data.entity.DefaultSettings
import com.example.physi_lock.data.entity.ExcessiveUsagePredictionLog
import com.example.physi_lock.data.entity.FocusBlockedApp
import com.example.physi_lock.data.entity.FocusSession
import com.example.physi_lock.data.entity.LoginEvent
import com.example.physi_lock.data.entity.MotionInterventionLog
import com.example.physi_lock.data.entity.NotificationLog
import com.example.physi_lock.data.entity.PomodoroBlockedApp
import com.example.physi_lock.data.entity.PomodoroSession
import com.example.physi_lock.data.entity.ReflectionEntry
import com.example.physi_lock.data.entity.UsageSession
import com.example.physi_lock.data.entity.UserConfiguration

@Database(
    entities = [
        UsageSession::class,
        AppLockRule::class,
        AppUsageLog::class,
        UserConfiguration::class,
        MotionInterventionLog::class,
        AppCategory::class,
        DefaultSettings::class,
        CachedAccount::class,
        ExcessiveUsagePredictionLog::class,
        NotificationLog::class,
        FocusSession::class,
        CategoryGoal::class,
        ReflectionEntry::class,
        AllowlistedApp::class,
        FocusBlockedApp::class,
        LoginEvent::class,
        DeepWorkSession::class,
        DeepWorkSchedule::class,
        // Student Mode Pomodoro (2026-09-07) replaces ScheduleBlock for both Student and
        // Work Mode -- that entity/DAO are removed here now that nothing references them.
        PomodoroSession::class,
        PomodoroBlockedApp::class
    ],
    version = 31, // 31: LoginEvent.accountId -> CachedAccount.id foreign key (CASCADE)
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
    abstract fun excessiveUsagePredictionLogDao(): ExcessiveUsagePredictionLogDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun categoryGoalDao(): CategoryGoalDao
    abstract fun reflectionEntryDao(): ReflectionEntryDao
    abstract fun allowlistedAppDao(): AllowlistedAppDao
    abstract fun focusBlockedAppDao(): FocusBlockedAppDao
    abstract fun loginEventDao(): LoginEventDao
    abstract fun deepWorkSessionDao(): DeepWorkSessionDao
    abstract fun deepWorkScheduleDao(): DeepWorkScheduleDao
    abstract fun pomodoroSessionDao(): PomodoroSessionDao
    abstract fun pomodoroBlockedAppDao(): PomodoroBlockedAppDao

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
