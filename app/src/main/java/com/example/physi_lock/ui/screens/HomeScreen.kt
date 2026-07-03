package com.example.physi_lock.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.ui.home.HomeViewModel

@Composable
fun HomeScreen(homeViewModel: HomeViewModel = viewModel()) {
    val todayMinutes by homeViewModel.todayScreenTimeMinutes.collectAsState(initial = 0)
    val riskLevel by homeViewModel.riskLevel.collectAsState(initial = "Moderate")

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
                Text(text = "Risk Level", style = MaterialTheme.typography.titleLarge)
                Text(text = riskLevel, style = MaterialTheme.typography.headlineMedium)
            }
        }

    }
}
