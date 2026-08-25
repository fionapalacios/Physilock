package com.example.physi_lock.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.physi_lock.data.DefaultSettings
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.ui.theme.MutedText

private val userModes = listOf("STUDENT_MODE" to "Student", "WORK_MODE" to "Work")
private val sensitivityLevels = listOf("LOW", "MEDIUM", "HIGH")

@Composable
fun AdminDefaultsSection(
    defaults: DefaultSettings,
    onSetUserMode: (String) -> Unit,
    onSetDailyLimitMinutes: (Int) -> Unit,
    onSetDoomscrolling: (Boolean) -> Unit,
    onSetMotionSensitivity: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Applied to new accounts on registration. Motion Lock Sensitivity and Doomscrolling " +
                "Detection are Admin-controlled — users can view these in their own Settings but cannot change them.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Default User Mode", style = MaterialTheme.typography.titleLarge)
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        userModes.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = defaults.userMode == value,
                                onClick = { onSetUserMode(value) },
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
                Text(text = "Default Daily Screen Time Limit", style = MaterialTheme.typography.titleLarge)
                val minutes = (defaults.dailyScreenTimeThresholdMs / 60_000L).toInt().coerceIn(30, 720)
                Text(
                    text = "$minutes minutes (${minutes / 60}h ${minutes % 60}m)",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { onSetDailyLimitMinutes(it.toInt()) },
                    valueRange = 30f..720f,
                    steps = 22
                )
            }
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Default Doomscrolling Detection", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Warn new users when scrolling patterns suggest doomscrolling",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Switch(
                    checked = defaults.doomscrollingDetectionEnabled,
                    onCheckedChange = onSetDoomscrolling
                )
            }
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Default Motion Lock Sensitivity", style = MaterialTheme.typography.titleLarge)
                val activeSensitivity = ChallengeSensitivity.fromLabel(defaults.motionLockSensitivity)
                Text(
                    text = "Requires ${activeSensitivity.armRepsRequired} arm-movement reps " +
                        "(or a ${activeSensitivity.walkStepsRequired}-step walk / sustained jog) to unlock",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        sensitivityLevels.forEachIndexed { index, level ->
                            SegmentedButton(
                                selected = defaults.motionLockSensitivity == level,
                                onClick = { onSetMotionSensitivity(level) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = sensitivityLevels.size)
                            ) {
                                Text(ChallengeSensitivity.displayLabel(level))
                            }
                        }
                    }
                }
            }
        }
    }
}
