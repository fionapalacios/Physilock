package com.example.physi_lock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.data.entity.DeepWorkSchedule
import com.example.physi_lock.data.entity.UserConfiguration
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SoftSand

private fun hourLabel(hour: Int): String {
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:00 ${if (hour < 12) "AM" else "PM"}"
}

/**
 * Built from scratch — the teammate's WorkModeScreen.kt is a confirmed 0-byte stub, no
 * design to port. Real schedule-based enforcement as of 2026-08-27 (see ScheduleViewModel /
 * AppMonitorService.activeScheduleBlock/isQuietHours): a Work Hours schedule during which
 * Admin-curated Social Media/Entertainment apps (the same set Focus Mode blocks) are sent
 * to the home screen and Break Reminder/Overuse/Excessive-Usage notifications are held.
 *
 * Redesigned 2026-09-07 against a separately-pasted `WorkModePage` mockup, per the same
 * held-backlog planning session as the Overuse Intervention/Focus-DeepWork-overlay/Wellness
 * Nudges/Delete Account items in MODULE_PROGRESS.md. Fully replaces the old per-day
 * `ScheduleBlockSection` (day-of-week + hour dialog) with: a single Mon-Fri Work Hours
 * window (`UserConfiguration.workHoursStart/EndMinute`, see AppMonitorService.isWithinWorkHours
 * -- matches the mockup's static "Monday - Friday" label, which has no day picker of its
 * own); "Deep Work Blocks", named sub-windows that auto-start/end the app's *existing* real
 * Deep Work Mode session (see AppMonitorService.checkDeepWorkSchedules) rather than being a
 * new blocking concept; and an inline Break Reminder picker reusing the exact same real
 * setting (`breakReminderIntervalPresets`/`setBreakReminderIntervalMinutes`) Settings already
 * has -- the mockup's own 15/30/45/60/90 options already matched exactly, nothing new to add
 * there. The mockup's per-app "Blocked During Work Hours" toggle list is shown read-only
 * instead: Work Mode's blocking is Admin-governed (the same category set Focus Mode blocks),
 * not user-owned like Focus Mode's `FocusBlockedApp`, so a real toggle here would grant User
 * authority Work Mode has never had. The mockup's "Auto-activates based on calendar
 * schedule" toggle is dropped entirely -- no Calendar API integration exists or is in scope;
 * the real mechanism stays "active whenever now falls in the configured window and userMode
 * is WORK_MODE," exactly as before.
 */
@Composable
fun WorkModeScreen(
    config: UserConfiguration,
    isActive: Boolean,
    onSwitchToThisMode: () -> Unit,
    onBackClick: () -> Unit,
    scheduleViewModel: ScheduleViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val deepWorkSchedules by scheduleViewModel.deepWorkSchedules.collectAsState()
    val blockedAppNames by scheduleViewModel.workModeBlockedAppNames.collectAsState()
    val sensitivity = ChallengeSensitivity.fromLabel(config.motionLockSensitivity)
    val dailyLimitMinutes = (config.dailyScreenTimeThresholdMs / 60_000L).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftSand)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = DeepOlive,
                modifier = Modifier.size(20.dp).clickable(onClick = onBackClick)
            )
            Text(
                text = "Work Mode",
                fontFamily = Nunito,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepOlive
            )
        }

        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepOlive, RoundedCornerShape(18.dp))
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(BackgroundLight.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Work, contentDescription = null, tint = BackgroundLight, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        text = "Tuned for the workday",
                        fontFamily = Nunito,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BackgroundLight
                    )
                    Text(
                        text = "Lighter interruptions during focused hours",
                        fontFamily = Nunito,
                        fontSize = 12.sp,
                        color = BackgroundLight.copy(alpha = 0.85f)
                    )
                }
                if (isActive) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = SageAccent)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepOlive, RoundedCornerShape(15.dp))
                        .clickable(onClick = onSwitchToThisMode)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Switch to Work Mode",
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BackgroundLight
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            SettingsCard {
                Text(
                    text = "YOUR CURRENT SETTINGS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SageAccent
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Your daily limit switches to Work Mode's default when you tap in here; the rest below stay shared with Student Mode.",
                    fontFamily = Nunito,
                    fontSize = 12.sp,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(11.dp))
                SettingSummaryRow("Daily screen time limit", "${dailyLimitMinutes / 60}h ${dailyLimitMinutes % 60}m")
                SettingSummaryRow("Motion lock sensitivity", ChallengeSensitivity.displayLabel(config.motionLockSensitivity))
                SettingSummaryRow("Doomscroll detection", if (config.doomscrollingDetectionEnabled) "On" else "Off")
                SettingSummaryRow(
                    "Unlock challenge",
                    "${sensitivity.armRepsRequired} arm reps / ${sensitivity.walkStepsRequired} steps"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            WorkHoursSection(
                startMinute = config.workHoursStartMinute,
                endMinute = config.workHoursEndMinute,
                onSelectStart = { hour -> settingsViewModel.setWorkHoursStartMinute(hour * 60) },
                onSelectEnd = { hour -> settingsViewModel.setWorkHoursEndMinute(hour * 60) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            DeepWorkBlocksSection(
                schedules = deepWorkSchedules,
                onAdd = { start, end ->
                    val nextLetter = ('A' + deepWorkSchedules.size)
                    scheduleViewModel.addDeepWorkSchedule("Deep Work $nextLetter", start, end)
                },
                onToggle = { id, active -> scheduleViewModel.setDeepWorkScheduleActive(id, active) },
                onDelete = { id -> scheduleViewModel.deleteDeepWorkSchedule(id) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            BreakReminderSection(
                enabled = config.breakReminderEnabled,
                currentMinutes = config.breakReminderIntervalMs / 60_000L,
                onSelect = { minutes -> settingsViewModel.setBreakReminderIntervalMinutes(minutes) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            WorkModeBlockedAppsSection(blockedAppNames)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WorkHoursSection(
    startMinute: Int,
    endMinute: Int,
    onSelectStart: (Int) -> Unit,
    onSelectEnd: (Int) -> Unit
) {
    SettingsCard {
        Text(text = "Work Hours", fontFamily = Nunito, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DeepOlive)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Monday – Friday. During this window, distracting apps (Social Media/Entertainment — the same categories Focus Mode blocks) are sent to the home screen, and Break/Overuse notifications are held until after.",
            fontFamily = Nunito,
            fontSize = 12.sp,
            color = DeepOlive
        )
        Spacer(modifier = Modifier.height(11.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            WorkHourField(label = "START", hour = startMinute / 60, onSelect = onSelectStart, modifier = Modifier.weight(1f))
            WorkHourField(label = "END", hour = endMinute / 60, onSelect = onSelectEnd, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WorkHourField(label: String, hour: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .background(DeepOlive.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .clickable { expanded = true }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(text = label, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SageAccent)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = hourLabel(hour), fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepOlive)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (0..23).forEach { h ->
                DropdownMenuItem(text = { Text(hourLabel(h)) }, onClick = { onSelect(h); expanded = false })
            }
        }
    }
}

@Composable
private fun DeepWorkBlocksSection(
    schedules: List<DeepWorkSchedule>,
    onAdd: (startMinute: Int, endMinute: Int) -> Unit,
    onToggle: (id: Long, active: Boolean) -> Unit,
    onDelete: (id: Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    SettingsCard {
        Text(text = "Deep Work Blocks", fontFamily = Nunito, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DeepOlive)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Named windows inside your work day that automatically start a real Deep Work Mode session — all Social Media/Entertainment apps blocked, exit only via the shake gate.",
            fontFamily = Nunito,
            fontSize = 12.sp,
            color = DeepOlive
        )
        Spacer(modifier = Modifier.height(11.dp))

        if (schedules.isEmpty()) {
            Text(
                text = "No blocks yet — add one below.",
                fontFamily = Nunito,
                fontSize = 13.sp,
                color = DeepOlive.copy(alpha = 0.7f)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                schedules.forEach { schedule ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = schedule.label, fontFamily = Nunito, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepOlive)
                            Text(
                                text = "${hourLabel(schedule.startMinute / 60)} – ${hourLabel(schedule.endMinute / 60)}",
                                fontFamily = Nunito,
                                fontSize = 12.sp,
                                color = DeepOlive.copy(alpha = 0.7f)
                            )
                        }
                        Switch(checked = schedule.active, onCheckedChange = { onToggle(schedule.id, it) })
                        IconButton(onClick = { onDelete(schedule.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove block", tint = DeepOlive.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(11.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .clickable { showAddDialog = true }
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+ Add block", fontFamily = Nunito, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepOlive)
        }
    }

    if (showAddDialog) {
        AddDeepWorkBlockDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { start, end ->
                onAdd(start, end)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddDeepWorkBlockDialog(onDismiss: () -> Unit, onConfirm: (startMinute: Int, endMinute: Int) -> Unit) {
    var startHour by remember { mutableStateOf(9) }
    var endHour by remember { mutableStateOf(12) }
    val isValid = endHour > startHour

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Deep Work block") },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Start", fontFamily = Nunito, fontSize = 12.sp, color = DeepOlive.copy(alpha = 0.7f))
                        DeepWorkHourDropdown(selectedHour = startHour, onSelect = { startHour = it })
                    }
                    Column {
                        Text("End", fontFamily = Nunito, fontSize = 12.sp, color = DeepOlive.copy(alpha = 0.7f))
                        DeepWorkHourDropdown(selectedHour = endHour, onSelect = { endHour = it })
                    }
                }
                if (!isValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("End must be after start.", fontFamily = Nunito, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Red)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = isValid, onClick = { onConfirm(startHour * 60, endHour * 60) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeepWorkHourDropdown(selectedHour: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .background(DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = hourLabel(selectedHour), fontFamily = Nunito, fontSize = 13.sp, color = DeepOlive)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (0..23).forEach { hour ->
                DropdownMenuItem(text = { Text(hourLabel(hour)) }, onClick = { onSelect(hour); expanded = false })
            }
        }
    }
}

@Composable
private fun BreakReminderSection(enabled: Boolean, currentMinutes: Long, onSelect: (Int) -> Unit) {
    SettingsCard {
        Text(text = "Break Reminder", fontFamily = Nunito, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DeepOlive)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = if (enabled) "Remind me to take a break every:" else "Break reminders are off — turn them on in Settings.",
            fontFamily = Nunito,
            fontSize = 12.sp,
            color = DeepOlive
        )
        Spacer(modifier = Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            breakReminderIntervalPresets.forEach { minutes ->
                val isSelected = minutes.toLong() == currentMinutes
                Box(
                    modifier = Modifier
                        .background(if (isSelected) DeepOlive else DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .clickable { onSelect(minutes) }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "${minutes}m",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) BackgroundLight else DeepOlive
                    )
                }
            }
        }
    }
}

/** Read-only, deliberately -- see the class kdoc above for why this doesn't mirror the
 *  mockup's per-app toggle list. */
@Composable
private fun WorkModeBlockedAppsSection(appNames: List<String>) {
    SettingsCard {
        Text(text = "Blocked During Work Hours", fontFamily = Nunito, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DeepOlive)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Set by your Admin's app categories (Social Media/Entertainment) — the same set Focus Mode blocks.",
            fontFamily = Nunito,
            fontSize = 12.sp,
            color = DeepOlive
        )
        Spacer(modifier = Modifier.height(11.dp))
        if (appNames.isEmpty()) {
            Text(
                text = "No apps categorized as Social Media/Entertainment yet — ask your Admin to categorize apps.",
                fontFamily = Nunito,
                fontSize = 13.sp,
                color = DeepOlive.copy(alpha = 0.7f)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                appNames.forEach { name ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = DeepOlive.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        Text(text = name, fontFamily = Nunito, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DeepOlive)
                    }
                }
            }
        }
    }
}
