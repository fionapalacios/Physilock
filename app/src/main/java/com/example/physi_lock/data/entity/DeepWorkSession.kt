package com.example.physi_lock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Deep Work Mode (2026-09-04): a stricter tier than Focus Mode, own separate entry point
// (Home, between Focus Mode and Lock Apps). Unlike FocusSession (indefinite, user-picked
// blocklist, ends anytime), this is a fixed-duration commitment against ALL Admin-curated
// Social Media/Entertainment apps (see AppMonitorService.deepWorkBlockedPackages), and the
// only way out before durationSecs elapses is a real 5x-shake gate (see
// RotationalArmDetector). endedEarly is recorded for honesty/analytics only -- per
// instruction, ending early still earns credit at the same rate as completing normally, no
// forfeiture penalty.
@Entity(tableName = "deep_work_sessions")
data class DeepWorkSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeMillis: Long,
    val durationSecs: Int,
    val endTimeMillis: Long? = null,
    val endedEarly: Boolean = false,
    val creditMinutesEarned: Int = 0
)
