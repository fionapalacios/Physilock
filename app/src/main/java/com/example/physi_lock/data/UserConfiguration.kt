package com.example.physi_lock.data

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
    // Module 7 (Context-Aware AI): a Wi-Fi-network-name-matched "Context Alert" (not GPS
    // geofencing -- the manuscript itself lists real location-based locking as a Future
    // Enhancement, out of MVP scope). contextAlertWifiSsid null means no network configured
    // yet even if the toggle is on.
    val contextAlertsEnabled: Boolean = false,
    val contextAlertWifiSsid: String? = null,
    // Focus Mode's earned-but-previously-unspendable "screen credit" (see FocusSession):
    // 1 credit minute per 5 minutes focused, redeemable 1:1 for temporary-unlock minutes on
    // a locked app from the Move hub -- the same real per-app timed-unlock mechanism
    // Activity Challenges already use (AppMonitorService.grantTemporaryUnlock).
    val focusCreditBalanceMinutes: Int = 0,
    val lastUpdatedTime: Long = System.currentTimeMillis()
)
