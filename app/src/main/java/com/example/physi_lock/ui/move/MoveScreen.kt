package com.example.physi_lock.ui.move

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
import com.example.physi_lock.ui.move.MoveViewModel

@Composable
fun MoveScreen(moveViewModel: MoveViewModel = viewModel()) {
    val streakDays by moveViewModel.streakDays.collectAsState(initial = 0)

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Move Challenges", style = MaterialTheme.typography.titleLarge)
                Text(text = "Current streak: $streakDays days", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Active Challenge", style = MaterialTheme.typography.titleLarge)
                Text(text = "Shake 20x or Walk 100 steps to unlock", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
