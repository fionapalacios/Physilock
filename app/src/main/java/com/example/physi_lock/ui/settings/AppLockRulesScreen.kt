package com.example.physi_lock.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.SageAccent

@Composable
fun AppLockRulesScreen(
    onBack: () -> Unit,
    viewModel: AppLockViewModel = viewModel()
) {
    val apps by viewModel.installedApps.collectAsState()
    val lockedPackages by viewModel.lockedPackages.collectAsState()
    val adaptivePackages by viewModel.adaptivePackages.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(
                    text = "App Lock Rules",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Locked apps require a shake challenge to open",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Loading installed apps...", style = MaterialTheme.typography.bodyMedium)
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
                                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                    color = if (isLocked) DeepOlive else DeepOlive.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(text = app.appName, fontWeight = FontWeight.SemiBold)
                            }
                            Switch(
                                checked = isLocked,
                                onCheckedChange = { checked -> viewModel.setLocked(app, checked) }
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
                                    Text(text = "Adaptive difficulty", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        text = "Scale the unlock challenge with your current risk level, instead of a fixed difficulty",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isAdaptive,
                                    onCheckedChange = { checked -> viewModel.setAdaptive(app, checked) }
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
