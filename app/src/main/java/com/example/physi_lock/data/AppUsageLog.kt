package com.example.physi_lock.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_usage_logs",
    indices = [
        Index("packageName"),
        Index("dateKey"),
        Index(value = ["packageName", "dateKey"], unique = false)
    ]
)
data class AppUsageLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val sessionStartTime: Long,
    val sessionEndTime: Long? = null,
    val foregroundDurationMs: Long,
    val scrollEventCount: Int = 0,
    val dateKey: String // e.g., "2026-07-03"
)
