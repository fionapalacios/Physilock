package com.example.physi_lock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Admin-configured global defaults applied to new user accounts. Mirrors
// UserConfiguration's shape; individual users can still override in their own Settings.
@Entity(tableName = "default_settings")
data class DefaultSettings(
    @PrimaryKey val id: Int = 1, // Single active default configuration
    val userMode: String = "STUDENT_MODE",
    val dailyScreenTimeThresholdMs: Long = 480 * 60 * 1000,
    val hourlyExcessiveUsageThresholdMs: Long = 60 * 60 * 1000,
    val doomscrollingDetectionEnabled: Boolean = true,
    val doomscrollingSensitivity: String = "MODERATE",
    val motionLockSensitivity: String = "MODERATE",
    val lastUpdatedTime: Long = System.currentTimeMillis()
)