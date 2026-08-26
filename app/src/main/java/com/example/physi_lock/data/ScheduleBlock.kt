package com.example.physi_lock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Student/Work Mode real schedule-based enforcement (see AppMonitorService.activeScheduleBlock):
 * a recurring weekly time window during which the active mode's enforcement kicks in — Student
 * Mode's allowlist-only blocking, or Work Mode's distracting-app blocking + quiet-hours
 * notification hold. dayOfWeek uses java.util.Calendar.DAY_OF_WEEK convention (1=Sunday..7=
 * Saturday) to match AppMonitorService's existing Calendar-based time checks elsewhere in that
 * file. start/endMinute are minutes-since-midnight (0-1439) -- stored at minute granularity even
 * though the current picker UI only offers on-the-hour presets, so a finer-grained editor can be
 * added later without another schema migration. */
@Entity(tableName = "schedule_blocks")
data class ScheduleBlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String, // "STUDENT_MODE" or "WORK_MODE"
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val label: String
)
