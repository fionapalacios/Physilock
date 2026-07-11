package com.prototype.physi_lock.ui.screens.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.ui.components.AuthTabsBackground
import com.prototype.physi_lock.ui.theme.AccentLavender
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen
import com.prototype.physi_lock.ui.theme.SecondarySage
import com.prototype.physi_lock.ui.theme.TertiaryTan

private enum class ReportPeriod { WEEK, MONTH }

private data class ReportStat(val label: String, val value: String, val delta: String)
private data class CategoryUsage(val emoji: String, val name: String, val timeLabel: String, val percent: Int, val color: Color)
private data class InsightItem(val icon: ImageVector, val tint: Color, val background: Color, val title: String, val description: String)
private data class DailyPoint(val label: String, val hours: Float, val color: Color)
private data class WeeklyPoint(val label: String, val hours: Float)

private data class ReportData(
    val statLeft: ReportStat,
    val statRight: ReportStat,
    val chartTitle: String,
    val chartUnit: String,
    val categories: List<CategoryUsage>,
    val insights: List<InsightItem>
)

private val weekDaily = listOf(
    DailyPoint("Mon", 4.3f, PrimaryGreen),
    DailyPoint("Tue", 7.0f, PrimaryGreen),
    DailyPoint("Wed", 5.6f, PrimaryDark),
    DailyPoint("Thu", 7.3f, AccentLavender),
    DailyPoint("Fri", 8.4f, AccentLavender),
    DailyPoint("Sat", 4.0f, PrimaryGreen),
    DailyPoint("Sun", 5.4f, PrimaryGreen)
)

private val monthWeekly = listOf(
    WeeklyPoint("W1", 38.7f),
    WeeklyPoint("W2", 42.1f),
    WeeklyPoint("W3", 35.7f),
    WeeklyPoint("W4", 41.0f)
)

private val weekData = ReportData(
    statLeft = ReportStat("Avg Daily", "5h 53m", "−18m vs last week"),
    statRight = ReportStat("Total Week", "41.2h", "↓ improving"),
    chartTitle = "Daily Usage",
    chartUnit = "hrs / day",
    categories = listOf(
        CategoryUsage("📱", "Social Media", "3h 14m · 43%", 43, AccentLavender),
        CategoryUsage("🎬", "Entertainment", "1h 52m · 25%", 25, PrimaryGreen),
        CategoryUsage("📋", "Productivity", "1h 7m · 15%", 15, DeepOlive),
        CategoryUsage("💬", "Communication", "48m · 11%", 11, SecondarySage),
        CategoryUsage("🎮", "Gaming", "34m · 7%", 7, TertiaryTan)
    ),
    insights = listOf(
        InsightItem(Icons.Default.Visibility, AccentLavender, AccentLavender.copy(alpha = 0.07f), "Doomscrolling Detected", "3 sessions over 20 min of continuous scrolling today"),
        InsightItem(Icons.AutoMirrored.Filled.TrendingDown, PrimaryGreen, SecondarySage.copy(alpha = 0.13f), "Weekend Dip", "Sat usage was 54% below your weekday average"),
        InsightItem(Icons.AutoMirrored.Filled.TrendingUp, AccentLavender, AccentLavender.copy(alpha = 0.07f), "Friday Spike", "8.4h on Friday — highest this week"),
        InsightItem(Icons.Default.Bedtime, AccentLavender, AccentLavender.copy(alpha = 0.07f), "Bedtime Usage", "Screen use after 11 PM detected on 4 nights")
    )
)

private val monthData = ReportData(
    statLeft = ReportStat("Avg Weekly", "39.4h", "−3.2h vs May"),
    statRight = ReportStat("Total Month", "157.4h", "↓ on track"),
    chartTitle = "Weekly Usage",
    chartUnit = "hrs / week",
    categories = listOf(
        CategoryUsage("📱", "Social Media", "13h 40m · 42%", 42, AccentLavender),
        CategoryUsage("🎬", "Entertainment", "8h 10m · 25%", 25, PrimaryGreen),
        CategoryUsage("📋", "Productivity", "5h 10m · 16%", 16, DeepOlive),
        CategoryUsage("💬", "Communication", "3h 25m · 10%", 10, SecondarySage),
        CategoryUsage("🎮", "Gaming", "2h 25m · 7%", 7, TertiaryTan)
    ),
    insights = listOf(
        InsightItem(Icons.AutoMirrored.Filled.TrendingDown, PrimaryGreen, SecondarySage.copy(alpha = 0.13f), "Month-over-Month Drop", "Total usage down 3.2h compared to May"),
        InsightItem(Icons.AutoMirrored.Filled.TrendingUp, AccentLavender, AccentLavender.copy(alpha = 0.07f), "Week 2 Peak", "42.1h in Week 2 — your highest this month"),
        InsightItem(Icons.Default.PieChart, AccentLavender, AccentLavender.copy(alpha = 0.07f), "Social Media Dominance", "Social apps account for 42% of total usage"),
        InsightItem(Icons.Default.EmojiEvents, PrimaryGreen, SecondarySage.copy(alpha = 0.13f), "Best Week: W3", "Week 3 at 35.7h — closest to your 35h goal")
    )
)

@Composable
fun ReportsScreen(modifier: Modifier = Modifier) {
    var period by remember { mutableStateOf(ReportPeriod.WEEK) }
    val data = if (period == ReportPeriod.WEEK) weekData else monthData

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 15.dp)
            .padding(top = 7.5.dp, bottom = 24.dp)
    ) {
        Text(
            text = "ANALYTICS",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.5.sp,
            color = PrimaryGreen
        )
        Text(
            text = "Usage Reports",
            fontFamily = NunitoFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 22.sp,
            color = PrimaryDark
        )

        Spacer(modifier = Modifier.height(18.75.dp))

        PeriodTabs(selected = period, onSelected = { period = it })

        Spacer(modifier = Modifier.height(18.75.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.25.dp)) {
            StatCard(stat = data.statLeft, modifier = Modifier.weight(1f))
            StatCard(stat = data.statRight, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AuthTabsBackground, RoundedCornerShape(22.5.dp))
                .border(0.79.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(22.5.dp))
                .padding(15.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = data.chartTitle,
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 21.sp,
                    color = PrimaryDark
                )
                Text(
                    text = data.chartUnit,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = PrimaryGreen
                )
            }
            Spacer(modifier = Modifier.height(11.25.dp))
            if (period == ReportPeriod.WEEK) {
                DailyUsageChart()
                Spacer(modifier = Modifier.height(3.75.dp))
                DailyUsageLegend()
            } else {
                WeeklyUsageChart()
                Spacer(modifier = Modifier.height(3.75.dp))
                WeeklyUsageLegend()
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AuthTabsBackground, RoundedCornerShape(22.5.dp))
                .border(0.79.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(22.5.dp))
                .padding(15.dp)
        ) {
            Text(
                text = "Category Breakdown",
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 21.sp,
                color = PrimaryDark
            )
            Spacer(modifier = Modifier.height(15.dp))
            Column(verticalArrangement = Arrangement.spacedBy(11.25.dp)) {
                data.categories.forEach { category -> CategoryRow(category) }
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AuthTabsBackground, RoundedCornerShape(22.5.dp))
                .border(0.79.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(22.5.dp))
                .padding(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.5.dp)) {
                Text(
                    text = "AI Insights",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 21.sp,
                    color = PrimaryDark
                )
                Box(
                    modifier = Modifier
                        .background(AccentLavender.copy(alpha = 0.13f), RoundedCornerShape(3.75.dp))
                        .padding(horizontal = 5.63.dp, vertical = 1.88.dp)
                ) {
                    Text(
                        text = "AI-POWERED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        color = AccentLavender
                    )
                }
            }
            Spacer(modifier = Modifier.height(15.dp))
            Column(verticalArrangement = Arrangement.spacedBy(11.25.dp)) {
                data.insights.forEach { insight -> InsightRow(insight) }
            }
        }
    }
}

@Composable
private fun PeriodTabs(selected: ReportPeriod, onSelected: (ReportPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthTabsBackground, RoundedCornerShape(19.dp))
            .padding(3.75.dp)
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
                    .padding(vertical = 7.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (entry == ReportPeriod.WEEK) "This Week" else "This Month",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    color = if (isActive) BackgroundLight else DeepOlive
                )
            }
        }
    }
}

@Composable
private fun StatCard(stat: ReportStat, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(AuthTabsBackground, RoundedCornerShape(15.dp))
            .border(0.79.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
            .padding(11.25.dp)
    ) {
        Text(
            text = stat.label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp,
            color = PrimaryGreen
        )
        Spacer(modifier = Modifier.height(3.75.dp))
        Text(
            text = stat.value,
            fontFamily = NunitoFontFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 18.7.sp,
            color = PrimaryDark
        )
        Spacer(modifier = Modifier.height(1.88.dp))
        Text(
            text = stat.delta,
            fontFamily = NunitoFontFamily,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = DeepOlive
        )
    }
}

private val chartHeight = 104.dp

@Composable
private fun DailyUsageChart() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.height(chartHeight).width(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("9", "6", "3", "0").forEach { label ->
                Text(
                    text = label,
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
                weekDaily.forEach { point ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .fillMaxHeight((point.hours / 9f).coerceIn(0f, 1f))
                                .background(point.color.copy(alpha = 0.78f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                weekDaily.forEach { point ->
                    Text(
                        text = point.label,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
    Row(horizontalArrangement = Arrangement.spacedBy(15.dp), modifier = Modifier.padding(top = 3.75.dp)) {
        LegendDot(color = PrimaryDark, label = "Today")
        LegendDot(color = AccentLavender, label = "Over limit")
        LegendDot(color = PrimaryGreen, label = "Normal")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.75.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = DeepOlive
        )
    }
}

private const val WEEKLY_MIN_HOURS = 30f
private const val WEEKLY_MAX_HOURS = 50f
private const val WEEKLY_GOAL_HOURS = 35f

@Composable
private fun WeeklyUsageChart() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.height(chartHeight).width(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "50", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = PrimaryGreen)
            Text(text = "30", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = PrimaryGreen)
        }
        Spacer(modifier = Modifier.width(7.dp))
        Column(modifier = Modifier.weight(1f)) {
            val primaryDarkColor = PrimaryDark
            val primaryGreenColor = PrimaryGreen
            val backgroundColor = BackgroundLight
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                val range = WEEKLY_MAX_HOURS - WEEKLY_MIN_HOURS
                fun yFor(hours: Float): Float = size.height * (1f - ((hours - WEEKLY_MIN_HOURS) / range).coerceIn(0f, 1f))
                val stepX = size.width / (monthWeekly.size + 1)
                val points = monthWeekly.mapIndexed { index, point ->
                    Offset(stepX * (index + 1), yFor(point.hours))
                }

                val goalY = yFor(WEEKLY_GOAL_HOURS)
                drawLine(
                    color = primaryDarkColor,
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )

                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = primaryGreenColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                points.forEach { offset ->
                    drawCircle(color = primaryGreenColor, radius = 5.dp.toPx(), center = offset)
                    drawCircle(
                        color = primaryDarkColor,
                        radius = 5.dp.toPx(),
                        center = offset,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(color = backgroundColor, radius = 1.5.dp.toPx(), center = offset)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                monthWeekly.forEach { point ->
                    Text(
                        text = point.label,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
private fun WeeklyUsageLegend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.63.dp), modifier = Modifier.padding(top = 3.75.dp)) {
        Box(modifier = Modifier.width(16.dp).height(2.dp).background(PrimaryDark, RoundedCornerShape(1.dp)))
        Text(
            text = "35h goal line",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = DeepOlive
        )
    }
}

@Composable
private fun CategoryRow(category: CategoryUsage) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.5.dp)) {
                Text(text = category.emoji, fontSize = 14.sp)
                Text(
                    text = category.name,
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    color = PrimaryDark
                )
            }
            Text(
                text = category.timeLabel,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.5.sp,
                color = DeepOlive
            )
        }
        Spacer(modifier = Modifier.height(3.75.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(TertiaryTan, RoundedCornerShape(50))
                .border(0.79.dp, PrimaryDark.copy(alpha = 0.20f), RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(category.percent / 100f)
                    .fillMaxHeight()
                    .background(category.color, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun InsightRow(insight: InsightItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(insight.background, RoundedCornerShape(19.dp))
            .padding(horizontal = 11.25.dp, vertical = 9.38.dp),
        horizontalArrangement = Arrangement.spacedBy(11.25.dp)
    ) {
        Icon(
            imageVector = insight.icon,
            contentDescription = null,
            tint = insight.tint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(15.dp)
        )
        Column {
            Text(
                text = insight.title,
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp,
                color = PrimaryDark
            )
            Text(
                text = insight.description,
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                lineHeight = 19.5.sp,
                color = DeepOlive
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ReportsScreenPreview() {
    PhysiLockTheme {
        ReportsScreen()
    }
}
