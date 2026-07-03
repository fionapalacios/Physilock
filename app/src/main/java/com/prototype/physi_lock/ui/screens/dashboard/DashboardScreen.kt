package com.prototype.physi_lock.ui.screens.dashboard

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.ui.components.AuthTabsBackground
import com.prototype.physi_lock.ui.components.BottomNavBar
import com.prototype.physi_lock.ui.components.BottomNavItem
import com.prototype.physi_lock.ui.components.CircularProgressRing
import com.prototype.physi_lock.ui.components.NotificationEntry
import com.prototype.physi_lock.ui.components.NotificationsOverlay
import com.prototype.physi_lock.ui.theme.AccentLavender
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen
import com.prototype.physi_lock.ui.theme.SecondarySage
import com.prototype.physi_lock.ui.theme.TertiaryTan
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val StreakOrange = Color(0xFFE8854A)

private val initialNotifications = listOf(
    NotificationEntry(
        icon = Icons.Default.PhoneAndroid,
        accentColor = AccentLavender,
        title = "Instagram limit exceeded",
        description = "You've used 14 min over your 2h daily limit.",
        timestamp = "2 min ago",
        isUnread = true
    ),
    NotificationEntry(
        icon = Icons.Default.PhoneAndroid,
        accentColor = AccentLavender,
        title = "Doomscrolling detected",
        description = "22 minutes of continuous scrolling on TikTok.",
        timestamp = "14 min ago",
        isUnread = true
    ),
    NotificationEntry(
        icon = Icons.Default.Spa,
        accentColor = PrimaryGreen,
        title = "Break reminder",
        description = "You've been online 47 min — stretch or take a walk 🌿",
        timestamp = "47 min ago",
        isUnread = true
    ),
    NotificationEntry(
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        accentColor = PrimaryGreen,
        title = "Daily step goal reached",
        description = "Great work! You've hit 8,200 steps today.",
        timestamp = "2h ago",
        isUnread = false
    ),
    NotificationEntry(
        icon = Icons.Default.LocalFireDepartment,
        accentColor = StreakOrange,
        title = "3-day streak maintained!",
        description = "Keep going — your longest streak is 7 days.",
        timestamp = "3h ago",
        isUnread = false
    ),
    NotificationEntry(
        icon = Icons.Default.Assessment,
        accentColor = PrimaryGreen,
        title = "Weekly report ready",
        description = "Your screen time is down 18 min vs last week.",
        timestamp = "Yesterday",
        isUnread = false
    )
)

/** Formats "today" as e.g. "WED, JUN 17" and refreshes itself right at each midnight rollover. */
@Composable
private fun rememberCurrentDateLabel(): String {
    val formatter = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    var label by remember { mutableStateOf(formatter.format(Calendar.getInstance().time).uppercase(Locale.getDefault())) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            delay((nextMidnight.timeInMillis - now.timeInMillis).coerceAtLeast(1_000L))
            label = formatter.format(Calendar.getInstance().time).uppercase(Locale.getDefault())
        }
    }
    return label
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(BottomNavItem.HOME) }
    var showNotifications by remember { mutableStateOf(false) }
    var showUsageGoals by remember { mutableStateOf(false) }
    val notifications = remember { mutableStateListOf(*initialNotifications.toTypedArray()) }

    var activeChallenge by remember { mutableStateOf<MotionChallenge?>(null) }
    var challengeJustCompleted by remember { mutableStateOf(false) }
    var focusModeActive by remember { mutableStateOf(false) }
    var focusStartMillis by remember { mutableStateOf(0L) }
    var showEndFocusSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
            Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == BottomNavItem.HOME) {
                    HomeContent(
                        onNotificationsClick = { showNotifications = true },
                        onUsageGoalsClick = { showUsageGoals = true },
                        focusModeActive = focusModeActive,
                        onFocusModeClick = {
                            if (focusModeActive) {
                                showEndFocusSheet = true
                            } else {
                                focusModeActive = true
                                focusStartMillis = System.currentTimeMillis()
                            }
                        },
                        onLockAppsClick = {
                            challengeJustCompleted = false
                            activeChallenge = motionChallenges.random()
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "${selectedTab.label} coming soon",
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = PrimaryDark.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            BottomNavBar(selected = selectedTab, onItemSelected = { selectedTab = it })
        }

        if (showNotifications) {
            NotificationsOverlay(
                notifications = notifications,
                onDismiss = { showNotifications = false },
                onMarkAllRead = {
                    for (i in notifications.indices) {
                        notifications[i] = notifications[i].copy(isUnread = false)
                    }
                }
            )
        }

        if (showUsageGoals) {
            UsageGoalsScreen(
                onBackClick = { showUsageGoals = false },
                onSaveClick = { showUsageGoals = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        val challenge = activeChallenge
        if (challenge != null) {
            if (challengeJustCompleted) {
                ChallengeCompleteScreen(
                    unlockedAppName = "Instagram",
                    onOpenApp = {
                        activeChallenge = null
                        challengeJustCompleted = false
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                MotionChallengeScreen(
                    challenge = challenge,
                    onComplete = { challengeJustCompleted = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showEndFocusSheet) {
            EndFocusSessionSheet(
                elapsedSeconds = (System.currentTimeMillis() - focusStartMillis) / 1000,
                onEndSession = {
                    focusModeActive = false
                    showEndFocusSheet = false
                },
                onKeepGoing = { showEndFocusSheet = false }
            )
        }
    }
}

@Composable
private fun HomeContent(
    onNotificationsClick: () -> Unit,
    onUsageGoalsClick: () -> Unit,
    focusModeActive: Boolean,
    onFocusModeClick: () -> Unit,
    onLockAppsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 15.dp)
            .padding(top = 7.5.dp, bottom = 24.dp)
    ) {
        HeaderRow(onNotificationsClick = onNotificationsClick)
        Spacer(modifier = Modifier.height(18.75.dp))
        ScreenTimeCard()
        Spacer(modifier = Modifier.height(15.dp))
        BreakReminderBanner()
        Spacer(modifier = Modifier.height(15.dp))
        PredictiveOveruseBanner()
        Spacer(modifier = Modifier.height(15.dp))
        RiskAndActionsRow(
            focusModeActive = focusModeActive,
            onFocusModeClick = onFocusModeClick,
            onLockAppsClick = onLockAppsClick
        )
        Spacer(modifier = Modifier.height(15.dp))
        AppUsageCard()
        Spacer(modifier = Modifier.height(15.dp))
        DoomscrollingBanner()
        Spacer(modifier = Modifier.height(15.dp))
        GoalsRow(onUsageGoalsClick = onUsageGoalsClick)
    }
}

@Composable
private fun HeaderRow(onNotificationsClick: () -> Unit) {
    val dateLabel = rememberCurrentDateLabel()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = dateLabel,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 19.5.sp,
                color = PrimaryGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Good morning, Alex",
                fontFamily = NunitoFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 22.sp,
                color = PrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(11.25.dp))
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(AuthTabsBackground, RoundedCornerShape(12.dp))
                .border(0.79.dp, PrimaryDark.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                .clickable(onClick = onNotificationsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = PrimaryDark,
                modifier = Modifier.size(17.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 5.dp, end = 5.dp)
                    .size(7.dp)
                    .background(AccentLavender, RoundedCornerShape(50))
                    .border(0.79.dp, BackgroundLight, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun ScreenTimeCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryDark, RoundedCornerShape(22.5.dp))
            .padding(18.75.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TODAY'S SCREEN TIME",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.5.sp,
                    color = PrimaryGreen
                )
                Spacer(modifier = Modifier.height(3.75.dp))
                Text(
                    text = "5h 33m",
                    fontFamily = NunitoFontFamily,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 34.sp,
                    color = BackgroundLight
                )
                Spacer(modifier = Modifier.height(3.75.dp))
                Text(
                    text = "of 7h daily limit",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 21.sp,
                    color = SecondarySage
                )
                Spacer(modifier = Modifier.height(11.25.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.63.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = AccentLavender,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "18 min less than yesterday",
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        color = AccentLavender
                    )
                }
            }
            CircularProgressRing(
                progress = 0.79f,
                trackColor = BackgroundLight.copy(alpha = 0.15f),
                progressColor = SecondarySage,
                ringSize = 100.dp,
                strokeWidth = 10.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "79%",
                        fontFamily = NunitoFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BackgroundLight
                    )
                    Text(
                        text = "used",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = PrimaryGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakReminderBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SecondarySage.copy(alpha = 0.20f), RoundedCornerShape(15.dp))
            .border(0.79.dp, SecondarySage, RoundedCornerShape(15.dp))
            .padding(horizontal = 15.dp, vertical = 11.25.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.25.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Spa,
            contentDescription = null,
            tint = DeepOlive,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = "Break reminder",
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp,
                color = PrimaryDark
            )
            Text(
                text = "You've been online 47 min — take a 5-min walk 🌿",
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                lineHeight = 19.5.sp,
                color = DeepOlive
            )
        }
    }
}

@Composable
private fun PredictiveOveruseBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentLavender.copy(alpha = 0.09f), RoundedCornerShape(15.dp))
            .border(0.79.dp, AccentLavender.copy(alpha = 0.40f), RoundedCornerShape(15.dp))
            .padding(horizontal = 15.dp, vertical = 11.25.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(11.25.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = null,
            tint = AccentLavender,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.63.dp)
            ) {
                Text(
                    text = "Predictive Overuse",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    color = PrimaryDark
                )
                Box(
                    modifier = Modifier
                        .background(AccentLavender.copy(alpha = 0.20f), RoundedCornerShape(3.75.dp))
                        .padding(horizontal = 5.63.dp, vertical = 1.88.dp)
                ) {
                    Text(
                        text = "AI",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentLavender
                    )
                }
            }
            Spacer(modifier = Modifier.height(1.88.dp))
            Text(
                text = buildAnnotatedString {
                    append("At your current pace, you'll hit your 7h limit by ")
                    withStyle(SpanStyle(color = AccentLavender, fontWeight = FontWeight.Bold)) {
                        append("8:22 PM")
                    }
                    append(". Consider a focus session now.")
                },
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                lineHeight = 18.85.sp,
                color = DeepOlive
            )
        }
        Box(
            modifier = Modifier
                .background(AccentLavender, RoundedCornerShape(19.dp))
                .padding(horizontal = 7.5.dp, vertical = 5.63.dp)
        ) {
            Text(
                text = "Focus",
                fontFamily = NunitoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                color = BackgroundLight
            )
        }
    }
}

@Composable
private fun RiskAndActionsRow(
    focusModeActive: Boolean,
    onFocusModeClick: () -> Unit,
    onLockAppsClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.25.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .background(AuthTabsBackground, RoundedCornerShape(15.dp))
                .border(0.79.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
                .padding(15.dp)
        ) {
            Text(
                text = "RISK SCORE",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                color = PrimaryGreen
            )
            Spacer(modifier = Modifier.height(7.5.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressRing(
                    progress = 0.62f,
                    trackColor = TertiaryTan,
                    progressColor = PrimaryDark,
                    ringSize = 60.dp,
                    strokeWidth = 8.dp
                ) {
                    Text(
                        text = "62",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 21.sp,
                        color = PrimaryDark
                    )
                }
            }
            Spacer(modifier = Modifier.height(7.5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.75.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = AccentLavender,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "Moderate risk",
                    fontFamily = NunitoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                    color = DeepOlive
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.5.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(
                        if (focusModeActive) PrimaryDark else SecondarySage,
                        RoundedCornerShape(19.dp)
                    )
                    .clickable(onClick = onFocusModeClick)
                    .padding(horizontal = 11.25.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = if (focusModeActive) BackgroundLight else PrimaryDark,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (focusModeActive) "Focus Active" else "Focus Mode",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    color = if (focusModeActive) BackgroundLight else PrimaryDark
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(PrimaryDark, RoundedCornerShape(19.dp))
                    .clickable(onClick = onLockAppsClick)
                    .padding(horizontal = 11.25.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = BackgroundLight,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Lock Apps",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    color = BackgroundLight
                )
            }
        }
    }
}

private data class AppUsageEntry(
    val emoji: String,
    val name: String,
    val time: String,
    val progress: Float,
    val isOverLimit: Boolean = false
)

private val appUsageEntries = listOf(
    AppUsageEntry("📸", "Instagram", "2h 14m", 1.0f, isOverLimit = true),
    AppUsageEntry("🎵", "TikTok", "1h 47m", 0.887f),
    AppUsageEntry("▶️", "YouTube", "58m", 0.641f),
    AppUsageEntry("𝕏", "Twitter/X", "34m", 0.751f)
)

@Composable
private fun AppUsageCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthTabsBackground, RoundedCornerShape(22.5.dp))
            .border(0.79.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(22.5.dp))
            .padding(15.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "App Usage Today",
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 21.sp,
                color = PrimaryDark
            )
            Text(
                text = "4 apps",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = PrimaryGreen
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
        Column(verticalArrangement = Arrangement.spacedBy(11.25.dp)) {
            appUsageEntries.forEach { entry -> AppUsageRow(entry) }
        }
    }
}

@Composable
private fun AppUsageRow(entry: AppUsageEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.5.dp)
            ) {
                Text(text = entry.emoji, fontSize = 16.sp)
                Text(
                    text = entry.name,
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    color = PrimaryDark
                )
                if (entry.isOverLimit) {
                    Box(
                        modifier = Modifier
                            .background(AccentLavender.copy(alpha = 0.13f), RoundedCornerShape(3.75.dp))
                            .padding(horizontal = 5.63.dp, vertical = 1.88.dp)
                    ) {
                        Text(
                            text = "OVER · Tap to unlock",
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp,
                            color = AccentLavender
                        )
                    }
                }
            }
            Text(
                text = entry.time,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 19.5.sp,
                color = DeepOlive
            )
        }
        Spacer(modifier = Modifier.height(5.63.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(TertiaryTan, RoundedCornerShape(50))
                .border(0.79.dp, PrimaryDark.copy(alpha = 0.20f), RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(entry.progress)
                    .fillMaxHeight()
                    .background(
                        if (entry.isOverLimit) AccentLavender else PrimaryGreen,
                        RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
private fun DoomscrollingBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentLavender.copy(alpha = 0.09f), RoundedCornerShape(15.dp))
            .border(0.79.dp, AccentLavender.copy(alpha = 0.27f), RoundedCornerShape(15.dp))
            .padding(horizontal = 15.dp, vertical = 11.25.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.25.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(AccentLavender.copy(alpha = 0.13f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = AccentLavender,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Doomscrolling detected on TikTok",
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp,
                color = PrimaryDark
            )
            Text(
                text = "23 min continuous session · Tap to intervene",
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.5.sp,
                color = DeepOlive
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AccentLavender,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun GoalsRow(onUsageGoalsClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.25.dp)) {
        GoalCard(
            modifier = Modifier.weight(1f),
            iconBackground = PrimaryGreen.copy(alpha = 0.13f),
            icon = Icons.Default.TrackChanges,
            iconTint = PrimaryGreen,
            title = "Usage Goals",
            subtitle = "5h / 7h today",
            onClick = onUsageGoalsClick
        )
        GoalCard(
            modifier = Modifier.weight(1f),
            iconBackground = AccentLavender.copy(alpha = 0.13f),
            icon = Icons.AutoMirrored.Filled.MenuBook,
            iconTint = AccentLavender,
            title = "Daily Reflection",
            subtitle = "Not done yet 💭",
            onClick = {}
        )
    }
}

@Composable
private fun GoalCard(
    modifier: Modifier,
    iconBackground: Color,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(AuthTabsBackground, RoundedCornerShape(15.dp))
            .border(0.79.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(15.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(iconBackground, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(7.5.dp))
        Text(
            text = title,
            fontFamily = NunitoFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 21.sp,
            color = PrimaryDark
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontFamily = NunitoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp,
            color = DeepOlive
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun DashboardScreenPreview() {
    PhysiLockTheme {
        DashboardScreen()
    }
}
