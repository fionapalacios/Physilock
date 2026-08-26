package com.example.physi_lock.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.data.AppCategoryType
import com.example.physi_lock.data.AppUsageTotal
import com.example.physi_lock.data.ExcessiveUsagePredictionLog
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage
import com.example.physi_lock.ui.theme.SoftSand
import com.example.physi_lock.ui.theme.TertiaryTan
import kotlin.math.abs
import kotlin.math.roundToInt

private val PrimaryDark = DeepOlive
private val PrimaryGreen = SageAccent
private val AccentLavender = Orchid
private val AuthTabsBackground = SoftSand

/** DAILY = the existing 7-day per-day chart; WEEKLY = the real 4-week aggregate view
 *  (2026-08-27, see ReportsViewModel.buildWeeklyBreakdown) — replaces the old static
 *  "Weekly view coming soon" placeholder. (Renamed from WEEK/MONTH, which had drifted
 *  out of sync with their own tab labels — WEEK showed "Daily", MONTH showed "Weekly".) */
private enum class ReportPeriod { DAILY, WEEKLY }

@Composable
fun ReportsScreen(reportsViewModel: ReportsViewModel = viewModel()) {
    var period by remember { mutableStateOf(ReportPeriod.DAILY) }
    val weeklyUsage by reportsViewModel.weeklyUsage.collectAsState(initial = emptyList())
    val weeklyBreakdown by reportsViewModel.weeklyBreakdown.collectAsState(initial = emptyList())
    val topApps by reportsViewModel.topApps.collectAsState(initial = emptyList())
    val categoryBreakdown by reportsViewModel.categoryBreakdown.collectAsState(initial = emptyList())
    val insights by reportsViewModel.insights.collectAsState(initial = emptyList())
    val dailyLimitMinutes by reportsViewModel.dailyLimitMinutes.collectAsState(initial = 480)
    val todaysPredictions by reportsViewModel.todaysPredictions.collectAsState(initial = emptyList())
    val usagePatterns by reportsViewModel.usagePatterns.collectAsState(initial = UsagePatterns())

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

        PeriodTabs(selected = period, onSelected = { period = it })

        Spacer(modifier = Modifier.height(15.dp))

        if (period == ReportPeriod.WEEKLY) {
            WeeklyBreakdownContent(weeklyBreakdown)
            return@Column
        }

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
                text = "Category Breakdown",
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryDark
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (categoryBreakdown.isEmpty()) {
                Text(
                    text = "Categorize apps in Admin to see a usage breakdown by category.",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = DeepOlive
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    categoryBreakdown.forEach { category -> CategoryRow(category) }
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AI Insights",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryDark
                )
                Box(
                    modifier = Modifier
                        .background(AccentLavender.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AI-POWERED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentLavender
                    )
                }
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

        Spacer(modifier = Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AuthTabsBackground, RoundedCornerShape(22.dp))
                .border(1.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                .padding(15.dp)
        ) {
            Text(
                text = "Usage Patterns",
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Recurring habits across the last 7 days — not a single-day comparison like Insights above.",
                fontFamily = Nunito,
                fontSize = 12.sp,
                color = DeepOlive.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val patternRows = buildList {
                usagePatterns.peakHour?.let { hour ->
                    add("You're most active around ${formatHour(hour)}.")
                }
                val weekday = usagePatterns.weekdayAvgMinutes
                val weekend = usagePatterns.weekendAvgMinutes
                if (weekday != null && weekend != null && weekday != weekend) {
                    val diffPct = (abs(weekend - weekday).toFloat() / weekday.coerceAtLeast(1) * 100).roundToInt()
                    add(
                        if (weekend > weekday) {
                            "You use your phone $diffPct% more on weekends than weekdays."
                        } else {
                            "You use your phone $diffPct% more on weekdays than weekends."
                        }
                    )
                }
            }

            if (patternRows.isEmpty()) {
                Text(
                    text = "Patterns appear once a full week of usage has been logged.",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = DeepOlive
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    patternRows.forEach { pattern -> InsightRow(pattern) }
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

/** Ported from the teammate's sprint-2-ui-navigation branch ReportsScreen (`PeriodTabs`),
 *  remapped to this repo's theme tokens. */
@Composable
private fun PeriodTabs(selected: ReportPeriod, onSelected: (ReportPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthTabsBackground, RoundedCornerShape(19.dp))
            .padding(4.dp)
    ) {
        ReportPeriod.entries.forEach { entry ->
            val isActive = entry == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isActive) Modifier.background(PrimaryDark, RoundedCornerShape(15.dp)) else Modifier
                    )
                    .clickable { onSelected(entry) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (entry == ReportPeriod.DAILY) "Daily" else "Weekly",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) BackgroundLight else DeepOlive
                )
            }
        }
    }
}

/** Reports' real Weekly view (2026-08-27, see ReportsViewModel.weeklyBreakdown) — a
 *  4-week bar chart + stat cards, scoped intentionally smaller than the Daily view: Top
 *  Apps/Category Breakdown/Insights/AI Insights stay Daily-only, not duplicated per-week. */
@Composable
private fun WeeklyBreakdownContent(weeks: List<WeekUsage>) {
    if (weeks.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AuthTabsBackground, RoundedCornerShape(22.dp))
                .border(1.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No usage logged yet this month",
                fontFamily = Nunito,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryDark
            )
        }
        return
    }

    val avgMinutes = weeks.map { it.totalMinutes }.average().roundToInt()
    val totalMinutes = weeks.sumOf { it.totalMinutes }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        StatCard(
            label = "Avg / Week",
            value = formatMinutes(avgMinutes),
            delta = "${weeks.size}-week average",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Total (${weeks.size} wk)",
            value = formatMinutes(totalMinutes),
            delta = "Since ${weeks.first().weekLabel}",
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
                text = "Weekly Usage",
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryDark
            )
            Text(
                text = "mins / week",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = PrimaryGreen
            )
        }
        Spacer(modifier = Modifier.height(11.dp))
        WeeklyUsageChart(weeks)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Top Apps, Category Breakdown, and Insights stay scoped to the last 7 days above.",
            fontFamily = Nunito,
            fontSize = 11.sp,
            color = DeepOlive.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun WeeklyUsageChart(weeks: List<WeekUsage>) {
    val maxMinutes = (weeks.maxOfOrNull { it.totalMinutes } ?: 0).coerceAtLeast(1)

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.height(chartHeight).width(32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(maxMinutes, maxMinutes * 2 / 3, maxMinutes / 3, 0).forEach { minutes ->
                Text(
                    text = formatMinutes(minutes),
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
                weeks.forEach { week ->
                    val barColor = if (week.isCurrentWeek) PrimaryDark else PrimaryGreen
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight((week.totalMinutes.toFloat() / maxMinutes).coerceIn(0f, 1f))
                                .background(barColor.copy(alpha = 0.85f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                weeks.forEach { week ->
                    Text(
                        text = week.weekLabel,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = DeepOlive
                    )
                }
            }
        }
    }
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
    val minutes = (app.totalDurationMs / 60_000L).toInt()
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
                text = formatMinutes(minutes),
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

private fun colorForCategory(category: String): androidx.compose.ui.graphics.Color = when (category) {
    AppCategoryType.SOCIAL_MEDIA -> AccentLavender
    AppCategoryType.ENTERTAINMENT -> PrimaryGreen
    AppCategoryType.PRODUCTIVITY -> PrimaryDark
    AppCategoryType.GAMES -> TertiaryTan
    else -> SecondarySage
}

/** Ported visual layout from the teammate's ReportsScreen `CategoryRow` — real data behind
 *  it here (see [ReportsViewModel.buildCategoryBreakdown]), not their static mock. */
@Composable
private fun CategoryRow(category: CategoryUsage) {
    val color = colorForCategory(category.category)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = category.emoji, fontSize = 16.sp)
                Text(
                    text = category.label,
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            }
            Text(
                text = "${formatMinutes((category.durationMs / 60_000L).toInt())} · ${category.percent}%",
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
                    .fillMaxWidth((category.percent / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(50))
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
