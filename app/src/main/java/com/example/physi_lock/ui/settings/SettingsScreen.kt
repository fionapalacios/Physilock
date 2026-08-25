package com.example.physi_lock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.data.Account
import com.example.physi_lock.data.auth.ChangePasswordResult
import com.example.physi_lock.data.auth.FirebaseAccountRepository
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.ErrorRed
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage
import com.example.physi_lock.ui.theme.SoftSand
import kotlinx.coroutines.launch

/**
 * Visual design ported from the teammate's sprint-2-ui-navigation branch (SettingsScreen.kt) —
 * corrects an earlier session's mistake: that file was wrongly logged as a 0-byte stub (it's
 * real, 545 lines) and this screen got a from-scratch restyle instead of an actual port. This
 * pass replaces that with the real port: ProfileCard, the Work/Student usage-mode card, and the
 * icon-row settings-section-card layout are all theirs (theme tokens remapped), rewired onto
 * real data (their version's rows are all local-only `remember` state, not persisted anywhere).
 *
 * Deliberately matches the teammate's exact row list — a "USAGE & MOTION" card with a Daily
 * Screen Time Limit slider, Break Reminder interval slider, and Motion Lock Sensitivity picker
 * existed briefly (real functionality this repo had that their design doesn't cover) but was
 * removed per instruction to strictly follow their UI; Motion Lock Sensitivity's current value
 * is still visible read-only in StudentModeScreen/WorkModeScreen, just no longer editable from
 * here. Rows with no real backend yet (Bedtime Mode, Wellness Nudges, Whitelist Manager,
 * Permissions, Delete Account) are kept visible with a plain "Coming soon" subtitle rather
 * than dropped, per the instruction not to skip ported UI just because the logic behind it
 * isn't built. About/Reset-to-Default (easy to make real, so made real rather than left as
 * dead taps) are additions beyond their row list. "Location Context" became real 2026-08-25
 * (Module 7, see ContextAlertsScreen.kt) — it's now also the sole entry point for this
 * feature, so the separate "Location & Context AI" placeholder row (ACCOUNT section) was
 * removed as a literal duplicate rather than wired to the same screen twice.
 */
private val userModes = listOf("WORK_MODE" to "Work", "STUDENT_MODE" to "Student")

private data class SettingsRow(
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color,
    val title: String,
    val subtitle: String? = null,
    val titleColor: Color = DeepOlive,
    val trailing: SettingsTrailing = SettingsTrailing.Chevron,
    val onClick: () -> Unit = {}
)

private sealed interface SettingsTrailing {
    data object Chevron : SettingsTrailing
    data class Toggle(val checked: Boolean, val onToggle: (Boolean) -> Unit) : SettingsTrailing
    /** Read-only status text — for values Admin controls the default of and the user can
     *  only view (Motion Lock Sensitivity, Doomscrolling Detection). No chevron, no switch. */
    data class Label(val text: String) : SettingsTrailing
}

@Composable
fun SettingsScreen(
    currentAccount: Account?,
    onAccountUpdated: (Account) -> Unit,
    onLogout: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val accountRepository = remember { FirebaseAccountRepository() }

    var showAppLockRules by remember { mutableStateOf(false) }
    var showAccountEditor by remember { mutableStateOf(false) }
    var showStudentMode by remember { mutableStateOf(false) }
    var showWorkMode by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showContextAlerts by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordChangeMessage by remember { mutableStateOf<String?>(null) }
    var passwordChangeSuccess by remember { mutableStateOf(false) }

    // Rows with no real backend — local-only state, exactly as unpersisted as the
    // teammate's own version of these same toggles.
    var wellnessNudgesEnabled by remember { mutableStateOf(true) }

    if (showAppLockRules) {
        AppLockRulesScreen(onBack = { showAppLockRules = false })
        return
    }

    if (showAccountEditor) {
        EditProfileScreen(
            currentAccount = currentAccount,
            errorMessage = errorMessage,
            canChangePassword = remember { accountRepository.hasPasswordProvider() },
            onChangePassword = { current, new ->
                coroutineScope.launch {
                    when (val result = accountRepository.changePassword(current, new)) {
                        is ChangePasswordResult.Success -> {
                            passwordChangeSuccess = true
                            passwordChangeMessage = "Password updated."
                        }
                        is ChangePasswordResult.WrongCurrentPassword -> {
                            passwordChangeSuccess = false
                            passwordChangeMessage = "Current password is incorrect."
                        }
                        is ChangePasswordResult.NoPasswordProvider -> {
                            passwordChangeSuccess = false
                            passwordChangeMessage = "This account has no password to change."
                        }
                        is ChangePasswordResult.Error -> {
                            passwordChangeSuccess = false
                            passwordChangeMessage = result.message
                        }
                    }
                }
            },
            passwordChangeMessage = passwordChangeMessage,
            passwordChangeSuccess = passwordChangeSuccess,
            onBackClick = {
                errorMessage = null
                passwordChangeMessage = null
                showAccountEditor = false
            },
            onSave = { updated ->
                coroutineScope.launch {
                    val saved = accountRepository.updateAccount(updated)
                    if (saved == null) {
                        errorMessage = "Username or email is already in use."
                    } else {
                        errorMessage = null
                        showAccountEditor = false
                        onAccountUpdated(saved)
                    }
                }
            }
        )
        return
    }

    val config by settingsViewModel.configuration.collectAsState()
    val streakDays by settingsViewModel.streakDays.collectAsState()

    if (showStudentMode) {
        StudentModeScreen(
            config = config,
            isActive = config.userMode == "STUDENT_MODE",
            onSwitchToThisMode = { settingsViewModel.setUserMode("STUDENT_MODE") },
            onBackClick = { showStudentMode = false }
        )
        return
    }

    if (showWorkMode) {
        WorkModeScreen(
            config = config,
            isActive = config.userMode == "WORK_MODE",
            onSwitchToThisMode = { settingsViewModel.setUserMode("WORK_MODE") },
            onBackClick = { showWorkMode = false }
        )
        return
    }

    if (showContextAlerts) {
        ContextAlertsScreen(
            config = config,
            onToggleEnabled = { settingsViewModel.setContextAlertsEnabled(it) },
            onSaveSsid = { settingsViewModel.setContextAlertWifiSsid(it) },
            onBackClick = { showContextAlerts = false }
        )
        return
    }

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
            fontFamily = Nunito,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepOlive
        )

        Spacer(modifier = Modifier.height(22.dp))

        ProfileCard(
            currentAccount = currentAccount,
            streakDays = streakDays,
            onClick = { showAccountEditor = true }
        )

        Spacer(modifier = Modifier.height(18.dp))

        UsageModeCard(
            selected = config.userMode,
            onSelectWork = { showWorkMode = true },
            onSelectStudent = { showStudentMode = true }
        )

        Spacer(modifier = Modifier.height(15.dp))

        SettingsSectionCard(
            heading = "NOTIFICATIONS & PRIVACY",
            rows = listOf(
                SettingsRow(
                    icon = Icons.Default.NotificationsActive,
                    iconBackground = SageAccent.copy(alpha = 0.13f),
                    iconTint = SageAccent,
                    title = "Break Reminders",
                    subtitle = if (config.breakReminderEnabled) {
                        "Every ${config.breakReminderIntervalMs / 60_000L} min of screen use"
                    } else "Off",
                    trailing = SettingsTrailing.Toggle(config.breakReminderEnabled) {
                        settingsViewModel.setBreakReminderEnabled(it)
                    }
                ),
                SettingsRow(
                    icon = Icons.Default.Shield,
                    iconBackground = DeepOlive.copy(alpha = 0.13f),
                    iconTint = DeepOlive,
                    title = "Overuse Alert",
                    subtitle = "Notify once per day when you exceed your daily limit",
                    trailing = SettingsTrailing.Toggle(config.overuseAlertsEnabled) {
                        settingsViewModel.setOveruseAlertsEnabled(it)
                    }
                ),
                SettingsRow(
                    icon = Icons.Default.NotificationsActive,
                    iconBackground = Orchid.copy(alpha = 0.13f),
                    iconTint = Orchid,
                    title = "Doomscrolling Detection",
                    subtitle = "Set by Admin — warns when scrolling patterns suggest doomscrolling",
                    trailing = SettingsTrailing.Label(if (config.doomscrollingDetectionEnabled) "On" else "Off")
                ),
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    iconBackground = SageAccent.copy(alpha = 0.13f),
                    iconTint = SageAccent,
                    title = "Motion Lock Sensitivity",
                    subtitle = "Set by Admin — how hard the unlock challenge is",
                    trailing = SettingsTrailing.Label(ChallengeSensitivity.displayLabel(config.motionLockSensitivity))
                ),
                SettingsRow(
                    icon = Icons.Default.Room,
                    iconBackground = Orchid.copy(alpha = 0.13f),
                    iconTint = Orchid,
                    title = "Location Context",
                    subtitle = when {
                        !config.contextAlertsEnabled -> "Off"
                        config.contextAlertWifiSsid.isNullOrBlank() -> "On — set your Wi-Fi network below"
                        else -> "Alerts on \"${config.contextAlertWifiSsid}\" Wi-Fi"
                    },
                    trailing = SettingsTrailing.Toggle(config.contextAlertsEnabled) {
                        settingsViewModel.setContextAlertsEnabled(it)
                    },
                    onClick = { showContextAlerts = true }
                ),
                SettingsRow(
                    icon = Icons.Default.Bedtime,
                    iconBackground = DeepOlive.copy(alpha = 0.08f),
                    iconTint = DeepOlive,
                    title = "Bedtime Mode",
                    subtitle = "Coming soon"
                ),
                SettingsRow(
                    icon = Icons.Default.SelfImprovement,
                    iconBackground = Orchid.copy(alpha = 0.13f),
                    iconTint = Orchid,
                    title = "Wellness Nudges",
                    subtitle = "Coming soon",
                    trailing = SettingsTrailing.Toggle(wellnessNudgesEnabled) { wellnessNudgesEnabled = it }
                )
            )
        )

        Spacer(modifier = Modifier.height(15.dp))

        SettingsSectionCard(
            heading = "ACCOUNT",
            rows = listOf(
                SettingsRow(
                    icon = Icons.Default.Shield,
                    iconBackground = DeepOlive.copy(alpha = 0.13f),
                    iconTint = DeepOlive,
                    title = "App Lock Rules",
                    subtitle = "Choose which apps require a shake challenge",
                    onClick = { showAppLockRules = true }
                ),
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                    iconBackground = SageAccent.copy(alpha = 0.13f),
                    iconTint = SageAccent,
                    title = "Whitelist Manager",
                    subtitle = "Coming soon"
                ),
                SettingsRow(
                    icon = Icons.Default.Lock,
                    iconBackground = SageAccent.copy(alpha = 0.13f),
                    iconTint = SageAccent,
                    title = "Permissions",
                    subtitle = "Coming soon"
                ),
                SettingsRow(
                    icon = Icons.Default.Info,
                    iconBackground = DeepOlive.copy(alpha = 0.13f),
                    iconTint = DeepOlive,
                    title = "About",
                    onClick = { showAboutDialog = true }
                ),
                SettingsRow(
                    icon = Icons.Default.Restore,
                    iconBackground = ErrorRed.copy(alpha = 0.07f),
                    iconTint = ErrorRed,
                    title = "Reset to Default Settings",
                    titleColor = ErrorRed,
                    onClick = { showResetConfirm = true }
                ),
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    iconBackground = ErrorRed.copy(alpha = 0.13f),
                    iconTint = ErrorRed,
                    title = "Sign Out",
                    titleColor = ErrorRed,
                    onClick = onLogout
                ),
                SettingsRow(
                    icon = Icons.Default.DeleteForever,
                    iconBackground = ErrorRed.copy(alpha = 0.07f),
                    iconTint = ErrorRed,
                    title = "Delete Account",
                    subtitle = "Coming soon",
                    titleColor = ErrorRed
                )
            )
        )

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "Physi-Lock · v${appVersionName(context)} · © 2026",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = SageAccent
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Physi-Lock") },
            text = {
                Text(
                    "Physi-Lock v${appVersionName(context)}\n\n" +
                        "AI-powered screen time control that rewards physical movement and protects your mental wellness."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset to default settings?") },
            text = { Text("This resets your daily limit, break reminders, alerts, and motion sensitivity back to their defaults. Your usage mode stays the same.") },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.resetToDefaults()
                    showResetConfirm = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

private fun appVersionName(context: android.content.Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
} catch (e: Exception) {
    "1.0"
}

@Composable
private fun ProfileCard(currentAccount: Account?, streakDays: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepOlive, RoundedCornerShape(22.dp))
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
            Icon(Icons.Default.Person, contentDescription = null, tint = DeepOlive, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentAccount?.fullName ?: "Signed in user",
                fontFamily = Nunito,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BackgroundLight
            )
            Text(
                text = currentAccount?.email ?: "",
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SecondarySage
            )
            if (streakDays > 0) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "· $streakDays-day streak 🔥",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondarySage
                )
            }
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
private fun UsageModeCard(
    selected: String,
    onSelectWork: () -> Unit,
    onSelectStudent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftSand, RoundedCornerShape(22.dp))
            .border(0.79.dp, DeepOlive.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
            .padding(15.dp)
    ) {
        Text(
            text = "USAGE MODE",
            fontFamily = Nunito,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepOlive
        )
        Spacer(modifier = Modifier.height(11.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.5.dp)) {
            UsageModeOption(
                icon = Icons.Default.Work,
                title = "Work Mode",
                subtitle = "Tuned for the workday",
                isSelected = selected == "WORK_MODE",
                onClick = onSelectWork
            )
            UsageModeOption(
                icon = Icons.Default.School,
                title = "Student Mode",
                subtitle = "Tuned for study sessions",
                isSelected = selected == "STUDENT_MODE",
                onClick = onSelectStudent
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
                if (isSelected) DeepOlive else DeepOlive.copy(alpha = 0.04f),
                RoundedCornerShape(19.dp)
            )
            .border(
                0.79.dp,
                if (isSelected) DeepOlive else DeepOlive.copy(alpha = 0.12f),
                RoundedCornerShape(19.dp)
            )
            .clickable(onClick = onClick)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
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
                tint = if (isSelected) SecondarySage else SageAccent,
                modifier = Modifier.size(15.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) BackgroundLight else DeepOlive
            )
            Text(
                text = subtitle,
                fontFamily = Nunito,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) SecondarySage else DeepOlive
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "View details",
            tint = if (isSelected) SecondarySage else DeepOlive.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SettingsSectionCard(heading: String, rows: List<SettingsRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftSand, RoundedCornerShape(22.dp))
            .border(0.79.dp, DeepOlive.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
            .padding(horizontal = 15.dp)
    ) {
        Text(
            text = heading,
            modifier = Modifier.padding(top = 11.dp, bottom = 11.dp),
            fontFamily = Nunito,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepOlive
        )
        rows.forEachIndexed { index, row ->
            SettingsRowItem(row = row, showDivider = index != rows.lastIndex)
        }
    }
}

@Composable
private fun SettingsRowItem(row: SettingsRow, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = row.onClick)
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(row.iconBackground, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = row.icon, contentDescription = null, tint = row.iconTint, modifier = Modifier.size(15.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = row.titleColor
                )
                if (row.subtitle != null) {
                    Text(
                        text = row.subtitle,
                        fontFamily = Nunito,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepOlive
                    )
                }
            }
            when (val trailing = row.trailing) {
                is SettingsTrailing.Toggle -> Switch(
                    checked = trailing.checked,
                    onCheckedChange = trailing.onToggle,
                    colors = brandedSwitchColors()
                )
                SettingsTrailing.Chevron -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = SageAccent,
                    modifier = Modifier.size(14.dp)
                )
                is SettingsTrailing.Label -> Text(
                    text = trailing.text,
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SageAccent
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.79.dp)
                    .background(DeepOlive.copy(alpha = 0.09f))
            )
        }
    }
}

@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftSand, RoundedCornerShape(22.dp))
            .border(0.79.dp, DeepOlive.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
            .padding(15.dp),
        content = content
    )
}

@Composable
internal fun brandedSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = BackgroundLight,
    checkedTrackColor = DeepOlive,
    uncheckedThumbColor = BackgroundLight,
    uncheckedTrackColor = DeepOlive.copy(alpha = 0.3f)
)
