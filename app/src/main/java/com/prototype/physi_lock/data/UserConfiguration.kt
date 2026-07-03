package com.prototype.physi_lock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_configuration")
data class UserConfiguration(
    @PrimaryKey val configId: Int = 1, // Single active configuration
    val userMode: String = "STUDENT_MODE", // "STUDENT_MODE" or "WORK_MODE"
    val dailyScreenTimeThresholdMs: Long = 480 * 60 * 1000, // 480 minutes default
    val hourlyExcessiveUsageThresholdMs: Long = 60 * 60 * 1000, // 60 minutes per hour
    val doomscrollingDetectionEnabled: Boolean = true,
    val motionLockSensitivity: String = "MEDIUM", // "LOW", "MEDIUM", "HIGH"
    val lastUpdatedTime: Long = System.currentTimeMillis()
) {
    // Placeholder until the AI risk-scoring pipeline is wired in; mirrors motion
    // sensitivity so the dashboard shows a sensible value meanwhile.
    val riskSensitivity: String
        get() = when (motionLockSensitivity) {
            "LOW" -> "Low"
            "HIGH" -> "High"
            else -> "Moderate"
        }
}
