package com.example.physi_lock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.NotificationLog
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.ErrorRed
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage
import java.util.concurrent.TimeUnit

/** Maps a real logged [NotificationLog] row (see AppMonitorService) to the display entry below. */
fun NotificationLog.toEntry(): NotificationEntry {
    val (icon, color) = when (type) {
        "BREAK_REMINDER" -> Icons.Default.Bedtime to SageAccent
        "OVERUSE_ALERT" -> Icons.Default.WarningAmber to ErrorRed
        "DOOMSCROLL_ALERT" -> Icons.Default.WarningAmber to Orchid
        "EXCESSIVE_USAGE_PREDICTION" -> Icons.Default.TrendingUp to DeepOlive
        "FOCUS_BLOCK" -> Icons.Default.Lock to SecondarySage
        else -> Icons.Default.Notifications to DeepOlive
    }
    return NotificationEntry(
        icon = icon,
        accentColor = color,
        title = title,
        description = description,
        timestamp = formatRelativeTime(timestamp),
        isUnread = !isRead
    )
}

private fun formatRelativeTime(timestamp: Long): String {
    val elapsedMs = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsedMs)
    val days = TimeUnit.MILLISECONDS.toDays(elapsedMs)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}

/** Ported from the teammate's sprint-2-ui-navigation branch, unchanged aside from theme tokens. */
data class NotificationEntry(
    val icon: ImageVector,
    val accentColor: Color,
    val title: String,
    val description: String,
    val timestamp: String,
    val isUnread: Boolean
)

@Composable
fun NotificationsOverlay(
    notifications: List<NotificationEntry>,
    onDismiss: () -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unreadCount = notifications.count { it.isUnread }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOlive.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .background(
                    BackgroundLight,
                    RoundedCornerShape(bottomStart = 22.5.dp, bottomEnd = 22.5.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.79.dp, DeepOlive.copy(alpha = 0.08f))
                    .padding(horizontal = 18.75.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.5.dp)) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = DeepOlive,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Notifications",
                        fontFamily = Nunito,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.5.sp,
                        color = DeepOlive
                    )
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(Orchid, RoundedCornerShape(50))
                                .padding(horizontal = 5.63.dp, vertical = 1.66.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadCount.toString(),
                                textAlign = TextAlign.Center,
                                fontFamily = Nunito,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 18.sp,
                                color = BackgroundLight
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.75.dp),
                    modifier = Modifier.clickable(onClick = onMarkAllRead)
                ) {
                    Text(
                        text = "Mark all read",
                        fontFamily = Nunito,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.5.sp,
                        color = SageAccent
                    )
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Mark all read",
                        tint = SageAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 15.dp, vertical = 11.25.dp),
                verticalArrangement = Arrangement.spacedBy(7.5.dp)
            ) {
                notifications.forEach { entry -> NotificationRow(entry) }
            }
        }
    }
}

@Composable
private fun NotificationRow(entry: NotificationEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (entry.isUnread) entry.accentColor.copy(alpha = 0.09f) else Color.Transparent,
                RoundedCornerShape(15.dp)
            )
            .border(
                0.79.dp,
                if (entry.isUnread) entry.accentColor.copy(alpha = 0.19f) else DeepOlive.copy(alpha = 0.06f),
                RoundedCornerShape(15.dp)
            )
            .padding(11.25.dp),
        horizontalArrangement = Arrangement.spacedBy(11.25.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(entry.accentColor.copy(alpha = 0.09f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = entry.accentColor,
                modifier = Modifier.size(15.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = entry.title,
                    modifier = Modifier.weight(1f),
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = if (entry.isUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                    lineHeight = 18.2.sp,
                    color = DeepOlive
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.63.dp)) {
                    if (entry.isUnread) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(entry.accentColor, RoundedCornerShape(50))
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = SageAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.description,
                fontFamily = Nunito,
                fontSize = 13.sp,
                lineHeight = 18.2.sp,
                color = MutedText
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = entry.timestamp,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = SageAccent
            )
        }
    }
}
