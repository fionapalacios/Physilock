package com.example.physi_lock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Student Mode's "Study App Allowlist" -- apps that stay reachable during an active
 * ScheduleBlock, when every other (non-system) app is sent to the home screen. Mirrors
 * AppCategory's shape (denormalized appName avoids re-resolving PackageManager labels). */
@Entity(tableName = "allowlisted_apps")
data class AllowlistedApp(
    @PrimaryKey val packageName: String,
    val appName: String
)
