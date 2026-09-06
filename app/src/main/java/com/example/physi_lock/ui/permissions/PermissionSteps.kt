package com.example.physi_lock.ui.permissions

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage
import com.example.physi_lock.ui.theme.TertiaryTan

/** Shared between OnboardingScreen (first-run grant flow) and Settings' revisitable
 *  Permissions screen, so both ever show the exact same live-checked state instead of
 *  drifting into two separate ideas of what's granted. Moved out of OnboardingScreen.kt
 *  2026-08-29 when Settings' "Permissions" row grew a real screen of its own. */
val OliveAccent = Color(0xFF6B7A4E)

data class PermissionStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color,
    val isGranted: (Context) -> Boolean,
    val settingsIntent: (Context) -> Intent,
    val required: Boolean = true,
    // Set only for steps Android lets an app request via a native Allow/Don't allow dialog
    // (normal runtime permissions). Usage Access, Overlay, and Accessibility are Android
    // "special app access" grants with no such API -- the OS requires Settings for those,
    // for every app, by design.
    val androidPermission: String? = null
)

val permissionSteps = listOf(
    PermissionStep(
        title = "Usage Access",
        description = "Lets Physi-Lock see how long you spend in each app.",
        icon = Icons.Default.Check,
        iconBackground = SageAccent,
        iconTint = BackgroundLight,
        isGranted = { context ->
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        },
        settingsIntent = { Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS) }
    ),
    PermissionStep(
        title = "Display over other apps",
        description = "Lets Physi-Lock draw the Move-to-Unlock challenge screen on top of any app.",
        icon = Icons.Default.Layers,
        iconBackground = OliveAccent,
        iconTint = BackgroundLight,
        isGranted = { context -> Settings.canDrawOverlays(context) },
        settingsIntent = { context ->
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        }
    ),
    PermissionStep(
        title = "Accessibility Service",
        description = "Detects when a locked app opens and shows the Move-to-Unlock screen.",
        icon = Icons.Default.Accessibility,
        iconBackground = DeepOlive,
        iconTint = SecondarySage,
        isGranted = { context ->
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
        },
        settingsIntent = { Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
    ),
    PermissionStep(
        title = "Notifications",
        description = "Sends break reminders, doomscrolling alerts, and overuse prompts.",
        icon = Icons.Default.Notifications,
        iconBackground = Orchid,
        iconTint = BackgroundLight,
        isGranted = { context ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                true
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        },
        settingsIntent = { context ->
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        },
        androidPermission = android.Manifest.permission.POST_NOTIFICATIONS
    ),
    PermissionStep(
        title = "Battery Optimization Exemption",
        // Made required 2026-09-07 (was optional) -- reports of screen-time readings
        // looking wrong and usage not syncing properly on some devices traced to this
        // being skippable: without it, OEM battery managers (observed on vivo/Funtouch,
        // see the Break Reminder heads-up-notification gap documented elsewhere in this
        // file) can freeze or kill AppMonitorService's background monitoring, silently
        // dropping usage events rather than erroring visibly.
        description = "Keeps background monitoring stable so usage tracking and locks always trigger reliably.",
        icon = Icons.Default.BatteryChargingFull,
        iconBackground = TertiaryTan,
        iconTint = DeepOlive,
        isGranted = { context ->
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        },
        settingsIntent = { context ->
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")
            )
        }
    )
)

/** Same row look used at onboarding and again in Settings > Permissions -- the colored
 *  dot (not a checkbox/switch) matches onboarding's original design intent: tapping a
 *  granted row does nothing, there's nothing left to toggle off from here. */
@Composable
fun PermissionRow(
    step: PermissionStep,
    isGranted: Boolean,
    isOptional: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 13.13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.25.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(step.iconBackground, RoundedCornerShape(19.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                tint = step.iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isOptional) "${step.title} (recommended)" else step.title,
                fontFamily = Nunito,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.5.sp,
                color = DeepOlive
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = step.description,
                fontFamily = Nunito,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.2.sp,
                color = DeepOlive
            )
        }
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    color = if (isGranted) SageAccent else TertiaryTan,
                    shape = CircleShape
                )
        )
    }
}
