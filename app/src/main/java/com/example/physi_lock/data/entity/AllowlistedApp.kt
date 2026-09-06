package com.example.physi_lock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Apps that stay reachable during Bedtime Mode's window, when every other (non-system)
 * app is sent to the home screen -- see AppMonitorService.isWithinBedtimeWindow. Also
 * editable directly via the standalone Whitelist Manager screen. Originally built as
 * Student Mode's "Study App Allowlist" (a ScheduleBlock-era concept); Student Mode has
 * used a real Pomodoro blocklist (PomodoroBlockedApp) instead since 2026-09-07, so this
 * table is Bedtime-only now. Mirrors AppCategory's shape (denormalized appName avoids
 * re-resolving PackageManager labels). */
@Entity(tableName = "allowlisted_apps")
data class AllowlistedApp(
    @PrimaryKey val packageName: String,
    val appName: String
)
