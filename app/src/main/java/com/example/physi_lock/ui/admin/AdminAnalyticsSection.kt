package com.example.physi_lock.ui.admin

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.CardCream
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.DmMono
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage

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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        // Previously these query failures were caught and silently defaulted to 0/emptyList
        // with nothing shown -- the stat cards below still show that same safe fallback, but
        // now flagged as a real failure with a retry (reuses the existing Refresh action).
        errorMessage?.let { message ->
            AdminErrorBanner(message = message, onRetry = onRefresh)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.25.dp)
        ) {
            StatCard(value = analytics.totalAccounts.toString(), label = "TOTAL ACCOUNTS", modifier = Modifier.weight(1f))
            StatCard(value = analytics.activeAccounts.toString(), label = "ACTIVE ACCOUNTS", modifier = Modifier.weight(1f))
        }

        AnalyticsCard {
            CardTitle("Top Apps (min/day, last 7 days)")
            if (analytics.topAppsDaily.isEmpty()) {
                Text(
                    text = "No usage data logged yet.",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = MutedText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                val palette = listOf(SageAccent, Orchid, SecondarySage)
                AdminDailyBarChart(
                    dayLabels = analytics.dailyLabels,
                    series = analytics.topAppsDaily.mapIndexed { index, app ->
                        ChartSeries(label = app.appName, color = palette[index % palette.size], values = app.minutesByDay)
                    }
                )
            }
        }

        AnalyticsCard {
            CardTitle("Daily Active Users (last 7 days)")
            AdminDailyBarChart(
                dayLabels = analytics.dailyLabels,
                series = listOf(ChartSeries(label = "Active users", color = SageAccent, values = analytics.dailyActiveUsers))
            )
        }

        AnalyticsCard {
            CardTitle("Motion Challenges Completed (last 7 days)")
            AdminDailyBarChart(
                dayLabels = analytics.dailyLabels,
                series = listOf(ChartSeries(label = "Challenges completed", color = Orchid, values = analytics.dailyChallengesCompleted))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.8.dp, DeepOlive.copy(alpha = 0.20f), RoundedCornerShape(15.dp))
                .clickable(onClick = onRefresh)
                .padding(vertical = 13.13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = DeepOlive, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(7.5.dp))
            Text(text = "Refresh Analytics", fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepOlive)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepOlive, RoundedCornerShape(15.dp))
                .clickable {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Physi-Lock Research Data Export")
                        putExtra(Intent.EXTRA_TEXT, buildExportText())
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export Research Data"))
                }
                .padding(vertical = 13.13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, tint = BackgroundLight, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(7.5.dp))
            Text(text = "Export Research Data", fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BackgroundLight)
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun AnalyticsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardCream, RoundedCornerShape(22.5.dp))
            .border(0.8.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(22.5.dp))
            .padding(15.dp),
        content = content
    )
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text = text,
        fontFamily = Nunito,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        color = DeepOlive
    )
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(CardCream, RoundedCornerShape(22.5.dp))
            .border(0.8.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(22.5.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(3.75.dp)
    ) {
        Text(text = value, fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 26.sp, color = DeepOlive)
        Text(text = label, fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = SageAccent)
    }
}
