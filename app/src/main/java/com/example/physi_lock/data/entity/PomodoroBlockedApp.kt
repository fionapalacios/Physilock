package com.example.physi_lock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Apps the User has chosen to block during an active Pomodoro study session's WORK phase
 * (see AppMonitorService's pomodoro block check). Mirrors [FocusBlockedApp]'s shape and
 * User-owned-selection reasoning exactly -- this is a genuinely different model from Student
 * Mode's old "Class Schedule" (which blocked everything except a Study App Allowlist); the
 * comparison mockup's own "Blocked During Study" list is a blocklist, not an allowlist. */
@Entity(tableName = "pomodoro_blocked_apps")
data class PomodoroBlockedApp(
    @PrimaryKey val packageName: String,
    val appName: String
)
