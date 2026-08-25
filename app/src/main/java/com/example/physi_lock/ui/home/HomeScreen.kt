package com.example.physi_lock.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.physi_lock.ui.components.CircularProgressRing
import com.example.physi_lock.ui.components.NotificationsOverlay
import com.example.physi_lock.ui.components.NotificationsViewModel
import com.example.physi_lock.ui.components.toEntry
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage
import com.example.physi_lock.ui.theme.SoftSand
import com.example.physi_lock.ui.theme.TertiaryTan
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

private val PrimaryDark = DeepOlive
private val PrimaryGreen = SageAccent
private val AccentLavender = Orchid
private val AuthTabsBackground = SoftSand

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    notificationsViewModel: NotificationsViewModel = viewModel(),
    displayName: String = "Alex",
    onManageAppLock: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToMove: () -> Unit = {},
    onNavigateToReflection: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationLogs by notificationsViewModel.notifications.collectAsState()
    var showNotifications by remember { mutableStateOf(false) }
    val todayMinutes by homeViewModel.todayScreenTimeMinutes.collectAsState(initial = 0)
    val dailyLimitMinutes by homeViewModel.dailyLimitMinutes.collectAsState(initial = 480)
    val riskLevel by homeViewModel.riskLevel.collectAsState(initial = "Moderate")
    val riskScorePercent by homeViewModel.riskScorePercent.collectAsState(initial = 0.5f)
    val appUsageToday by homeViewModel.appUsageToday.collectAsState(initial = emptyList())
    val predictiveOveruse by homeViewModel.predictiveOveruse.collectAsState(initial = null)
    val doomscrollAlert by homeViewModel.doomscrollAlert.collectAsState(initial = null)
    val hasReflectedToday by homeViewModel.hasReflectedToday.collectAsState(initial = false)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.refreshOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftSand)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 15.dp, vertical = 12.dp)
    ) {
        HomeHeader(
            displayName = displayName,
            unreadNotificationCount = notificationLogs.count { !it.isRead },
            onNotificationsClick = { showNotifications = true }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ScreenTimeCard(todayMinutes = todayMinutes, dailyLimitMinutes = dailyLimitMinutes)
        predictiveOveruse?.let { prediction ->
            Spacer(modifier = Modifier.height(12.dp))
            PredictiveOveruseBanner(prediction = prediction, onFocusClick = onNavigateToFocus)
        }
        Spacer(modifier = Modifier.height(12.dp))
        RiskAndActionsRow(
            riskLevel = riskLevel,
            riskScorePercent = riskScorePercent,
            onManageAppLock = onManageAppLock,
            onNavigateToFocus = onNavigateToFocus
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppUsageCard(appUsageToday = appUsageToday)
        doomscrollAlert?.let { alert ->
            Spacer(modifier = Modifier.height(12.dp))
            DoomscrollAlertBanner(alert = alert, onClick = onNavigateToMove)
        }
        Spacer(modifier = Modifier.height(12.dp))
        GoalsRow(
            todayMinutes = todayMinutes,
            dailyLimitMinutes = dailyLimitMinutes,
            onUsageGoalsClick = onNavigateToGoals,
            hasReflectedToday = hasReflectedToday,
            onReflectionClick = onNavigateToReflection
        )
    }

    if (showNotifications) {
        NotificationsOverlay(
            notifications = notificationLogs.map { it.toEntry() },
            onDismiss = { showNotifications = false },
            onMarkAllRead = { notificationsViewModel.markAllRead() }
        )
    }
    }
}

@Composable
private fun HomeHeader(
    displayName: String,
    unreadNotificationCount: Int = 0,
    onNotificationsClick: () -> Unit = {}
) {
    val dayLabel = remember {
        Calendar.getInstance().time.let {
            java.text.SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(it).uppercase(Locale.getDefault())
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dayLabel,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Good morning, $displayName",
                fontFamily = Nunito,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(AuthTabsBackground, RoundedCornerShape(12.dp))
                .border(1.dp, PrimaryDark.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                .clickable(onClick = onNotificationsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = PrimaryDark,
                modifier = Modifier.size(18.dp)
            )
            if (unreadNotificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .size(8.dp)
                        .background(Orchid, RoundedCornerShape(50))
                )
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
private fun ScreenTimeCard(todayMinutes: Int, dailyLimitMinutes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PrimaryDark),
        shape = RoundedCornerShape(24.dp)
    ) {
        val progress = if (dailyLimitMinutes > 0) {
            (todayMinutes.toFloat() / dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TODAY'S SCREEN TIME",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatMinutes(todayMinutes),
                    fontFamily = Nunito,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BackgroundLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "of ${formatMinutes(dailyLimitMinutes)} daily limit",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    color = SecondarySage
                )
            }
            CircularProgressRing(
                progress = progress,
                trackColor = BackgroundLight.copy(alpha = 0.15f),
                progressColor = SecondarySage,
                ringSize = 84.dp,
                strokeWidth = 9.dp
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontFamily = Nunito,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BackgroundLight
                )
            }
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

/** Ported from the teammate's DashboardScreen "Predictive Overuse" banner — real data
 *  (see [PredictiveOveruseUi] kdoc), only ever rendered when a real prediction exists. */
@Composable
private fun PredictiveOveruseBanner(prediction: PredictiveOveruseUi, onFocusClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentLavender.copy(alpha = 0.09f), RoundedCornerShape(15.dp))
            .border(1.dp, AccentLavender.copy(alpha = 0.40f), RoundedCornerShape(15.dp))
            .padding(horizontal = 15.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = null,
            tint = AccentLavender,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Predictive Overuse",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Box(
                    modifier = Modifier
                        .background(AccentLavender.copy(alpha = 0.20f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildAnnotatedString {
                    append("The model predicts excessive use around ")
                    withStyle(SpanStyle(color = AccentLavender, fontWeight = FontWeight.Bold)) {
                        append(formatHour(prediction.hour))
                    }
                    append(" (~${prediction.predictedMinutes.toInt()} min that hour). Consider a focus session now.")
                },
                fontFamily = Nunito,
                fontSize = 13.sp,
                color = DeepOlive
            )
        }
        Box(
            modifier = Modifier
                .background(AccentLavender, RoundedCornerShape(19.dp))
                .clickable(onClick = onFocusClick)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Focus",
                fontFamily = Nunito,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BackgroundLight
            )
        }
    }
}

/** Ported from the teammate's DashboardScreen "Doomscrolling detected" banner — real data
 *  (see [DoomscrollAlertUi] kdoc), only ever rendered while the alert is still fresh. */
@Composable
private fun DoomscrollAlertBanner(alert: DoomscrollAlertUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentLavender.copy(alpha = 0.09f), RoundedCornerShape(15.dp))
            .border(1.dp, AccentLavender.copy(alpha = 0.27f), RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
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
                text = "Doomscrolling detected on ${alert.appName}",
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )
            Text(
                text = if (alert.minutesAgo <= 0) "Just now · Tap to intervene" else "${alert.minutesAgo} min ago · Tap to intervene",
                fontFamily = Nunito,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
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
private fun RiskAndActionsRow(
    riskLevel: String,
    riskScorePercent: Float,
    onManageAppLock: () -> Unit,
    onNavigateToFocus: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = AuthTabsBackground),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RISK LEVEL",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = PrimaryGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressRing(
                    progress = riskScorePercent,
                    trackColor = TertiaryTan,
                    progressColor = PrimaryDark,
                    ringSize = 60.dp,
                    strokeWidth = 8.dp
                ) {
                    Text(
                        text = riskLevel,
                        fontFamily = Nunito,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryDark
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AccentLavender, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Behavioral risk score",
                        fontFamily = Nunito,
                        fontSize = 12.sp,
                        color = DeepOlive
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(
                icon = Icons.Default.Bedtime,
                title = "Focus Mode",
                subtitle = "Block distracting apps",
                onClick = onNavigateToFocus
            )
            ActionTile(
                icon = Icons.Default.Lock,
                title = "Lock Apps",
                subtitle = "Manage restrictions",
                onClick = onManageAppLock
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryDark, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = BackgroundLight, modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BackgroundLight
            )
            Text(
                text = subtitle,
                fontFamily = Nunito,
                fontSize = 12.sp,
                color = SecondarySage
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = BackgroundLight, modifier = Modifier.size(16.dp))
    }
}

/** Ported from the teammate's sprint-2-ui-navigation branch DashboardScreen's "App Usage
 *  Today" card — general top-apps-by-usage rather than locked-apps-only (that filtered
 *  version was redundant with the "Lock Apps" tile above, which already opens App Lock
 *  Rules directly, so its own "Manage Locks" CTA was dropped). */
@Composable
private fun AppUsageCard(appUsageToday: List<com.example.physi_lock.data.AppUsageTotal>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthTabsBackground, RoundedCornerShape(24.dp))
            .border(1.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(15.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "App Usage Today",
                fontFamily = Nunito,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryDark
            )
            Text(
                text = "${appUsageToday.size} apps",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = PrimaryGreen
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (appUsageToday.isEmpty()) {
            Text(
                text = "No app usage recorded yet today.",
                fontFamily = Nunito,
                fontSize = 13.sp,
                color = DeepOlive
            )
        } else {
            appUsageToday.forEach { item ->
                AppUsageRow(
                    appName = item.appName,
                    durationMs = item.totalDurationMs,
                    isOverLimit = item.totalDurationMs > TimeUnit.HOURS.toMillis(3)
                )
            }
        }
    }
}

@Composable
private fun AppUsageRow(
    appName: String,
    durationMs: Long,
    isOverLimit: Boolean
) {
    val progress = (durationMs.toFloat() / TimeUnit.HOURS.toMillis(3).toFloat()).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = if (isOverLimit) AccentLavender else PrimaryGreen,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = appName,
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                if (isOverLimit) {
                    Box(
                        modifier = Modifier
                            .background(AccentLavender.copy(alpha = 0.13f), RoundedCornerShape(3.75.dp))
                            .padding(horizontal = 5.63.dp, vertical = 1.88.dp)
                    ) {
                        Text(
                            text = "OVER",
                            fontFamily = Nunito,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentLavender
                        )
                    }
                }
            }
            Text(
                text = "${formatMinutes(TimeUnit.MILLISECONDS.toMinutes(durationMs).toInt())} today",
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
                    .fillMaxSize()
                    .background(if (isOverLimit) AccentLavender else PrimaryGreen, RoundedCornerShape(50))
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

/** Ported from the teammate's sprint-2-ui-navigation branch (`DashboardScreen.kt`'s `GoalsRow`/`GoalCard`).
 *  "Daily Reflection" was a non-clickable "Coming soon" placeholder until Module 5 shipped
 *  (2026-08-25, see `ui/reflection/ReflectionScreen.kt`) — now real and clickable, with a
 *  subtitle reflecting whether today's entry actually exists. */
@Composable
private fun GoalsRow(
    todayMinutes: Int,
    dailyLimitMinutes: Int,
    onUsageGoalsClick: () -> Unit,
    hasReflectedToday: Boolean,
    onReflectionClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.25.dp)) {
        GoalCard(
            modifier = Modifier.weight(1f),
            iconBackground = PrimaryGreen.copy(alpha = 0.13f),
            icon = Icons.Default.TrackChanges,
            iconTint = PrimaryGreen,
            title = "Usage Goals",
            subtitle = "${formatMinutes(todayMinutes)} / ${formatMinutes(dailyLimitMinutes)} today",
            onClick = onUsageGoalsClick
        )
        GoalCard(
            modifier = Modifier.weight(1f),
            iconBackground = AccentLavender.copy(alpha = 0.13f),
            icon = Icons.AutoMirrored.Filled.MenuBook,
            iconTint = AccentLavender,
            title = "Daily Reflection",
            subtitle = if (hasReflectedToday) "Answered today" else "Tap to reflect",
            onClick = onReflectionClick
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
            fontFamily = Nunito,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 21.sp,
            color = PrimaryDark
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontFamily = Nunito,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp,
            color = MutedText
        )
    }
}
