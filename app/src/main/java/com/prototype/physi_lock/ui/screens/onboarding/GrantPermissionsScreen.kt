package com.prototype.physi_lock.ui.screens.onboarding

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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

private data class PermissionSpec(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val required: Boolean,
    val whyText: String,
    val whatWeSaveText: String
)

private val permissionSpecs = listOf(
    PermissionSpec(
        id = "usage_access",
        icon = Icons.Default.Assessment,
        title = "Usage Access",
        required = true,
        whyText = "Monitors which apps you open and for how long — the core of Physi-Lock's tracking.",
        whatWeSaveText = "App names, durations, open/close timestamps — stored on-device only."
    ),
    PermissionSpec(
        id = "accessibility_service",
        icon = Icons.Default.Accessibility,
        title = "Accessibility Service",
        required = true,
        whyText = "Detects when a blocked app is opened so Physi-Lock can intervene in real time.",
        whatWeSaveText = "Nothing beyond the current foreground app — never your screen content."
    ),
    PermissionSpec(
        id = "motion_sensors",
        icon = Icons.Default.DirectionsRun,
        title = "Motion & Sensors",
        required = true,
        whyText = "Powers Motion Lock challenges that require physical movement to unlock apps.",
        whatWeSaveText = "Short motion bursts used only to verify a challenge — discarded immediately after."
    ),
    PermissionSpec(
        id = "background_process",
        icon = Icons.Default.Sync,
        title = "Background Process",
        required = true,
        whyText = "Keeps tracking and focus sessions running even when Physi-Lock isn't on screen.",
        whatWeSaveText = "No extra data — this only keeps the on-device monitoring service alive."
    ),
    PermissionSpec(
        id = "location_access",
        icon = Icons.Default.LocationOn,
        title = "Location Access",
        required = false,
        whyText = "Enables location-aware rules, like relaxing limits when you're at the gym.",
        whatWeSaveText = "Coarse location only, used on-device to evaluate your rules."
    ),
    PermissionSpec(
        id = "battery_optimization",
        icon = Icons.Default.BatteryChargingFull,
        title = "Battery Optimizer Exemption",
        required = false,
        whyText = "Prevents the system from killing Physi-Lock's monitoring in the background.",
        whatWeSaveText = "No data — this is a system setting, not a data permission."
    ),
    PermissionSpec(
        id = "notifications",
        icon = Icons.Default.Notifications,
        title = "Notifications",
        required = false,
        whyText = "Lets Physi-Lock send break reminders and overuse alerts.",
        whatWeSaveText = "No data — used only to deliver alerts to your device."
    )
)

@Composable
fun GrantPermissionsScreen(
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var grantedIds by remember { mutableStateOf(emptySet<String>()) }
    var expandedId by remember { mutableStateOf<String?>(permissionSpecs.first().id) }

    val requiredCount = permissionSpecs.count { it.required }
    val requiredGrantedCount = permissionSpecs.count { it.required && it.id in grantedIds }
    val requiredRemaining = requiredCount - requiredGrantedCount

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 480.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.99.dp, color = PrimaryDark.copy(alpha = 0.08f))
                    .padding(top = 18.75.dp, bottom = 15.dp, start = 18.75.dp, end = 18.75.dp)
            ) {
                Text(
                    text = "Grant App Permissions",
                    fontFamily = NunitoFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 23.sp,
                    color = PrimaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Physi-Lock works ")
                        withStyle(SpanStyle(color = PrimaryDark, fontWeight = FontWeight.Bold)) {
                            append("entirely on-device")
                        }
                        append(". No data is ever uploaded to the internet.")
                    },
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    color = DeepOlive
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.75.dp)
            ) {
                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SecondarySage.copy(alpha = 0.13f), RoundedCornerShape(15.dp))
                        .border(0.99.dp, SecondarySage.copy(alpha = 0.40f), RoundedCornerShape(15.dp))
                        .padding(horizontal = 15.dp, vertical = 11.25.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.25.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = DeepOlive,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "All data stays on your phone. Nothing is shared with Physi-Lock servers or third parties.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.2.sp,
                        color = PrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                Column(verticalArrangement = Arrangement.spacedBy(7.5.dp)) {
                    permissionSpecs.forEach { spec ->
                        PermissionCard(
                            spec = spec,
                            isGranted = spec.id in grantedIds,
                            isExpanded = expandedId == spec.id,
                            onToggleExpand = {
                                expandedId = if (expandedId == spec.id) null else spec.id
                            },
                            onAllow = { grantedIds = grantedIds + spec.id }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SecondarySage.copy(alpha = 0.20f), RoundedCornerShape(19.dp))
                        .border(0.99.dp, SecondarySage.copy(alpha = 0.53f), RoundedCornerShape(19.dp))
                        .clickable { grantedIds = permissionSpecs.map { it.id }.toSet() }
                        .padding(vertical = 11.25.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Allow All Permissions",
                        textAlign = TextAlign.Center,
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.5.sp,
                        color = PrimaryDark
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(11.25.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.99.dp, color = PrimaryDark.copy(alpha = 0.08f))
                    .padding(top = 7.5.dp, bottom = 22.5.dp, start = 18.75.dp, end = 18.75.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(PrimaryDark, RoundedCornerShape(15.dp))
                        .clickable(onClick = onContinueClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Continue to Physi-Lock →",
                        textAlign = TextAlign.Center,
                        fontFamily = NunitoFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.5.sp,
                        color = BackgroundLight
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (requiredRemaining > 0) {
                        "$requiredRemaining required permission${if (requiredRemaining == 1) "" else "s"} not yet granted"
                    } else {
                        "All required permissions granted"
                    },
                    textAlign = TextAlign.Center,
                    fontFamily = NunitoFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = PrimaryGreen,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    spec: PermissionSpec,
    isGranted: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAllow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthTabsBackground, RoundedCornerShape(15.dp))
            .border(0.99.dp, PrimaryDark.copy(alpha = 0.10f), RoundedCornerShape(15.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 15.dp, vertical = 11.25.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.25.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PrimaryDark.copy(alpha = 0.06f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = DeepOlive,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.63.dp)
                ) {
                    Text(
                        text = spec.title,
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.5.sp,
                        color = PrimaryDark
                    )
                    if (spec.required) {
                        Box(
                            modifier = Modifier
                                .background(AccentLavender.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "REQUIRED",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp,
                                color = AccentLavender
                            )
                        }
                    }
                }
                Text(
                    text = if (isGranted) "Granted" else "Tap to learn more",
                    fontFamily = NunitoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                    color = if (isGranted) PrimaryGreen else DeepOlive
                )
            }
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.99.dp, PrimaryDark.copy(alpha = 0.08f))
                    .padding(horizontal = 15.dp, vertical = 11.25.dp),
                verticalArrangement = Arrangement.spacedBy(7.5.dp)
            ) {
                Column {
                    Text(
                        text = "WHY WE NEED THIS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.5.sp,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = spec.whyText,
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        color = DeepOlive
                    )
                }
                Column {
                    Text(
                        text = "WHAT WE SAVE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.5.sp,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = spec.whatWeSaveText,
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        color = DeepOlive
                    )
                }
                if (!isGranted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.75.dp)
                            .background(PrimaryDark, RoundedCornerShape(19.dp))
                            .clickable(onClick = onAllow)
                            .padding(vertical = 9.38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Allow",
                            textAlign = TextAlign.Center,
                            fontFamily = NunitoFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 19.5.sp,
                            color = BackgroundLight
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun GrantPermissionsScreenPreview() {
    PhysiLockTheme {
        GrantPermissionsScreen(onContinueClick = {})
    }
}
