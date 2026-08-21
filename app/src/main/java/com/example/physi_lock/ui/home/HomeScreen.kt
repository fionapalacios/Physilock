package com.example.physi_lock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.ui.home.HomeViewModel

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onManageAppLock: () -> Unit = {}
) {
    val todayMinutes by homeViewModel.todayScreenTimeMinutes.collectAsState(initial = 0)
    val lockSensitivity by homeViewModel.lockSensitivity.collectAsState(initial = "Moderate")
    val lockedAppsToday by homeViewModel.lockedAppsToday.collectAsState(initial = emptyList())

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Today's Screen Time", style = MaterialTheme.typography.titleLarge)
                Text(text = "$todayMinutes minutes", style = MaterialTheme.typography.displayLarge)
            }
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Motion Lock Sensitivity", style = MaterialTheme.typography.titleLarge)
                Text(text = lockSensitivity, style = MaterialTheme.typography.headlineMedium)
            }
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Locked Apps Today", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onManageAppLock) {
                        Text("Manage")
                    }
                }
                if (lockedAppsToday.isEmpty()) {
                    Text(
                        text = "No apps locked yet. Tap Manage to pick apps that require a shake challenge to open.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        lockedAppsToday.forEach { app ->
                            val minutes = app.totalDurationMs / 60000L
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = app.appName, style = MaterialTheme.typography.bodyLarge)
                                Text(text = "${minutes}m today", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
