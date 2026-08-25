package com.example.physi_lock.ui.admin

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText

@Composable
fun AdminAnalyticsSection(
    analytics: AdminAnalytics,
    errorMessage: String?,
    onRefresh: () -> Unit,
    buildExportText: () -> String
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Aggregate, anonymized usage across all accounts — no individual user is identifiable here.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Previously these query failures were caught and silently defaulted to 0/emptyList
        // with nothing shown -- the stat cards below still show that same safe fallback, but
        // now flagged as a real failure with a retry (reuses the existing Refresh action).
        errorMessage?.let { message ->
            AdminErrorBanner(
                message = message,
                onRetry = onRefresh,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            StatCard(label = "Total Accounts", value = analytics.totalAccounts.toString(), modifier = Modifier.weight(1f))
            StatCard(label = "Active Accounts", value = analytics.activeAccounts.toString(), modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            StatCard(label = "Categorized Apps", value = analytics.categorizedAppCount.toString(), modifier = Modifier.weight(1f))
            StatCard(label = "Usage (7d, min)", value = analytics.totalUsageMinutesLast7Days.toString(), modifier = Modifier.weight(1f))
        }

        Card(modifier = Modifier.padding(8.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Top Apps (Last 7 Days)", style = MaterialTheme.typography.titleLarge)
                if (analytics.topApps.isEmpty()) {
                    Text(
                        text = "No usage data logged yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    analytics.topApps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(app.appName, fontWeight = FontWeight.SemiBold)
                            Text("${app.totalDurationMs / 60_000L} min", color = MutedText)
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onRefresh() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Refresh Analytics")
        }

        Button(
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Physi-Lock Research Data Export")
                    putExtra(Intent.EXTRA_TEXT, buildExportText())
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export Research Data"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepOlive, contentColor = Color(0xFFFEFEFE))
        ) {
            Text("Export Research Data", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(4.dp), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = DeepOlive, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MutedText)
        }
    }
}
