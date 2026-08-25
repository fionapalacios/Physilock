package com.example.physi_lock.ui.goals

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.data.AppCategoryType
import com.example.physi_lock.ui.components.AuthTabsBackground
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage
import com.example.physi_lock.ui.theme.TertiaryTan
import java.util.Locale
import kotlin.math.abs

/**
 * Ported from the teammate's sprint-2-ui-navigation branch. As of 2026-08-25, every "current
 * usage" number is real (via [UsageGoalsViewModel] — today's usage bucketed by each app's
 * Admin-curated category, same approach as ReportsViewModel's Category Breakdown card), and
 * every goal *target* is now real and persisted too — total daily limit and the weekly goal
 * live on UserConfiguration, per-category goals (Social Media/Entertainment/Gaming) live in
 * the new CategoryGoal table, all via [UsageGoalsViewModel]. A category with no saved target
 * yet falls back to [CategoryGoalItem.initialGoalHours], the same value the ported UI
 * originally hardcoded as its slider's starting position.
 */
private data class CategoryGoalItem(
    val emoji: String,
    val name: String,
    val categoryType: String?,
    val currentHours: Float,
    val initialGoalHours: Float,
    val maxRange: Float,
    val presets: List<Float>,
    val accentColor: Color
)

private val totalScreenTimeGoalTemplate = CategoryGoalItem(
    emoji = "📱",
    name = "Total Daily Screen Time",
    categoryType = null,
    currentHours = 0f,
    initialGoalHours = 5f,
    maxRange = 8f,
    presets = listOf(0.5f, 1f, 1.5f, 2f, 2.5f),
    accentColor = DeepOlive
)

private val categoryGoalTemplates = listOf(
    CategoryGoalItem(
        emoji = "💬",
        name = "Social Media",
        categoryType = AppCategoryType.SOCIAL_MEDIA,
        currentHours = 0f,
        initialGoalHours = 2f,
        maxRange = 4f,
        presets = listOf(0.5f, 1f, 1.5f, 2f, 2.5f),
        accentColor = Orchid
    ),
    CategoryGoalItem(
        emoji = "🎬",
        name = "Entertainment",
        categoryType = AppCategoryType.ENTERTAINMENT,
        currentHours = 0f,
        initialGoalHours = 1.5f,
        maxRange = 3f,
        presets = listOf(0.5f, 1f, 1.5f, 2f, 2.5f),
        accentColor = SageAccent
    ),
    CategoryGoalItem(
        emoji = "🎮",
        name = "Gaming",
        categoryType = AppCategoryType.GAMES,
        currentHours = 0f,
        initialGoalHours = 0.5f,
        maxRange = 2f,
        presets = listOf(0.5f, 1f, 1.5f, 2f, 2.5f),
        accentColor = MutedText
    )
)

private fun formatHours(value: Float): String {
    val text = String.format(Locale.US, "%.2f", value)
    return text.trimEnd('0').trimEnd('.')
}

@Composable
fun UsageGoalsScreen(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    usageGoalsViewModel: UsageGoalsViewModel = viewModel()
) {
    val weeklyGoal by usageGoalsViewModel.weeklyGoalHours.collectAsState()
    val weeklyCurrent by usageGoalsViewModel.weeklyTotalHours.collectAsState()
    val dailyLimitHours by usageGoalsViewModel.dailyLimitHours.collectAsState()
    val todayTotalHours by usageGoalsViewModel.todayTotalHours.collectAsState()
    val todayCategoryHours by usageGoalsViewModel.todayCategoryHours.collectAsState()
    val categoryGoalHours by usageGoalsViewModel.categoryGoalHours.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(BackgroundLight)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.79.dp, DeepOlive.copy(alpha = 0.08f))
                .padding(horizontal = 15.dp, vertical = 11.25.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.25.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DeepOlive,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Usage Goals",
                modifier = Modifier.weight(1f),
                fontFamily = Nunito,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 18.7.sp,
                color = DeepOlive
            )
            Box(
                modifier = Modifier
                    .background(DeepOlive, RoundedCornerShape(19.dp))
                    .clickable(onClick = onSaveClick)
                    .padding(horizontal = 11.25.dp, vertical = 5.63.dp)
            ) {
                Text(
                    text = "Save Goals",
                    fontFamily = Nunito,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp,
                    color = BackgroundLight
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 15.dp)
                .padding(top = 15.dp, bottom = 30.dp)
        ) {
            WeeklyGoalCard(
                current = weeklyCurrent,
                goal = weeklyGoal,
                onGoalChange = { usageGoalsViewModel.setWeeklyGoalHours(it) }
            )

            Spacer(modifier = Modifier.height(18.75.dp))

            Text(
                text = "DAILY LIMITS BY CATEGORY",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                color = SageAccent
            )

            Spacer(modifier = Modifier.height(11.25.dp))

            Column(verticalArrangement = Arrangement.spacedBy(11.25.dp)) {
                CategoryGoalCard(
                    item = totalScreenTimeGoalTemplate.copy(currentHours = todayTotalHours),
                    goal = dailyLimitHours,
                    onGoalChange = { usageGoalsViewModel.setDailyLimitHours(it) }
                )
                categoryGoalTemplates.forEach { template ->
                    val current = todayCategoryHours[template.categoryType] ?: 0f
                    val goal = categoryGoalHours[template.categoryType] ?: template.initialGoalHours
                    CategoryGoalCard(
                        item = template.copy(currentHours = current),
                        goal = goal,
                        onGoalChange = { usageGoalsViewModel.setCategoryGoalHours(template.categoryType!!, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyGoalCard(
    current: Float,
    goal: Float,
    onGoalChange: (Float) -> Unit
) {
    val diff = current - goal
    val isOver = diff > 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepOlive, RoundedCornerShape(22.5.dp))
            .padding(18.75.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.5.dp)) {
            Icon(
                imageVector = Icons.Default.TrackChanges,
                contentDescription = null,
                tint = SageAccent,
                modifier = Modifier.width(14.dp).height(14.dp)
            )
            Text(
                text = "WEEKLY SCREEN TIME GOAL",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                color = SageAccent
            )
        }

        Spacer(modifier = Modifier.height(3.75.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${formatHours(current)}h",
                    fontFamily = Nunito,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 28.sp,
                    color = BackgroundLight
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "/ ${formatHours(goal)}h goal",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 21.sp,
                    color = SecondarySage
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        if (isOver) Orchid.copy(alpha = 0.20f) else SageAccent.copy(alpha = 0.20f),
                        RoundedCornerShape(19.dp)
                    )
                    .padding(horizontal = 7.5.dp, vertical = 3.75.dp)
            ) {
                Text(
                    text = if (isOver) "+${formatHours(diff)}h OVER" else "${formatHours(-diff)}h left",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp,
                    color = if (isOver) Orchid else SageAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(11.25.dp))

        val fraction = (current / (goal * 1.4f)).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(if (isOver) Orchid else SageAccent, RoundedCornerShape(50))
            )
        }

        Spacer(modifier = Modifier.height(11.25.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = SecondarySage,
                modifier = Modifier.width(12.dp).height(12.dp)
            )
            Spacer(modifier = Modifier.width(7.5.dp))
            Text(
                text = "Adjust weekly goal:",
                fontFamily = Nunito,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 19.5.sp,
                color = SecondarySage
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(5.63.dp)) {
                listOf(28f, 35f, 42f).forEach { preset ->
                    val active = abs(preset - goal) < 0.01f
                    Box(
                        modifier = Modifier
                            .background(
                                if (active) SageAccent else Color.White.copy(alpha = 0.10f),
                                RoundedCornerShape(15.dp)
                            )
                            .clickable { onGoalChange(preset) }
                            .padding(horizontal = 7.5.dp, vertical = 3.75.dp)
                    ) {
                        Text(
                            text = "${preset.toInt()}h",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            color = BackgroundLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryGoalCard(item: CategoryGoalItem, goal: Float, onGoalChange: (Float) -> Unit) {
    val isOver = item.currentHours > goal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthTabsBackground, RoundedCornerShape(15.dp))
            .border(0.79.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.5.dp)) {
                Text(text = item.emoji, fontSize = 18.sp)
                Text(
                    text = item.name,
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    color = DeepOlive
                )
            }
            if (isOver) {
                Box(
                    modifier = Modifier
                        .background(Orchid.copy(alpha = 0.13f), RoundedCornerShape(50))
                        .padding(horizontal = 7.5.dp, vertical = 1.88.dp)
                ) {
                    Text(
                        text = "OVER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        color = Orchid
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = goal,
            onValueChange = onGoalChange,
            valueRange = 0f..item.maxRange,
            colors = SliderDefaults.colors(
                thumbColor = item.accentColor,
                activeTrackColor = Orchid.copy(alpha = 0.4f),
                inactiveTrackColor = TertiaryTan
            )
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row {
                Text(
                    text = "Current: ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 19.5.sp,
                    color = MutedText
                )
                Text(
                    text = "${formatHours(item.currentHours)}h",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.5.sp,
                    color = if (isOver) Orchid else SageAccent
                )
            }
            Row {
                Text(
                    text = "Goal: ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 19.5.sp,
                    color = MutedText
                )
                Text(
                    text = "${formatHours(goal)}h",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.5.sp,
                    color = item.accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(11.25.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Adjust goal:",
                fontFamily = Nunito,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 19.5.sp,
                color = MutedText
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.5.dp)) {
                item.presets.forEach { preset ->
                    val active = abs(preset - goal) < 0.01f
                    Box(
                        modifier = Modifier
                            .background(
                                if (active) item.accentColor else DeepOlive.copy(alpha = 0.06f),
                                RoundedCornerShape(15.dp)
                            )
                            .clickable { onGoalChange(preset) }
                            .padding(horizontal = 7.5.dp, vertical = 3.75.dp)
                    ) {
                        Text(
                            text = "${formatHours(preset)}h",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            color = if (active) BackgroundLight else MutedText
                        )
                    }
                }
            }
        }
    }
}

