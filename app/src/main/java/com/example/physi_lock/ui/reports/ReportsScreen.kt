package com.example.physi_lock.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.data.AppUsageTotal
import com.example.physi_lock.data.ExcessiveUsagePredictionLog
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SoftSand
import com.example.physi_lock.ui.theme.TertiaryTan
import kotlin.math.roundToInt

private val PrimaryDark = DeepOlive
private val PrimaryGreen = SageAccent
private val AccentLavender = Orchid
private val AuthTabsBackground = SoftSand

@Composable
fun ReportsScreen(reportsViewModel: ReportsViewModel = viewModel()) {
    val weeklyUsage by reportsViewModel.weeklyUsage.collectAsState(initial = emptyList())
    val topApps by reportsViewModel.topApps.collectAsState(initial = emptyList())
    val insights by reportsViewModel.insights.collectAsState(initial = emptyList())
    val dailyLimitMinutes by reportsViewModel.dailyLimitMinutes.collectAsState(initial = 480)
    val todaysPredictions by reportsViewModel.todaysPredictions.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftSand)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 15.dp)
            .padding(top = 12.dp, bottom = 24.dp)
    ) {
        Text(
            text = "ANALYTICS",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = PrimaryGreen
        )
        Text(
            text = "Usage Reports",
            fontFamily = Nunito,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryDark
        )

        Spacer(modifier = Modifier.height(15.dp))

        val todayMinutes = weeklyUsage.lastOrNull { it.isToday }?.minutes ?: 0
        val avgMinutes = weeklyUsage.map { it.minutes }.average().takeIf { !it.isNaN() } ?: 0.0
        val totalMinutes = weeklyUsage.sumOf { it.minutes }
        val daysTracked = weeklyUsage.count { it.minutes > 0 }
        val avgDelta = if (avgMinutes > 0) {
            val diffPct = (((todayMinutes - avgMinutes) / avgMinutes) * 100).roundToInt()
            if (diffPct >= 0) "$diffPct% above avg today" else "${-diffPct}% below avg today"
        } else {
            "No usage yet"
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            StatCard(
                label = "Avg Daily",
                value = formatMinutes(avgMinutes.roundToInt()),
                delta = avgDelta,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Total Week",
                value = formatMinutes(totalMinutes),
                delta = "$daysTracked/7 days tracked",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AuthTabsBackground, RoundedCornerShape(22.dp))
                .border(1.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                .padding(15.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Daily Usage",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryDark
                )
                Text(
                    text = "mins / day",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = PrimaryGreen
                )
            }
            Spacer(modifier = Modifier.height(11.dp))
            DailyUsageChart(days = weeklyUsage, dailyLimitMinutes = dailyLimitMinutes)
            Spacer(modifier = Modifier.height(4.dp))
            DailyUsageLegend()
        }

        Spacer(modifier = Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AuthTabsBackground, RoundedCornerShape(22.dp))
                .border(1.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                .padding(15.dp)
        ) {
            Text(
                text = "Top Apps This Week",
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryDark
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (topApps.isEmpty()) {
                Text(
                    text = "No app usage logged yet this week.",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = DeepOlive
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    topApps.forEach { app -> TopAppRow(app, topApps) }
                }
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AuthTabsBackground, RoundedCornerShape(22.dp))
                .border(1.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                .padding(15.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Today's Usage Predictions",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryDark
                )
                Text(
                    text = "AI",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = PrimaryGreen
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (todaysPredictions.isEmpty()) {
                Text(
                    text = "Predictions appear here throughout the day as they're generated.",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = DeepOlive
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    todaysPredictions.sortedBy { it.hour }.forEach { prediction -> PredictionRow(prediction) }
                }
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AuthTabsBackground, RoundedCornerShape(22.dp))
                .border(1.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                .padding(15.dp)
        ) {
            Text(
                text = "Insights",
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryDark
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (insights.isEmpty()) {
                Text(
                    text = "Insights appear once a full day of usage has been logged.",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = DeepOlive
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    insights.forEach { insight -> InsightRow(insight) }
                }
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

@Composable
private fun StatCard(label: String, value: String, delta: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(AuthTabsBackground, RoundedCornerShape(15.dp))
            .border(1.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
            .padding(11.dp)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = PrimaryGreen
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = Nunito,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryDark
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = delta,
            fontFamily = Nunito,
            fontSize = 12.sp,
            color = DeepOlive
        )
    }
}

private val chartHeight = 104.dp

@Composable
private fun DailyUsageChart(days: List<DayUsage>, dailyLimitMinutes: Int) {
    val maxMinutes = (days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.height(chartHeight).width(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(maxMinutes, maxMinutes * 2 / 3, maxMinutes / 3, 0).forEach { minutes ->
                Text(
                    text = "${minutes}m",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = PrimaryGreen
                )
            }
        }
        Spacer(modifier = Modifier.width(7.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(chartHeight),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                days.forEach { day ->
                    val isOverLimit = day.minutes > dailyLimitMinutes
                    val barColor = when {
                        day.isToday -> PrimaryDark
                        isOverLimit -> AccentLavender
                        else -> PrimaryGreen
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .fillMaxHeight((day.minutes.toFloat() / maxMinutes).coerceIn(0f, 1f))
                                .background(barColor.copy(alpha = 0.85f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                days.forEach { day ->
                    Text(
                        text = day.dayLabel,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = DeepOlive
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyUsageLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(15.dp), modifier = Modifier.padding(top = 4.dp)) {
        LegendDot(color = PrimaryDark, label = "Today")
        LegendDot(color = AccentLavender, label = "Over limit")
        LegendDot(color = PrimaryGreen, label = "Normal")
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = DeepOlive
        )
    }
}

@Composable
private fun TopAppRow(app: AppUsageTotal, allApps: List<AppUsageTotal>) {
    val maxMs = (allApps.maxOfOrNull { it.totalDurationMs } ?: 0L).coerceAtLeast(1L)
    val minutes = app.totalDurationMs / 60_000L
    val progress = (app.totalDurationMs.toFloat() / maxMs.toFloat()).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = app.appName,
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )
            Text(
                text = "${minutes}m",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = DeepOlive
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(TertiaryTan, RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(PrimaryGreen, RoundedCornerShape(50))
            )
        }
    }
}

private fun formatHour(hour: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour $period"
}

@Composable
private fun PredictionRow(prediction: ExcessiveUsagePredictionLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (prediction.isExcessive) AccentLavender.copy(alpha = 0.10f) else PrimaryGreen.copy(alpha = 0.08f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatHour(prediction.hour),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryDark
        )
        Text(
            text = "~${prediction.predictedUsageMinutes.roundToInt()} min predicted",
            fontFamily = Nunito,
            fontSize = 13.sp,
            color = DeepOlive
        )
        Text(
            text = if (prediction.isExcessive) "Excessive" else "Normal",
            fontFamily = Nunito,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (prediction.isExcessive) AccentLavender else PrimaryGreen
        )
    }
}

@Composable
private fun InsightRow(insight: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryGreen.copy(alpha = 0.10f), RoundedCornerShape(15.dp))
            .border(1.dp, PrimaryGreen.copy(alpha = 0.25f), RoundedCornerShape(15.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = insight,
            fontFamily = Nunito,
            fontSize = 13.sp,
            color = DeepOlive
        )
    }
}
