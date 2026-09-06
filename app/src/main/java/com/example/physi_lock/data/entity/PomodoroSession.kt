package com.example.physi_lock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Student Mode Pomodoro (2026-09-07): one row spans a full run from Start to Reset,
// cycling between WORK and BREAK phases automatically -- unlike FocusSession/DeepWorkSession
// (single continuous commitment), this needs to track which phase is current and support a
// real pause. phaseStartTimeMillis is the wall-clock anchor the current phase's remaining
// time is computed from (phaseMinutes*60 - (now - phaseStartTimeMillis)); pausing freezes
// that computation by recording pausedAt, and resuming shifts phaseStartTimeMillis forward
// by the paused duration so the remaining time picks up exactly where it left off -- same
// survives-navigation/process-death principle as FocusSession/DeepWorkSession (a live
// in-memory countdown alone would reset on rotation/process death), just with pause added.
@Entity(tableName = "pomodoro_sessions")
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeMillis: Long,
    val workMinutes: Int,
    val breakMinutes: Int,
    val phase: String, // "WORK" or "BREAK"
    val phaseStartTimeMillis: Long,
    val sessionNumber: Int,
    val pausedAt: Long? = null,
    val endedAt: Long? = null
)
