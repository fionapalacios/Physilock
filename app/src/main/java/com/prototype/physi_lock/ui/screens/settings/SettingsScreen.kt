package com.prototype.physi_lock.ui.screens.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prototype.physi_lock.ui.components.AuthTabsBackground
import com.prototype.physi_lock.ui.theme.AccentLavender
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.ErrorRed
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen
import com.prototype.physi_lock.ui.theme.SecondarySage
import com.prototype.physi_lock.ui.theme.TertiaryTan

private enum class UsageMode { WORK, STUDENT }

private data class SettingsRow(
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color,
    val title: String,
    val subtitle: String? = null,
    val titleColor: Color = PrimaryDark,
    val trailing: SettingsTrailing = SettingsTrailing.Chevron
)

private sealed interface SettingsTrailing {
    data object Chevron : SettingsTrailing
    data class Toggle(val checked: Boolean) : SettingsTrailing
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val profile by profileViewModel.profile.collectAsState()

    var usageMode by remember { mutableStateOf(UsageMode.WORK) }
    var locationContextEnabled by remember { mutableStateOf(false) }
    var wellnessNudgesEnabled by remember { mutableStateOf(true) }
    var showEditProfile by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 15.dp)
                .padding(top = 15.dp, bottom = 24.dp)
        ) {
            Text(
                text = "Settings",
                fontFamily = NunitoFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 22.sp,
                color = PrimaryDark
            )

            Spacer(modifier = Modifier.height(22.5.dp))

            ProfileCard(profile = profile, onClick = { showEditProfile = true })

            Spacer(modifier = Modifier.height(18.75.dp))

            UsageModeCard(selected = usageMode, onSelected = { usageMode = it })

            Spacer(modifier = Modifier.height(15.dp))

            SettingsSectionCard(
                heading = "NOTIFICATIONS & PRIVACY",
                rows = listOf(
                    SettingsRow(
                        icon = Icons.Default.NotificationsActive,
                        iconBackground = PrimaryGreen.copy(alpha = 0.13f),
                        iconTint = PrimaryGreen,
                        title = "Break Reminders",
                        subtitle = "Every 45 min of screen use"
                    ),
                    SettingsRow(
                        icon = Icons.Default.Room,
                        iconBackground = AccentLavender.copy(alpha = 0.13f),
                        iconTint = AccentLavender,
                        title = "Location Context",
                        subtitle = "Adapt limits by location",
                        trailing = SettingsTrailing.Toggle(locationContextEnabled)
                    ),
                    SettingsRow(
                        icon = Icons.Default.Bedtime,
                        iconBackground = PrimaryDark.copy(alpha = 0.08f),
                        iconTint = PrimaryDark,
                        title = "Bedtime Mode",
                        subtitle = "Lock apps after 11 PM"
                    ),
                    SettingsRow(
                        icon = Icons.Default.SelfImprovement,
                        iconBackground = AccentLavender.copy(alpha = 0.13f),
                        iconTint = AccentLavender,
                        title = "Wellness Nudges",
                        subtitle = "Daily reflection prompts",
                        trailing = SettingsTrailing.Toggle(wellnessNudgesEnabled)
                    )
                ),
                onToggle = { index ->
                    when (index) {
                        1 -> locationContextEnabled = !locationContextEnabled
                        3 -> wellnessNudgesEnabled = !wellnessNudgesEnabled
                    }
                }
            )

            Spacer(modifier = Modifier.height(15.dp))

            SettingsSectionCard(
                heading = "ACCOUNT",
                rows = listOf(
                    SettingsRow(
                        icon = Icons.Default.Shield,
                        iconBackground = DeepOlive.copy(alpha = 0.13f),
                        iconTint = DeepOlive,
                        title = "App Lock Rules"
                    ),
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                        iconBackground = PrimaryGreen.copy(alpha = 0.13f),
                        iconTint = PrimaryGreen,
                        title = "Whitelist Manager",
                        subtitle = "Always-accessible apps"
                    ),
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        iconBackground = PrimaryGreen.copy(alpha = 0.13f),
                        iconTint = PrimaryGreen,
                        title = "Permissions",
                        subtitle = "Manage app permissions"
                    ),
                    SettingsRow(
                        icon = Icons.Default.Room,
                        iconBackground = AccentLavender.copy(alpha = 0.13f),
                        iconTint = AccentLavender,
                        title = "Location & Context AI"
                    ),
                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconBackground = DeepOlive.copy(alpha = 0.13f),
                        iconTint = DeepOlive,
                        title = "About"
                    ),
                    SettingsRow(
                        icon = Icons.Default.Restore,
                        iconBackground = ErrorRed.copy(alpha = 0.07f),
                        iconTint = ErrorRed,
                        title = "Reset to Default Settings",
                        titleColor = ErrorRed
                    ),
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconBackground = ErrorRed.copy(alpha = 0.13f),
                        iconTint = ErrorRed,
                        title = "Sign Out",
                        titleColor = ErrorRed
                    ),
                    SettingsRow(
                        icon = Icons.Default.DeleteForever,
                        iconBackground = ErrorRed.copy(alpha = 0.07f),
                        iconTint = ErrorRed,
                        title = "Delete Account",
                        titleColor = ErrorRed
                    )
                ),
                onToggle = {}
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Physi-Lock · v1.0.0 · © 2026",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.5.sp,
                color = PrimaryGreen
            )
        }

        if (showEditProfile) {
            EditProfileScreen(
                onBackClick = { showEditProfile = false },
                onSaveClick = { showEditProfile = false },
                modifier = Modifier.fillMaxSize(),
                profileViewModel = profileViewModel
            )
        }
    }
}

@Composable
private fun ProfileCard(profile: ProfileUiState, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryDark, RoundedCornerShape(22.5.dp))
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(SecondarySage, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = PrimaryDark,
                modifier = Modifier.size(26.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.fullName,
                fontFamily = NunitoFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 18.7.sp,
                color = BackgroundLight
            )
            Text(
                text = profile.email,
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 21.sp,
                color = SecondarySage
            )
            Spacer(modifier = Modifier.height(5.63.dp))
            Text(
                text = "· 3-day streak 🔥",
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.5.sp,
                color = SecondarySage
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Edit profile",
            tint = SecondarySage,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun UsageModeCard(selected: UsageMode, onSelected: (UsageMode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthTabsBackground, RoundedCornerShape(22.5.dp))
            .border(0.79.dp, PrimaryDark.copy(alpha = 0.14f), RoundedCornerShape(22.5.dp))
            .padding(15.dp)
    ) {
        Text(
            text = "USAGE MODE",
            fontFamily = NunitoFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 21.sp,
            color = PrimaryDark
        )
        Spacer(modifier = Modifier.height(11.25.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.5.dp)) {
            UsageModeOption(
                icon = Icons.Default.Work,
                title = "Work Mode",
                subtitle = "Block social media 9–5",
                isSelected = selected == UsageMode.WORK,
                onClick = { onSelected(UsageMode.WORK) }
            )
            UsageModeOption(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "Student Mode",
                subtitle = "Study focus with Pomodoro",
                isSelected = selected == UsageMode.STUDENT,
                onClick = { onSelected(UsageMode.STUDENT) }
            )
        }
    }
}

@Composable
private fun UsageModeOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) PrimaryDark else PrimaryDark.copy(alpha = 0.04f),
                RoundedCornerShape(19.dp)
            )
            .border(
                0.79.dp,
                if (isSelected) PrimaryDark else PrimaryDark.copy(alpha = 0.12f),
                RoundedCornerShape(19.dp)
            )
            .clickable(onClick = onClick)
            .padding(11.25.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.25.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (isSelected) SecondarySage.copy(alpha = 0.20f) else SecondarySage.copy(alpha = 0.13f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) SecondarySage else PrimaryGreen,
                modifier = Modifier.size(15.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp,
                color = if (isSelected) BackgroundLight else PrimaryDark
            )
            Text(
                text = subtitle,
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.5.sp,
                color = if (isSelected) SecondarySage else DeepOlive
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(SecondarySage, CircleShape)
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    heading: String,
    rows: List<SettingsRow>,
    onToggle: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthTabsBackground, RoundedCornerShape(22.5.dp))
            .border(0.79.dp, PrimaryDark.copy(alpha = 0.14f), RoundedCornerShape(22.5.dp))
            .padding(horizontal = 15.dp)
    ) {
        Text(
            text = heading,
            modifier = Modifier.padding(top = 11.25.dp, bottom = 11.25.dp),
            fontFamily = NunitoFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 21.sp,
            color = PrimaryDark
        )
        rows.forEachIndexed { index, row ->
            SettingsRowItem(
                row = row,
                showDivider = index != rows.lastIndex,
                onClick = { if (row.trailing is SettingsTrailing.Toggle) onToggle(index) }
            )
        }
    }
}

@Composable
private fun SettingsRowItem(row: SettingsRow, showDivider: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 11.25.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.25.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(row.iconBackground, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = row.icon,
                    contentDescription = null,
                    tint = row.iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    color = row.titleColor
                )
                if (row.subtitle != null) {
                    Text(
                        text = row.subtitle,
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 19.5.sp,
                        color = DeepOlive
                    )
                }
            }
            when (row.trailing) {
                is SettingsTrailing.Toggle -> SettingsToggle(checked = row.trailing.checked)
                SettingsTrailing.Chevron -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.79.dp)
                    .background(PrimaryDark.copy(alpha = 0.09f))
            )
        }
    }
}

@Composable
private fun SettingsToggle(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(42.dp)
            .height(24.dp)
            .background(
                if (checked) PrimaryDark else TertiaryTan,
                RoundedCornerShape(50)
            )
            .then(
                if (checked) Modifier else Modifier.border(0.79.dp, PrimaryDark.copy(alpha = 0.25f), RoundedCornerShape(50))
            )
    ) {
        Box(
            modifier = Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 3.dp)
                .size(18.dp)
                .background(BackgroundLight, CircleShape)
        )
    }
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun SettingsScreenPreview() {
    PhysiLockTheme {
        SettingsScreen()
    }
}
