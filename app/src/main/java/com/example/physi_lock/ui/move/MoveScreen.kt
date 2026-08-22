package com.example.physi_lock.ui.move

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.sensor.ChallengeType
import com.example.physi_lock.sensor.isActivityRecognitionGranted
import com.example.physi_lock.ui.challenge.ChallengeProgressContent

private val challengeLabels = mapOf(
    ChallengeType.RUN_JOG to "Run / Jog",
    ChallengeType.WALK to "5-Minute Walk",
    ChallengeType.ROTATIONAL_ARM to "Rotational Arm Movements"
)

@Composable
fun MoveScreen(moveViewModel: MoveViewModel = viewModel()) {
    val context = LocalContext.current
    val streakDays by moveViewModel.streakDays.collectAsState()
    val totalXp by moveViewModel.totalXp.collectAsState()
    val lockedApps by moveViewModel.lockedApps.collectAsState()

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

    val active = inProgressChallenge
    val activeApp = pickedApp
    if (active != null && activeApp != null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = challengeLabels[active] ?: "Challenge", style = MaterialTheme.typography.titleLarge)
            Text(text = "Unlocking ${activeApp.appName}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(32.dp))
            ChallengeProgressContent(
                challengeType = active,
                sensitivity = moveViewModel.sensitivity,
                onComplete = {
                    moveViewModel.onChallengeCompleted(activeApp.packageName, active)
                    inProgressChallenge = null
                    pickedApp = null
                },
                ringColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurface,
                secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { inProgressChallenge = null; pickedApp = null }) {
                Text("Cancel")
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Move Challenges", style = MaterialTheme.typography.titleLarge)
                Text(text = "Total XP: $totalXp", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Current streak: $streakDays days", style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (lockedApps.isEmpty()) {
            Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "No apps locked", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Lock an app in Settings to unlock Activity Challenges.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            ChallengeType.entries.forEach { type ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable {
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
                        },
                    colors = CardDefaults.cardColors()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = challengeLabels[type] ?: type.name, style = MaterialTheme.typography.titleLarge)
                        Text(text = "+${type.xpReward} XP", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        permissionDeniedMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
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
