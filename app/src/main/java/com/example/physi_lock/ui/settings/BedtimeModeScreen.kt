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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.CardCream
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent

private fun hourLabel(hour: Int): String {
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:00 ${if (hour < 12) "AM" else "PM"}"
}

/** Ported from the teammate's Figma "BedtimeModePage" (2026-09-04), with real backend
 *  behind it -- see UserConfiguration.bedtimeStart/EndMinute and
 *  AppMonitorService.isWithinBedtimeWindow for the actual enforcement. Per instruction,
 *  the mockup's separate "Enable Bedtime Mode" toggle was dropped: same as Class/Work
 *  Mode, this is active purely by the current time falling in the configured window,
 *  nothing else to switch on/off. Hour-only granularity (DropdownMenu, matching
 *  ScheduleBlockSection's picker) rather than the mockup's native minute-precision time
 *  input, for consistency with the rest of the app's time pickers. */
@Composable
fun BedtimeModeScreen(
    onBackClick: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val config by settingsViewModel.configuration.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
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
                text = "Bedtime Mode",
                fontFamily = Nunito,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepOlive
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 15.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardCream)
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Icon(imageVector = Icons.Filled.Bedtime, contentDescription = null, tint = DeepOlive, modifier = Modifier.size(16.dp))
                Text(
                    text = "Apps are locked during Bedtime Mode, based on the hours below",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepOlive,
                    modifier = Modifier.weight(1f)
                )
            }

            HourField(
                label = "BEDTIME STARTS",
                hour = config.bedtimeStartMinute / 60,
                onSelect = { hour -> settingsViewModel.setBedtimeStartMinute(hour * 60) }
            )
            HourField(
                label = "WAKE TIME",
                hour = config.bedtimeEndMinute / 60,
                onSelect = { hour -> settingsViewModel.setBedtimeEndMinute(hour * 60) }
            )

            Text(
                text = "Apps are locked during Bedtime Mode. Only whitelisted apps (see Whitelist Manager) remain accessible.",
                fontFamily = Nunito,
                fontSize = 12.sp,
                color = MutedText,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HourField(label: String, hour: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardCream)
            .clickable { expanded = true }
            .padding(15.dp)
    ) {
        Column {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SageAccent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hourLabel(hour),
                fontFamily = FontFamily.Monospace,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepOlive
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (0..23).forEach { h ->
                DropdownMenuItem(
                    text = { Text(hourLabel(h)) },
                    onClick = { onSelect(h); expanded = false }
                )
            }
        }
    }
}
