package com.example.physi_lock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.ui.reports.DayUsage
import com.example.physi_lock.ui.reports.ReportsViewModel

@Composable
fun ReportsScreen(reportsViewModel: ReportsViewModel = viewModel()) {
    val weeklyUsage by reportsViewModel.weeklyUsage.collectAsState(initial = emptyList())

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Weekly Screen Time", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Today vs. the last 7 days, in minutes",
                    style = MaterialTheme.typography.bodyLarge
                )
                WeeklyBarChart(
                    days = weeklyUsage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Category Breakdown", style = MaterialTheme.typography.titleLarge)
                Text(text = "Placeholder for bar/pie chart (MPAndroidChart or Compose chart)", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private val chartHeight = 140.dp
private val barWidth = 20.dp
private val valueLabelHeight = 20.dp

@Composable
private fun WeeklyBarChart(days: List<DayUsage>, modifier: Modifier = Modifier) {
    val maxMinutes = (days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Fixed-height slot so every column lines up even when only today's bar is labeled.
                Box(modifier = Modifier.height(valueLabelHeight), contentAlignment = Alignment.BottomCenter) {
                    if (day.isToday) {
                        Text(
                            text = "${day.minutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Box(modifier = Modifier.height(chartHeight), contentAlignment = Alignment.BottomCenter) {
                    val barHeight = chartHeight * (day.minutes.toFloat() / maxMinutes)
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (day.isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                    )
                }
                Text(
                    text = day.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
