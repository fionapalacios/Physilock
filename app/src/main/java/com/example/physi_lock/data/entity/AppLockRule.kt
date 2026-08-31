package com.example.physi_lock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_lock_rules")
data class AppLockRule(
    @PrimaryKey val packageName: String,
    val isLocked: Boolean,
    val lockType: String,               // ADAPTIVE, CUSTOM, or GOALS
    val dailyLimitMs: Long? = null
)