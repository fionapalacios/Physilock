package com.example.physi_lock.ui.move

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.sensor.ChallengeType
import com.example.physi_lock.sensor.isActivityRecognitionGranted
import com.example.physi_lock.ui.challenge.ChallengeActiveScreen
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SoftSand

/**
 * Visual design ported from the teammate's sprint-2-ui-navigation branch
 * (MoveScreen.kt). Their version has no permission handling, no locked-app
 * gating, and a hardcoded streak/XP display — this keeps the real MoveViewModel
 * data (totalXp, streakDays, lockedApps, completedToday) and the real
 * ACTIVITY_RECOGNITION permission + app-picker flow underneath the new visuals.
 */
private val MoveStreakOrange = androidx.compose.ui.graphics.Color(0xFFE8854A)

private data class ChallengeCardSpec(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: androidx.compose.ui.graphics.Color
)

private fun cardSpecFor(type: ChallengeType): ChallengeCardSpec = when (type) {
    ChallengeType.RUN_JOG -> ChallengeCardSpec(
        title = "Run / Jog",
        description = "Continuous jogging detected via accelerometer",
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        accentColor = com.example.physi_lock.ui.theme.Orchid
    )
    ChallengeType.WALK -> ChallengeCardSpec(
        title = "5-Minute Walk",
        description = "Walk at a steady pace",
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        accentColor = SageAccent
    )
    ChallengeType.ROTATIONAL_ARM -> ChallengeCardSpec(
        title = "Rotational Arm Movements",
        description = "Rotate your arm through the full motion",
        icon = Icons.Default.Bolt,
        accentColor = DeepOlive
    )
}

private fun targetLabel(type: ChallengeType, sensitivity: ChallengeSensitivity): String = when (type) {
    ChallengeType.RUN_JOG -> "${sensitivity.jogDurationMs / 60_000}m"
    ChallengeType.WALK -> "${sensitivity.walkStepsRequired} steps"
    ChallengeType.ROTATIONAL_ARM -> "${sensitivity.armRepsRequired} reps"
}

@Composable
fun MoveScreen(moveViewModel: MoveViewModel = viewModel()) {
    val context = LocalContext.current
    val streakDays by moveViewModel.streakDays.collectAsState()
    val totalXp by moveViewModel.totalXp.collectAsState()
    val lockedApps by moveViewModel.lockedApps.collectAsState()
    val completedToday by moveViewModel.completedToday.collectAsState()

    var inProgressChallenge by remember { mutableStateOf<ChallengeType?>(null) }
    var pickedApp by remember { mutableStateOf<LockedAppInfo?>(null) }
    var appPickerFor by remember { mutableStateOf<ChallengeType?>(null) }
    var pendingPermissionType by remember { mutableStateOf<ChallengeType?>(null) }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pendingType = pendingPermissionType
        pendingPermissionType = null
        if (granted && pendingType != null) {
            permissionDeniedMessage = null
            appPickerFor = pendingType
        } else {
            permissionDeniedMessage = "Activity Recognition permission is required for this challenge."
        }
    }

    fun requestChallenge(type: ChallengeType) {
        val needsStepSensor = type == ChallengeType.WALK || type == ChallengeType.RUN_JOG
        if (needsStepSensor && !isActivityRecognitionGranted(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                pendingPermissionType = type
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                appPickerFor = type
            }
        } else {
            appPickerFor = type
        }
    }

    val active = inProgressChallenge
    val activeApp = pickedApp
    if (active != null && activeApp != null) {
        ChallengeActiveScreen(
            challengeType = active,
            sensitivity = moveViewModel.sensitivity,
            appName = activeApp.appName,
            onBackClick = {
                inProgressChallenge = null
                pickedApp = null
            },
            onClaim = {
                moveViewModel.onChallengeCompleted(activeApp.packageName, active)
                inProgressChallenge = null
                pickedApp = null
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftSand)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 15.dp)
            .padding(top = 12.dp, bottom = 24.dp)
    ) {
        Text(
            text = "MOTION LOCK",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.5.sp,
            color = SageAccent
        )
        Text(
            text = "Move to Unlock",
            fontFamily = Nunito,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 22.sp,
            color = DeepOlive
        )

        Spacer(modifier = Modifier.height(18.75.dp))

        XpStreakCard(totalXp = totalXp, streakDays = streakDays)

        Spacer(modifier = Modifier.height(15.dp))

        if (lockedApps.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundLight, RoundedCornerShape(15.dp))
                    .border(1.06.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
                    .padding(15.dp)
            ) {
                Text(
                    text = "No apps locked yet",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lock an app in Settings to unlock Activity Challenges.",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = DeepOlive
                )
            }
        } else {
            Text(
                text = "TODAY'S CHALLENGES",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 19.5.sp,
                color = SageAccent
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(11.25.dp)) {
                ChallengeType.entries.forEach { type ->
                    ChallengeCard(
                        type = type,
                        sensitivity = moveViewModel.sensitivity,
                        isCompleted = type in completedToday,
                        onStartChallenge = { requestChallenge(type) }
                    )
                }
            }
        }

        permissionDeniedMessage?.let { message ->
            Spacer(modifier = Modifier.height(11.25.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontFamily = Nunito,
                fontSize = 13.sp
            )
        }
    }

    appPickerFor?.let { type ->
        AlertDialog(
            onDismissRequest = { appPickerFor = null },
            title = { Text("Pick an app to unlock") },
            text = {
                Column {
                    lockedApps.forEach { app ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pickedApp = app
                                    inProgressChallenge = type
                                    appPickerFor = null
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(text = app.appName)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { appPickerFor = null }) { Text("Cancel") }
            }
        )
    }
}

private val weekDayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
private fun XpStreakCard(totalXp: Int, streakDays: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepOlive, RoundedCornerShape(22.5.dp))
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TOTAL XP EARNED",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = SageAccent
                )
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.75.dp)) {
                    Text(
                        text = "$totalXp",
                        fontFamily = Nunito,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp,
                        color = BackgroundLight
                    )
                    Text(
                        text = "pts",
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = com.example.physi_lock.ui.theme.SecondarySage
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.63.dp)) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MoveStreakOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "$streakDays",
                    fontFamily = Nunito,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 27.sp,
                    color = BackgroundLight
                )
                Text(
                    text = "day streak",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    color = com.example.physi_lock.ui.theme.SecondarySage
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        // No per-day history exists, only a rolling streakDays count — lights up
        // the last min(streakDays, 7) slots rather than fabricating which specific
        // weekdays were active.
        val activeSlots = streakDays.coerceIn(0, 7)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.63.dp)) {
            weekDayLabels.forEachIndexed { index, label ->
                val isActive = index >= weekDayLabels.size - activeSlots
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.75.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .then(
                                if (isActive) {
                                    Modifier.background(MoveStreakOrange.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                } else {
                                    Modifier.border(1.06.dp, SageAccent.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (isActive) MoveStreakOrange else SageAccent.copy(alpha = 0.45f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = if (isActive) SageAccent else SageAccent.copy(alpha = 0.40f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    type: ChallengeType,
    sensitivity: ChallengeSensitivity,
    isCompleted: Boolean,
    onStartChallenge: () -> Unit
) {
    val spec = cardSpecFor(type)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(spec.accentColor.copy(alpha = 0.13f), RoundedCornerShape(15.dp))
            .border(1.06.dp, spec.accentColor.copy(alpha = 0.27f), RoundedCornerShape(15.dp))
            .padding(15.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(11.25.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(spec.accentColor.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = spec.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.63.dp)) {
                    Text(
                        text = spec.title,
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 21.sp,
                        color = DeepOlive
                    )
                    Box(
                        modifier = Modifier
                            .background(spec.accentColor.copy(alpha = 0.13f), RoundedCornerShape(3.75.dp))
                            .padding(horizontal = 5.63.dp, vertical = 1.88.dp)
                    ) {
                        Text(
                            text = "+${type.xpReward} XP",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            color = spec.accentColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(3.75.dp))
                            .padding(horizontal = 5.63.dp, vertical = 1.88.dp)
                    ) {
                        Text(
                            text = targetLabel(type, sensitivity),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            color = DeepOlive
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = spec.description,
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🎁 Unlocks the app you pick for 20 min",
                    fontFamily = Nunito,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                    color = spec.accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(11.25.dp))

        if (isCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(spec.accentColor.copy(alpha = 0.13f), RoundedCornerShape(19.dp))
                    .padding(vertical = 7.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.63.dp)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = spec.accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Completed",
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp,
                        color = spec.accentColor
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(spec.accentColor, RoundedCornerShape(19.dp))
                    .clickable(onClick = onStartChallenge)
                    .padding(vertical = 9.38.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.63.dp)) {
                    Text(
                        text = "Start Challenge",
                        textAlign = TextAlign.Center,
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp,
                        color = BackgroundLight
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BackgroundLight,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
