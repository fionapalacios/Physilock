package com.example.physi_lock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Work Mode redesign (2026-09-07): the mockup's "Deep Work Blocks" -- named time windows
// (e.g. "Deep Work A" 09:00-12:00) that auto-start/end the app's existing real Deep Work
// Mode session (DeepWorkSession) rather than being a new blocking concept of their own. No
// day-of-week field, matching the mockup's own simpler {label, start, end, active} shape --
// these are scoped inside Work Mode's Mon-Fri window (see AppMonitorService.isWithinWorkHours)
// so a day field would be redundant. See AppMonitorService.checkDeepWorkSchedules for the
// real auto-start/stop logic.
@Entity(tableName = "deep_work_schedules")
data class DeepWorkSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val startMinute: Int,
    val endMinute: Int,
    val active: Boolean = true
)
