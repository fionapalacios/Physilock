package com.example.physi_lock.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.physi_lock.data.DefaultSettings
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.CardCream
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.DmMono
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.PillTrackBackground
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.TertiaryTan

private val userModes = listOf("STUDENT_MODE" to "Student", "WORK_MODE" to "Work")
private val sensitivityLevels = listOf("LOW" to "Low", "MODERATE" to "Moderate", "HIGH" to "High")

@Composable
fun AdminDefaultsSection(
    defaults: DefaultSettings,
    onSetUserMode: (String) -> Unit,
    onSetDailyLimitMinutes: (Int) -> Unit,
    onSetDoomscrolling: (Boolean) -> Unit,
    onSetDoomscrollingSensitivity: (String) -> Unit,
    onSetMotionSensitivity: (String) -> Unit
) {
    var justSaved by remember { mutableStateOf(false) }
    LaunchedEffect(justSaved) {
        if (justSaved) {
            delay(1500)
            justSaved = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(11.25.dp)
    ) {
        DefaultsCard {
            CardTitle("Default User Mode")
            SegmentedPill(
                options = userModes,
                selected = defaults.userMode,
                onSelect = onSetUserMode
            )
        }

        DefaultsCard {
            CardTitle("Default Daily Screen Time Limit")
            val minutes = (defaults.dailyScreenTimeThresholdMs / 60_000L).toInt().coerceIn(30, 720)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Limit", fontFamily = Nunito, fontSize = 13.sp, color = MutedText)
                Text(
                    text = "$minutes minutes (${minutes / 60}h ${minutes % 60}m)",
                    fontFamily = DmMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = DeepOlive
                )
            }
            Slider(
                value = minutes.toFloat(),
                onValueChange = { onSetDailyLimitMinutes(it.toInt()) },
                valueRange = 30f..720f,
                steps = 22,
                colors = SliderDefaults.colors(
                    thumbColor = DeepOlive,
                    activeTrackColor = DeepOlive,
                    inactiveTrackColor = TertiaryTan
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "30 min", fontFamily = DmMono, fontSize = 11.sp, color = SageAccent)
                Text(text = "12 h", fontFamily = DmMono, fontSize = 11.sp, color = SageAccent)
            }
        }

        DefaultsCard {
            CardTitle("Default Doomscrolling Detection")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Warn new users when scrolling patterns suggest doomscrolling",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = MutedText,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(11.dp))
                Switch(
                    checked = defaults.doomscrollingDetectionEnabled,
                    onCheckedChange = onSetDoomscrolling,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = DeepOlive,
                        checkedThumbColor = BackgroundLight,
                        uncheckedTrackColor = TertiaryTan,
                        uncheckedThumbColor = BackgroundLight,
                        uncheckedBorderColor = DeepOlive.copy(alpha = 0.25f)
                    )
                )
            }
        }

        DefaultsCard {
            CardTitle("Default Doomscrolling Sensitivity")
            Text(
                text = "Moderate matches today's existing behavior; Low raises the bar for fewer " +
                    "alerts, High lowers it for more — layered on top of the risk-level detection " +
                    "recipe, not a replacement for it.",
                fontFamily = Nunito,
                fontSize = 12.sp,
                color = SageAccent
            )
            SegmentedPill(
                options = sensitivityLevels,
                selected = defaults.doomscrollingSensitivity,
                onSelect = onSetDoomscrollingSensitivity
            )
        }

        DefaultsCard {
            CardTitle("Default Motion Lock Sensitivity")
            val activeSensitivity = ChallengeSensitivity.fromLabel(defaults.motionLockSensitivity)
            Text(
                text = "Requires ${activeSensitivity.armRepsRequired} arm-movement reps " +
                    "(or a ${activeSensitivity.walkStepsRequired}-step walk / sustained jog) to unlock",
                fontFamily = DmMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = SageAccent
            )
            SegmentedPill(
                options = sensitivityLevels,
                selected = defaults.motionLockSensitivity,
                onSelect = onSetMotionSensitivity
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepOlive, RoundedCornerShape(15.dp))
                .clickable { justSaved = true }
                .padding(vertical = 15.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (justSaved) "Saved" else "Save Changes",
                textAlign = TextAlign.Center,
                fontFamily = Nunito,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = BackgroundLight
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun DefaultsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardCream, RoundedCornerShape(22.5.dp))
            .border(0.8.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(22.5.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(11.25.dp),
        content = content
    )
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text = text,
        fontFamily = Nunito,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        color = DeepOlive
    )
}

@Composable
private fun SegmentedPill(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PillTrackBackground, RoundedCornerShape(19.dp))
            .padding(3.75.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(value) }
                    .background(
                        if (isSelected) DeepOlive else Color.Transparent,
                        RoundedCornerShape(15.dp)
                    )
                    .padding(vertical = 7.5.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    textAlign = TextAlign.Center,
                    fontFamily = Nunito,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) BackgroundLight else MutedText
                )
            }
        }
    }
}
