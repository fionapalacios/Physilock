package com.example.physi_lock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Apps the User has chosen to block during an active Focus Mode session. Deliberately a
 * User-owned selection, not derived from Admin's app categories -- Admin's job is curating
 * categories (for tracking/reporting), not deciding what gets blocked. Mirrors AllowlistedApp's
 * shape (denormalized appName avoids re-resolving PackageManager labels). */
@Entity(tableName = "focus_blocked_apps")
data class FocusBlockedApp(
    @PrimaryKey val packageName: String,
    val appName: String
)
