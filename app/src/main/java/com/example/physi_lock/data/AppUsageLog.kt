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
    // Longest gap between two consecutive scroll events in this session, in ms.
    // A proxy for "pause patterns" (Module 2's doomscroll classifier input) — a
    // low max gap means near-continuous scrolling with no real pause, typical of
    // doomscrolling; a high max gap means at least one substantial reading pause.
    val maxScrollGapMs: Long = 0,
    val dateKey: String // e.g., "2026-07-03"
)
