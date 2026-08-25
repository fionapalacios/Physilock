package com.example.physi_lock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Module 6 (Personalization & User Control): a real Focus Mode session — replaces the
// UI-only locally-ticking timer that reset on navigating away. endTimeMillis is null
// while the session is active; creditMinutesEarned is only set once the session ends.
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val creditMinutesEarned: Int = 0
)
