package com.example.physi_lock.ui.settings

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
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent

/** Visual pattern matches FocusBlockedAppsScreen's picker exactly -- see that file for why
 *  the two are separate lists rather than a shared one (2026-09-04 design check). Both
 *  restyled together 2026-09-04 to match the rest of the app's real theme (previously bare
 *  MaterialTheme defaults). */
@Composable
fun AppLockRulesScreen(
    onBack: () -> Unit,
    viewModel: AppLockViewModel = viewModel()
) {
    val apps by viewModel.installedApps.collectAsState()
    val lockedPackages by viewModel.lockedPackages.collectAsState()
    val adaptivePackages by viewModel.adaptivePackages.collectAsState()

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
                    text = "App Lock Rules",
                    fontFamily = Nunito,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepOlive
                )
                Text(
                    text = "Locked apps require a shake challenge to open",
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
                    val isLocked = app.packageName in lockedPackages
                    val isAdaptive = app.packageName in adaptivePackages
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                                    .background(if (isLocked) SageAccent else DeepOlive.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.appName.take(1).uppercase(),
                                    fontFamily = Nunito,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLocked) DeepOlive else DeepOlive.copy(alpha = 0.6f)
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
                                checked = isLocked,
                                onCheckedChange = { checked -> viewModel.setLocked(app, checked) },
                                colors = brandedSwitchColors()
                            )
                        }
                        if (isLocked) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 68.dp, end = 16.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Adaptive difficulty",
                                        fontFamily = Nunito,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DeepOlive
                                    )
                                    Text(
                                        text = "Scale the unlock challenge with your current risk level, instead of a fixed difficulty",
                                        fontFamily = Nunito,
                                        fontSize = 11.sp,
                                        color = MutedText
                                    )
                                }
                                Switch(
                                    checked = isAdaptive,
                                    onCheckedChange = { checked -> viewModel.setAdaptive(app, checked) },
                                    colors = brandedSwitchColors()
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = DeepOlive.copy(alpha = 0.08f))
                }
            }
        }
    }
}
