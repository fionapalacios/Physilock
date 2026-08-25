package com.example.physi_lock.ui.settings

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.UserConfiguration
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SoftSand

/**
 * Built from scratch — the teammate's StudentModeScreen.kt is a confirmed 0-byte stub, no
 * design to port. There's no mode-differentiated backend yet (userMode only labels which
 * profile is active; the settings below are the same global UserConfiguration Student and
 * Work modes currently share) — this screen is honest about that rather than faking
 * per-mode values, and lists the differentiated behavior that isn't built yet as a
 * roadmap, not as if it already works.
 */
@Composable
fun StudentModeScreen(
    config: UserConfiguration,
    isActive: Boolean,
    onSwitchToThisMode: () -> Unit,
    onBackClick: () -> Unit
) {
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
                text = "Student Mode",
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
                    Icon(Icons.Default.School, contentDescription = null, tint = BackgroundLight, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        text = "Tuned for study sessions",
                        fontFamily = Nunito,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BackgroundLight
                    )
                    Text(
                        text = "Stricter lock rules and break reminders",
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
                        text = "Switch to Student Mode",
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
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SageAccent
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "These apply globally right now — Student and Work mode share the same settings below until per-mode presets are built.",
                    fontFamily = Nunito,
                    fontSize = 12.sp,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(11.dp))
                SettingSummaryRow("Daily screen time limit", "${dailyLimitMinutes / 60}h ${dailyLimitMinutes % 60}m")
                SettingSummaryRow("Motion lock sensitivity", ChallengeSensitivity.displayLabel(config.motionLockSensitivity))
                SettingSummaryRow(
                    "Break reminders",
                    if (config.breakReminderEnabled) "Every ${config.breakReminderIntervalMs / 60_000L} min" else "Off"
                )
                SettingSummaryRow("Doomscroll detection", if (config.doomscrollingDetectionEnabled) "On" else "Off")
                SettingSummaryRow(
                    "Unlock challenge",
                    "${sensitivity.armRepsRequired} arm reps / ${sensitivity.walkStepsRequired} steps"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard {
                Text(
                    text = "Planned for Student Mode",
                    fontFamily = Nunito,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Not built yet — noted here so the profile isn't an empty shell.",
                    fontFamily = Nunito,
                    fontSize = 12.sp,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(9.dp))
                PlannedFeatureRow("Class schedule awareness", "Auto-tighten limits during class hours")
                PlannedFeatureRow("Study app allowlist", "Keep note-taking and research apps unlocked")
                PlannedFeatureRow("Stricter default limit", "A lower daily threshold than Work Mode's default")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun SettingSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontFamily = Nunito, fontSize = 13.sp, color = DeepOlive)
        Text(text = value, fontFamily = Nunito, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepOlive)
    }
}

@Composable
internal fun PlannedFeatureRow(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(5.dp)
                .background(SageAccent, RoundedCornerShape(50))
        )
        Column(modifier = Modifier.padding(start = 9.dp)) {
            Text(text = title, fontFamily = Nunito, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepOlive)
            Text(text = description, fontFamily = Nunito, fontSize = 12.sp, color = DeepOlive)
        }
    }
}
