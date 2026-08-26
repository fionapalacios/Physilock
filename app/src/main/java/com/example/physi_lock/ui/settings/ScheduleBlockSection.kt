package com.example.physi_lock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.ScheduleBlock
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent

// java.util.Calendar.DAY_OF_WEEK convention (1=Sunday..7=Saturday) — matches
// AppMonitorService.activeScheduleBlock's runtime check.
private val dayOptions = listOf(1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat")

private fun dayName(dayOfWeek: Int) = dayOptions.firstOrNull { it.first == dayOfWeek }?.second ?: "?"

private fun hourLabel(hour: Int): String {
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:00 ${if (hour < 12) "AM" else "PM"}"
}

/** Student/Work Mode real schedule enforcement UI (see AppMonitorService.
 * activeScheduleBlock for the runtime side). One list of ScheduleBlock rows for the
 * given [mode], plus an "Add block" dialog with a day picker and on-the-hour start/end
 * dropdowns — minute-level editing isn't exposed yet, though the schema supports it. */
@Composable
fun ScheduleBlockSection(
    title: String,
    description: String,
    mode: String,
    blocks: List<ScheduleBlock>,
    onAdd: (dayOfWeek: Int, startMinute: Int, endMinute: Int, label: String) -> Unit,
    onDelete: (id: Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val modeBlocks = blocks.filter { it.mode == mode }
        .sortedWith(compareBy({ it.dayOfWeek }, { it.startMinute }))

    SettingsCard {
        Text(text = title, fontFamily = Nunito, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DeepOlive)
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = description, fontFamily = Nunito, fontSize = 12.sp, color = DeepOlive)
        Spacer(modifier = Modifier.height(11.dp))

        if (modeBlocks.isEmpty()) {
            Text(
                text = "No blocks yet — add one below.",
                fontFamily = Nunito,
                fontSize = 13.sp,
                color = DeepOlive.copy(alpha = 0.7f)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                modeBlocks.forEach { block ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = block.label, fontFamily = Nunito, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepOlive)
                            Text(
                                text = "${dayName(block.dayOfWeek)} · ${hourLabel(block.startMinute / 60)}–${hourLabel(block.endMinute / 60)}",
                                fontFamily = Nunito,
                                fontSize = 12.sp,
                                color = DeepOlive.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = { onDelete(block.id) }) {
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
        AddScheduleBlockDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { day, startMinute, endMinute, label ->
                onAdd(day, startMinute, endMinute, label)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddScheduleBlockDialog(
    onDismiss: () -> Unit,
    onConfirm: (dayOfWeek: Int, startMinute: Int, endMinute: Int, label: String) -> Unit
) {
    var selectedDay by remember { mutableStateOf(2) } // Monday
    var startHour by remember { mutableStateOf(8) }
    var endHour by remember { mutableStateOf(15) }
    val isValid = endHour > startHour

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add schedule block") },
        text = {
            Column {
                Text("Day", fontFamily = Nunito, fontSize = 12.sp, color = DeepOlive.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayOptions.forEach { (value, label) ->
                        val isSelected = value == selectedDay
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) DeepOlive else DeepOlive.copy(alpha = 0.08f))
                                .clickable { selectedDay = value },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label.take(1),
                                fontFamily = Nunito,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BackgroundLight else DeepOlive
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Start", fontFamily = Nunito, fontSize = 12.sp, color = DeepOlive.copy(alpha = 0.7f))
                        HourDropdown(selectedHour = startHour, onSelect = { startHour = it })
                    }
                    Column {
                        Text("End", fontFamily = Nunito, fontSize = 12.sp, color = DeepOlive.copy(alpha = 0.7f))
                        HourDropdown(selectedHour = endHour, onSelect = { endHour = it })
                    }
                }
                if (!isValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("End must be after start.", fontFamily = Nunito, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Red)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onConfirm(
                        selectedDay,
                        startHour * 60,
                        endHour * 60,
                        "${dayName(selectedDay)} ${hourLabel(startHour)}–${hourLabel(endHour)}"
                    )
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun HourDropdown(selectedHour: Int, onSelect: (Int) -> Unit) {
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
                DropdownMenuItem(
                    text = { Text(hourLabel(hour)) },
                    onClick = { onSelect(hour); expanded = false }
                )
            }
        }
    }
}

/** Student Mode's "Study App Allowlist" -- apps that stay reachable while a class
 * schedule block is active (see AppMonitorService's inverted allowlist check). Row
 * visual matches AppLockRulesScreen's picker exactly, bound to a different DAO. */
@Composable
fun AllowlistSection(
    installedApps: List<InstalledAppInfo>,
    allowlistedPackages: Set<String>,
    onToggle: (InstalledAppInfo, Boolean) -> Unit
) {
    SettingsCard {
        Text(
            text = "Study App Allowlist",
            fontFamily = Nunito,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepOlive
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "These apps stay reachable during an active class block. Everything else (except system apps like Phone and Settings) is sent to the home screen.",
            fontFamily = Nunito,
            fontSize = 12.sp,
            color = DeepOlive
        )
        Spacer(modifier = Modifier.height(11.dp))

        if (installedApps.isEmpty()) {
            Text("Loading installed apps...", fontFamily = Nunito, fontSize = 13.sp, color = DeepOlive.copy(alpha = 0.7f))
        } else {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                installedApps.forEach { app ->
                    val allowed = app.packageName in allowlistedPackages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (allowed) SageAccent else DeepOlive.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = app.appName.take(1).uppercase(),
                                fontFamily = Nunito,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (allowed) DeepOlive else DeepOlive.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = app.appName,
                            fontFamily = Nunito,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepOlive,
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                        )
                        Switch(checked = allowed, onCheckedChange = { onToggle(app, it) })
                    }
                }
            }
        }
    }
}
