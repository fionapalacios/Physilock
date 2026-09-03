package com.example.physi_lock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_configuration")
data class UserConfiguration(
    @PrimaryKey val configId: Int = 1, // Single active configuration
    val userMode: String = "STUDENT_MODE", // "STUDENT_MODE" or "WORK_MODE"
    val dailyScreenTimeThresholdMs: Long = 480 * 60 * 1000, // 480 minutes default
    val hourlyExcessiveUsageThresholdMs: Long = 60 * 60 * 1000, // 60 minutes per hour
    val doomscrollingDetectionEnabled: Boolean = true,
    // Doomscroll Sensitivity (2026-08-27): an Admin-set bias layered on top of
    // DoomscrollDetector's existing risk-level threshold "recipe", not a replacement for
    // it -- MODERATE leaves that recipe exactly as it was before this field existed. See
    // project memory "module2-doomscroll-design" for the confirmed design/rejected
    // alternatives. Same Admin-controlled, User-read-only treatment as motionLockSensitivity.
    val doomscrollingSensitivity: String = "MODERATE", // "LOW", "MODERATE", "HIGH"
    val motionLockSensitivity: String = "MODERATE", // "LOW", "MODERATE", "HIGH"
    val breakReminderEnabled: Boolean = true,
    val breakReminderIntervalMs: Long = 30 * 60 * 1000, // 30 minutes default
    val overuseAlertsEnabled: Boolean = true,
    val weeklyScreenTimeGoalMs: Long = 35 * 3_600_000L, // 35 hours default (Usage Goals screen)
    // Module 7 (Context-Aware AI): a "Context Alert" -- a passive notification when a
    // distracting app opens somewhere the user flagged, either by Wi-Fi network name or
    // (2026-09-04, user's own explicit ask, going beyond the manuscript's stated MVP scope
    // which lists real location-based locking as a Future Enhancement) by real GPS
    // proximity via a Leaflet.js map picker -- see ContextAlertsScreen.kt /
    // AppMonitorService.isNearWatchedLocation. Either/both trigger signals can be set;
    // contextAlertWifiSsid/contextAlertLatitude+Longitude null means that one isn't
    // configured, independent of whether the other is.
    val contextAlertsEnabled: Boolean = false,
    val contextAlertWifiSsid: String? = null,
    val contextAlertLatitude: Double? = null,
    val contextAlertLongitude: Double? = null,
    val contextAlertRadiusMeters: Int = 100,
    // Bedtime Mode (2026-09-04): a daily recurring window, minutes-since-midnight, during
    // which every app not on the (shared, see AllowlistedApp/Whitelist Manager) allowlist
    // gets sent to the home screen -- same allowlist-inverted enforcement shape as Student
    // Mode's Class Mode, just time-window-only rather than day-of-week + userMode scoped.
    // Defaults (11 PM-7 PM) wrap past midnight -- see AppMonitorService.isWithinBedtimeWindow.
    // No separate enabled flag: like Class/Work Mode, active purely by being inside the
    // window, nothing else to toggle.
    val bedtimeStartMinute: Int = 23 * 60,
    val bedtimeEndMinute: Int = 7 * 60,
    // Focus Mode's earned-but-previously-unspendable "screen credit" (see FocusSession):
    // 1 credit minute per 5 minutes focused, redeemable 1:1 for temporary-unlock minutes on
    // a locked app from the Move hub -- the same real per-app timed-unlock mechanism
    // Activity Challenges already use (AppMonitorService.grantTemporaryUnlock).
    val focusCreditBalanceMinutes: Int = 0,
    val lastUpdatedTime: Long = System.currentTimeMillis()
)
