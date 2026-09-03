package com.example.physi_lock.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.ui.settings.brandedSwitchColors
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent

/** Lets the User pick which apps get blocked during their own Focus Mode sessions --
 *  deliberately separate from Admin's app categories (Admin curates categories for
 *  tracking/reporting; blocking policy is the User's own call), and deliberately its own
 *  list rather than reusing App Lock Rules (2026-09-04 design check: App Lock Rules gates
 *  an app behind a challenge and it still opens after, always-on; this is a hard block, no
 *  challenge, only while a Focus session is running -- YPT's dedicated study-session
 *  blocklist model, not One Sec's single-list-reused-across-modes one). Visual pattern
 *  matches AppLockRulesScreen's picker exactly, and both match the rest of the app's real
 *  theme (2026-09-04 -- previously both used bare MaterialTheme defaults, not Nunito/
 *  DeepOlive/BackgroundLight like every other screen). */
@Composable
fun FocusBlockedAppsScreen(
    onBack: () -> Unit,
    viewModel: FocusModeViewModel = viewModel()
) {
    val apps by viewModel.installedApps.collectAsState()
    val blockedPackages by viewModel.blockedPackages.collectAsState()

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
                modifier = Modifier.size(20.dp).clickable(onClick = onBack)
            )
            Column {
                Text(
                    text = "Focus Mode Blocked Apps",
                    fontFamily = Nunito,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepOlive
                )
                Text(
                    text = "Choose which apps stay locked during a Focus session",
                    fontFamily = Nunito,
                    fontSize = 12.sp,
                    color = MutedText
                )
            }
        }

        if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Loading installed apps...", fontFamily = Nunito, fontSize = 13.sp, color = MutedText)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(apps, key = { it.packageName }) { app ->
                    val isBlocked = app.packageName in blockedPackages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isBlocked) SageAccent else DeepOlive.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = app.appName.take(1).uppercase(),
                                fontFamily = Nunito,
                                fontWeight = FontWeight.Bold,
                                color = if (isBlocked) DeepOlive else DeepOlive.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = app.appName,
                            fontFamily = Nunito,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = DeepOlive,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                        Switch(
                            checked = isBlocked,
                            onCheckedChange = { checked -> viewModel.setBlocked(app, checked) },
                            colors = brandedSwitchColors()
                        )
                    }
                    HorizontalDivider(color = DeepOlive.copy(alpha = 0.08f))
                }
            }
        }
    }
}
