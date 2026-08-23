package com.example.physi_lock.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.data.Account
import com.example.physi_lock.data.auth.FirebaseAccountRepository
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.ui.theme.DeepOlive
import kotlinx.coroutines.launch

private val userModes = listOf("STUDENT_MODE" to "Student", "WORK_MODE" to "Work")
private val sensitivityLevels = listOf("LOW", "MEDIUM", "HIGH")

@Composable
fun SettingsScreen(
    currentAccount: Account?,
    onAccountUpdated: (Account) -> Unit,
    onLogout: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val accountRepository = remember { FirebaseAccountRepository() }

    var showAppLockRules by remember { mutableStateOf(false) }
    var showAccountEditor by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showAppLockRules) {
        AppLockRulesScreen(onBack = { showAppLockRules = false })
        return
    }

    if (showAccountEditor) {
        EditProfileScreen(
            currentAccount = currentAccount,
            errorMessage = errorMessage,
            onBackClick = {
                errorMessage = null
                showAccountEditor = false
            },
            onSave = { updated ->
                coroutineScope.launch {
                    val saved = accountRepository.updateAccount(updated)
                    if (saved == null) {
                        errorMessage = "Username or email is already in use."
                    } else {
                        errorMessage = null
                        showAccountEditor = false
                        onAccountUpdated(saved)
                    }
                }
            }
        )
        return
    }

    val config by settingsViewModel.configuration.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        AccountSummaryCard(
            currentAccount = currentAccount,
            onEditProfile = { showAccountEditor = true },
            onLogout = onLogout
        )

        Card(
            modifier = Modifier
                .padding(8.dp)
                .clickable { showAppLockRules = true },
            colors = CardDefaults.cardColors()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = DeepOlive)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(text = "App Lock Rules", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Choose which apps require a shake challenge to open",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "User Mode", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Tune thresholds for how you use your device",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        userModes.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = config.userMode == value,
                                onClick = { settingsViewModel.setUserMode(value) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = userModes.size)
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Daily Screen Time Limit", style = MaterialTheme.typography.titleLarge)
                val minutes = (config.dailyScreenTimeThresholdMs / 60_000L).toInt().coerceIn(30, 720)
                Text(
                    text = "$minutes minutes (${minutes / 60}h ${minutes % 60}m)",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { settingsViewModel.setDailyScreenTimeThresholdMinutes(it.toInt()) },
                    valueRange = 30f..720f,
                    steps = 22
                )
            }
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Overuse Alert", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Notify me once per day when I exceed my daily screen time limit",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Switch(
                    checked = config.overuseAlertsEnabled,
                    onCheckedChange = { settingsViewModel.setOveruseAlertsEnabled(it) }
                )
            }
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Doomscrolling Detection", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Warn when scrolling patterns suggest doomscrolling",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Switch(
                    checked = config.doomscrollingDetectionEnabled,
                    onCheckedChange = { settingsViewModel.setDoomscrollingDetectionEnabled(it) }
                )
            }
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Break Reminder", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Notify me to stretch or take a walk after continuous device use",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Switch(
                        checked = config.breakReminderEnabled,
                        onCheckedChange = { settingsViewModel.setBreakReminderEnabled(it) }
                    )
                }
                if (config.breakReminderEnabled) {
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    val breakMinutes = (config.breakReminderIntervalMs / 60_000L).toInt().coerceIn(5, 120)
                    Text(
                        text = "Remind me every $breakMinutes minutes",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Slider(
                        value = breakMinutes.toFloat(),
                        onValueChange = { settingsViewModel.setBreakReminderIntervalMinutes(it.toInt()) },
                        valueRange = 5f..120f,
                        steps = 22
                    )
                }
            }
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Motion Lock Sensitivity", style = MaterialTheme.typography.titleLarge)
                val activeSensitivity = ChallengeSensitivity.fromLabel(config.motionLockSensitivity)
                Text(
                    text = "Requires ${activeSensitivity.armRepsRequired} arm-movement reps " +
                        "(or a ${activeSensitivity.walkStepsRequired}-step walk / sustained jog) to unlock",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        sensitivityLevels.forEachIndexed { index, level ->
                            SegmentedButton(
                                selected = config.motionLockSensitivity == level,
                                onClick = { settingsViewModel.setMotionLockSensitivity(level) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = sensitivityLevels.size)
                            ) {
                                Text(level.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSummaryCard(
    currentAccount: Account?,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Account", style = MaterialTheme.typography.titleLarge)
            Text(
                text = currentAccount?.fullName ?: "Signed in user",
                style = MaterialTheme.typography.bodyLarge
            )
            if (currentAccount != null) {
                Text(text = currentAccount.username, style = MaterialTheme.typography.bodyLarge)
                Text(text = currentAccount.email, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = currentAccount.occupation?.takeIf { it.isNotBlank() } ?: "Occupation not set",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onEditProfile) {
                    Text("Manage account settings")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onLogout) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 6.dp))
                    Text("Log out")
                }
            }
        }
    }
}
