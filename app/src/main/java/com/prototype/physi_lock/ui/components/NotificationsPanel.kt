package com.prototype.physi_lock.ui.components

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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
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
import com.prototype.physi_lock.ui.theme.AccentLavender
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen

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
            .background(PrimaryDark.copy(alpha = 0.35f))
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
                    .border(0.79.dp, PrimaryDark.copy(alpha = 0.08f))
                    .padding(horizontal = 18.75.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.5.dp)) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = PrimaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Notifications",
                        fontFamily = NunitoFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.5.sp,
                        color = PrimaryDark
                    )
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(AccentLavender, RoundedCornerShape(50))
                                .padding(horizontal = 5.63.dp, vertical = 1.66.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadCount.toString(),
                                textAlign = TextAlign.Center,
                                fontFamily = NunitoFontFamily,
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
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.5.sp,
                        color = PrimaryGreen
                    )
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Mark all read",
                        tint = PrimaryGreen,
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
                if (entry.isUnread) entry.accentColor.copy(alpha = 0.19f) else PrimaryDark.copy(alpha = 0.06f),
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
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = if (entry.isUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                    lineHeight = 18.2.sp,
                    color = PrimaryDark
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
                        tint = PrimaryGreen,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.description,
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                lineHeight = 18.2.sp,
                color = DeepOlive
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = entry.timestamp,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = PrimaryGreen
            )
        }
    }
}
