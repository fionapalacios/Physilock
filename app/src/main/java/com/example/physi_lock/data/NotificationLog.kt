package com.example.physi_lock.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// In-app notification history backing NotificationsOverlay (ui/components/NotificationsPanel.kt).
// One row per real system notification actually posted by AppMonitorService — inserted
// alongside each postXNotification() call, not a separate/independent log.
@Entity(
    tableName = "notification_logs",
    indices = [Index("timestamp")]
)
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "BREAK_REMINDER", "OVERUSE_ALERT", "DOOMSCROLL_ALERT", "EXCESSIVE_USAGE_PREDICTION"
    val title: String,
    val description: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
