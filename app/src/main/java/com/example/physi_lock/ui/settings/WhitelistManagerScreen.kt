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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.CardCream
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.ErrorRed
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import kotlinx.coroutines.delay

/** Direct shortcut to Student Mode's "Study App Allowlist" (same real data as
 *  AllowlistSection in ScheduleBlockSection.kt / ScheduleViewModel -- reachable from
 *  Settings without going through Student Mode's schedule-block screen), but with its own
 *  layout ported from the teammate's Figma "WhitelistPage" (2026-09-04): a two-section
 *  always-accessible/add-app split with remove/add affordances, instead of AllowlistSection's
 *  compact single-list-with-switches (kept as-is for its embedded use in StudentModeScreen).
 *
 *  The Figma copy claimed whitelisted apps are "never locked by any mechanism -- not by
 *  usage goals, adaptive lock, Focus Mode, or Bedtime Mode." That's not what the real
 *  allowlist does (see AppMonitorService.onAccessibilityEvent): it only exempts apps from
 *  Class Mode's schedule block. Adaptive Lock's challenge check runs first and overrides it
 *  even then, Focus Mode uses a separate blocklist entirely, usage goals aren't consulted
 *  here at all, and Bedtime Mode doesn't exist yet. Copy below describes the real scope
 *  instead of porting the mockup's broader claim verbatim. */
@Composable
fun WhitelistManagerScreen(
    onBackClick: () -> Unit,
    scheduleViewModel: ScheduleViewModel = viewModel()
) {
    val installedApps by scheduleViewModel.installedApps.collectAsState()
    val allowlistedApps by scheduleViewModel.allowlistedApps.collectAsState()
    val allowlistedPackages = allowlistedApps.map { it.packageName }.toSet()
    val addableApps = installedApps.filter { it.packageName !in allowlistedPackages }

    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(saved) {
        if (saved) {
            delay(2000)
            saved = false
        }
    }

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
                text = "Whitelist Manager",
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
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Whitelisted apps stay reachable during an active class schedule block, " +
                    "when everything else (except system apps like Phone and Settings) is sent " +
                    "to the home screen. This applies to Class Mode only, not Adaptive Lock or Focus Mode.",
                fontFamily = Nunito,
                fontSize = 13.sp,
                color = MutedText,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SectionLabel("ALWAYS ACCESSIBLE")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardCream)
            ) {
                if (allowlistedApps.isEmpty()) {
                    Text(
                        text = "No whitelisted apps yet.",
                        fontFamily = Nunito,
                        fontSize = 13.sp,
                        color = MutedText,
                        modifier = Modifier.padding(15.dp)
                    )
                } else {
                    allowlistedApps.forEachIndexed { index, app ->
                        WhitelistRow(
                            appName = app.appName,
                            showDivider = index > 0,
                            trailing = {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ErrorRed.copy(alpha = 0.07f))
                                        .clickable {
                                            scheduleViewModel.setAllowlisted(
                                                InstalledAppInfo(app.packageName, app.appName),
                                                false
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Remove,
                                        contentDescription = "Remove ${app.appName}",
                                        tint = ErrorRed,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            SectionLabel("ADD APP")

            if (addableApps.isEmpty()) {
                Text(
                    text = if (installedApps.isEmpty()) "Loading installed apps..." else "All installed apps are whitelisted.",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = MutedText.copy(alpha = 0.7f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    addableApps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardCream)
                                .clickable { scheduleViewModel.setAllowlisted(app, true) }
                                .padding(horizontal = 15.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            AppAvatar(appName = app.appName, allowed = false)
                            Text(
                                text = app.appName,
                                fontFamily = Nunito,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepOlive,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add ${app.appName}",
                                tint = SageAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepOlive)
                    .clickable { saved = true }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (saved) "Saved!" else "Save Whitelist",
                    fontFamily = Nunito,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BackgroundLight
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = SageAccent,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun WhitelistRow(appName: String, showDivider: Boolean, trailing: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DeepOlive.copy(alpha = 0.07f))
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            AppAvatar(appName = appName, allowed = true)
            Text(
                text = appName,
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DeepOlive,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }
    }
}

@Composable
private fun AppAvatar(appName: String, allowed: Boolean) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (allowed) SageAccent else DeepOlive.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = appName.take(1).uppercase(),
            fontFamily = Nunito,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (allowed) DeepOlive else DeepOlive.copy(alpha = 0.6f)
        )
    }
}
