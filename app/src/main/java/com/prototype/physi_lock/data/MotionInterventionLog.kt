package com.prototype.physi_lock.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "motion_intervention_logs",
    indices = [
        Index("packageName"),
        Index("interventionTimestamp")
    ]
)
data class MotionInterventionLog(
    @PrimaryKey(autoGenerate = true) val interventionId: Int = 0,
    val packageName: String,
    val triggerType: String, // "MOTION_LOCK", "DOOMSCROLL_ALERT", "HOURLY_PREDICTION"
    val interventionTimestamp: Long,
    val userResponse: String? = null, // "UNLOCKED", "DISMISSED", "TIMEOUT"
    val accelerometerVariance: Float = 0f, // Motion intensity metric
    val riskScore: Double = 0.0 // AI-computed behavioral risk (0.0-1.0)
)
