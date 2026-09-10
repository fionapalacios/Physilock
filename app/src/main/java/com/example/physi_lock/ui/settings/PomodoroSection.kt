package com.example.physi_lock.ui.settings

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.entity.PomodoroSession
import com.example.physi_lock.ui.components.AppIconAvatar
import com.example.physi_lock.ui.components.CircularProgressRing
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.DmMono
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage

private val studyMinutePresets = listOf(15, 20, 25, 30, 45, 50)
private val breakMinutePresets = listOf(5, 10, 15, 20)

private fun formatCountdown(secs: Long): String {
    val clamped = secs.coerceAtLeast(0)
    return "%02d:%02d".format(clamped / 60, clamped % 60)
}

private fun phaseLabel(session: PomodoroSession?): String = when {
    session == null -> "Ready to study"
    session.phase == "BREAK" -> "Break time!"
    else -> "Study session"
}

private fun phaseColor(session: PomodoroSession?): Color = if (session?.phase == "BREAK") Orchid else SageAccent

/** Student Mode's real Pomodoro timer card (2026-09-07) -- replaces the old Class
 *  Schedule's day/hour dialog. Backed by [PomodoroViewModel]; work/break minutes are only
 *  actually committed when a new session starts (see [PomodoroViewModel.startSessionIfNeeded]),
 *  matching the comparison mockup's own behavior where changing presets mid-session doesn't
 *  retroactively resize the running phase. */
@Composable
fun PomodoroTimerCard(
    session: PomodoroSession?,
    remainingSeconds: Long,
    pendingWorkMinutes: Int,
    onStart: () -> Unit,
    onPauseResume: () -> Unit,
    onReset: () -> Unit,
    onExpand: () -> Unit
) {
    val running = session != null && session.pausedAt == null
    val targetSecs = if (session != null) {
        (if (session.phase == "WORK") session.workMinutes else session.breakMinutes) * 60L
    } else {
        pendingWorkMinutes * 60L
    }
    val progress = if (targetSecs > 0) (1f - remainingSeconds.toFloat() / targetSecs).coerceIn(0f, 1f) else 0f
    val tint = phaseColor(session)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepOlive, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "POMODORO TIMER · SESSION ${session?.sessionNumber ?: 1}",
                    fontFamily = DmMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SageAccent,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Expand",
                    tint = SageAccent,
                    modifier = Modifier.size(18.dp).clickable(onClick = onExpand)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            AnimatedContent(targetState = phaseLabel(session), label = "pomodoroPhase") { label ->
                Text(text = label, fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tint)
            }
            Spacer(modifier = Modifier.height(16.dp))

            CircularProgressRing(
                progress = progress,
                trackColor = BackgroundLight.copy(alpha = 0.1f),
                progressColor = tint,
                ringSize = 136.dp,
                strokeWidth = 8.dp,
                outlineColor = DeepOlive
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatCountdown(remainingSeconds),
                        fontFamily = DmMono,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BackgroundLight
                    )
                    Text(
                        text = if (session?.phase == "BREAK") "break" else "focus",
                        fontFamily = DmMono,
                        fontSize = 11.sp,
                        color = SageAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .background(SageAccent, RoundedCornerShape(14.dp))
                        .clickable(onClick = if (session == null) onStart else onPauseResume)
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = BackgroundLight,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = when {
                                session == null -> "Start"
                                running -> "Pause"
                                else -> "Resume"
                            },
                            fontFamily = Nunito,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BackgroundLight
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(BackgroundLight.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onReset)
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = SecondarySage, modifier = Modifier.size(15.dp))
                        Text(text = "Reset", fontFamily = Nunito, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SecondarySage)
                    }
                }
            }
        }
    }
}

@Composable
fun PomodoroSessionSettingsCard(
    workMinutes: Int,
    breakMinutes: Int,
    onSelectWork: (Int) -> Unit,
    onSelectBreak: (Int) -> Unit
) {
    SettingsCard {
        Text(text = "Session Settings", fontFamily = Nunito, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DeepOlive)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Applies to your next session — changing these mid-session doesn't resize the phase already running.",
            fontFamily = Nunito,
            fontSize = 12.sp,
            color = DeepOlive
        )
        Spacer(modifier = Modifier.height(11.dp))
        PresetRow(label = "STUDY (MIN)", options = studyMinutePresets, selected = workMinutes, onSelect = onSelectWork)
        Spacer(modifier = Modifier.height(11.dp))
        PresetRow(label = "BREAK (MIN)", options = breakMinutePresets, selected = breakMinutes, onSelect = onSelectBreak)
    }
}

@Composable
private fun PresetRow(label: String, options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Text(text = label, fontFamily = DmMono, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SageAccent)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { minutes ->
                val isSelected = minutes == selected
                Box(
                    modifier = Modifier
                        .background(if (isSelected) DeepOlive else DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .clickable { onSelect(minutes) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${minutes}m",
                        fontFamily = DmMono,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) BackgroundLight else DeepOlive
                    )
                }
            }
        }
    }
}

/** Student Mode Pomodoro's blocklist (2026-09-07) -- a genuinely different model from the
 *  old Class Schedule's allowlist-inversion: specific apps blocked during the WORK phase,
 *  everything else stays reachable. Same app-picker row visual (AppIconAvatar + name +
 *  Switch) as WhitelistManagerScreen/AppLockRulesScreen, inverted polarity here (a checked
 *  switch means blocked, not allowed). */
@Composable
fun PomodoroBlockedAppsSection(
    installedApps: List<InstalledAppInfo>,
    blockedPackages: Set<String>,
    onToggle: (InstalledAppInfo, Boolean) -> Unit
) {
    SettingsCard {
        Text(text = "Blocked During Study", fontFamily = Nunito, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DeepOlive)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "These apps are blocked while a study session's WORK phase is running. Everything else stays reachable, and blocking pauses during your break.",
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
                    val blocked = app.packageName in blockedPackages
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconAvatar(
                            packageName = app.packageName,
                            appName = app.appName,
                            size = 34.dp,
                            fontSize = 12.sp,
                            backgroundColor = if (blocked) DeepOlive else DeepOlive.copy(alpha = 0.12f),
                            contentColor = if (blocked) BackgroundLight else DeepOlive.copy(alpha = 0.6f)
                        )
                        Text(
                            text = app.appName,
                            fontFamily = Nunito,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepOlive,
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                        )
                        Switch(checked = blocked, onCheckedChange = { onToggle(app, it) })
                    }
                }
            }
        }
    }
}

/** Fullscreen expand view (2026-09-07) -- modeled on DeepWorkScreen's fullscreen pulsing-ring
 *  pattern, just without the pulse (Pomodoro isn't a locked/forced screen the way Deep Work
 *  is -- tapping outside/back is fine, this is a bigger view of the same real timer). */
@Composable
fun PomodoroFullscreenOverlay(
    session: PomodoroSession?,
    remainingSeconds: Long,
    pendingWorkMinutes: Int,
    onPauseResume: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    val running = session != null && session.pausedAt == null
    val targetSecs = if (session != null) {
        (if (session.phase == "WORK") session.workMinutes else session.breakMinutes) * 60L
    } else {
        pendingWorkMinutes * 60L
    }
    val progress = if (targetSecs > 0) (1f - remainingSeconds.toFloat() / targetSecs).coerceIn(0f, 1f) else 0f
    val tint = phaseColor(session)

    Box(modifier = Modifier.fillMaxSize().background(DeepOlive)) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = BackgroundLight,
            modifier = Modifier
                .padding(20.dp)
                .size(22.dp)
                .background(BackgroundLight.copy(alpha = 0.1f), CircleShape)
                .padding(4.dp)
                .clickable(onClick = onClose)
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "POMODORO TIMER · SESSION ${session?.sessionNumber ?: 1}",
                fontFamily = DmMono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SageAccent
            )
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedContent(targetState = phaseLabel(session), label = "pomodoroPhaseFullscreen") { label ->
                Text(text = label, fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tint)
            }
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressRing(
                progress = progress,
                trackColor = BackgroundLight.copy(alpha = 0.1f),
                progressColor = tint,
                ringSize = 220.dp,
                strokeWidth = 10.dp,
                outlineColor = DeepOlive
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatCountdown(remainingSeconds),
                        fontFamily = DmMono,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BackgroundLight
                    )
                    Text(
                        text = if (session?.phase == "BREAK") "break" else "focus",
                        fontFamily = DmMono,
                        fontSize = 13.sp,
                        color = SageAccent
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .background(SageAccent, RoundedCornerShape(16.dp))
                        .clickable(enabled = session != null, onClick = onPauseResume)
                        .padding(horizontal = 24.dp, vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = BackgroundLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (running) "Pause" else "Resume",
                            fontFamily = Nunito,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BackgroundLight
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(BackgroundLight.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .clickable(enabled = session != null, onClick = onReset)
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = SecondarySage, modifier = Modifier.size(16.dp))
                        Text(text = "Reset", fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SecondarySage)
                    }
                }
            }
        }
    }
}
