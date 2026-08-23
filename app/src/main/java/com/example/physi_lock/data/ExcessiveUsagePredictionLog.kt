package com.example.physi_lock.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Mirrors the manuscript's Data Dictionary EXCESSIVE_USAGE_PREDICTION table —
// Logistic Regression II's output log. One row per hour (see AppMonitorService's
// checkExcessiveUsagePrediction). See ml/README.md for the model itself.
@Entity(
    tableName = "excessive_usage_predictions",
    indices = [Index("dateKey"), Index(value = ["dateKey", "hour"])]
)
data class ExcessiveUsagePredictionLog(
    @PrimaryKey(autoGenerate = true) val predictionId: Int = 0,
    val dateKey: String,
    val hour: Int,
    val predictedUsageMinutes: Double,
    val excessiveProbability: Double,
    val isExcessive: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
