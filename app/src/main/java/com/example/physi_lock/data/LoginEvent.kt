package com.example.physi_lock.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// One row per account per calendar day it was seen signed in (explicit login, registration,
// or an already-signed-in session resuming on app launch) -- see AuthViewModel. The unique
// index on (accountId, dateKey) plus @Insert(onConflict = IGNORE) in LoginEventDao makes
// recording this idempotent, so every entry point can call it without first checking whether
// today's row already exists. Backs Admin Analytics' real "Daily Active Users" chart.
@Entity(
    tableName = "login_events",
    indices = [Index(value = ["accountId", "dateKey"], unique = true)]
)
data class LoginEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: String,
    val dateKey: String, // e.g. 2026-08-29
    val timestamp: Long
)
