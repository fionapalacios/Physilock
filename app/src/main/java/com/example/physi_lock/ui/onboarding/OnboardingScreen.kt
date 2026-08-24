package com.example.physi_lock.ui.onboarding

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.physi_lock.R
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage
import com.example.physi_lock.ui.theme.TertiaryTan

/**
 * Visual design ported from the teammate's sprint-2-ui-navigation branch
 * (GrantPermissionsScreen.kt, 2026-08-22 rework). Their version fakes permission state with
 * local in-memory toggles — this keeps the real permission checks/settings-intent launches
 * this screen already had, same "their design, our data layer" pattern used for Home/Reports.
 *
 * 2026-08-24: Notifications now requests via Android's native runtime-permission dialog
 * (Allow/Don't allow) instead of opening Settings — see [PermissionStep.androidPermission].
 * Usage Access, Overlay, and Accessibility Service can't get the same treatment: Android
 * classifies these as "special app access" permissions with no request-dialog API at all —
 * every app, including YPT/Digital Wellbeing/Forest, has to send the user to Settings for
 * these three specifically, by OS design (they're powerful enough — usage history, drawing
 * over other apps, full UI introspection — that Google deliberately requires the extra
 * Settings friction rather than a one-tap dialog). Battery Optimization already shows a
 * native system Allow/Deny dialog via its settings intent — no dialog code needed there,
 * that's just how `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` behaves.
 */
private val OliveAccent = Color(0xFF6B7A4E)

private data class PermissionStep(
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
    // "special app access" grants with no such API — the OS requires Settings for those,
    // for every app, by design (see OnboardingScreen's kdoc).
    val androidPermission: String? = null
)

private val permissionSteps = listOf(
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
                androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
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
        description = "Keeps background monitoring stable so locks always trigger reliably.",
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
        },
        required = false
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTick by remember { mutableIntStateOf(0) }

    // For steps with a real Android runtime permission (currently just Notifications): shows
    // the native Allow/Don't allow dialog. Falls back to the settings intent only once the OS
    // reports the permission as permanently denied ("Don't ask again"), since re-launching the
    // dialog at that point would silently no-op — Settings is the only way back in from there.
    var askedPermissions by remember { mutableStateOf(emptySet<String>()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTick++ }

    fun requestStep(step: PermissionStep) {
        val permission = step.androidPermission
        if (permission == null) {
            context.startActivity(step.settingsIntent(context))
            return
        }
        val permanentlyDenied = activity != null &&
            permission in askedPermissions &&
            !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        if (permanentlyDenied) {
            context.startActivity(step.settingsIntent(context))
        } else {
            askedPermissions = askedPermissions + permission
            permissionLauncher.launch(permission)
        }
    }

    val grantedStates = remember(refreshTick) {
        permissionSteps.map { it.isGranted(context) }
    }
    val requiredSteps = permissionSteps.filter { it.required }
    val grantedCount = permissionSteps.filterIndexed { index, step ->
        step.required && grantedStates.getOrElse(index) { false }
    }.size
    val totalCount = requiredSteps.size
    val allGranted = grantedCount == totalCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 480.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.75.dp)
                    .padding(top = 24.dp, bottom = 15.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Physi-Lock logo",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 18.dp)
                )
                Text(
                    text = "Welcome,",
                    fontFamily = Nunito,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 33.sp,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Just grant a few permissions and Physi-Lock is ready to go!",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.15.sp,
                    color = DeepOlive
                )
            }

            HorizontalDivider(thickness = 1.25.dp, color = DeepOlive.copy(alpha = 0.08f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.75.dp)
                    .padding(vertical = 15.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundLight, RoundedCornerShape(15.dp))
                        .border(1.25.dp, DeepOlive.copy(alpha = 0.16f), RoundedCornerShape(15.dp))
                ) {
                    permissionSteps.forEachIndexed { index, step ->
                        val granted = grantedStates.getOrElse(index) { false }
                        PermissionRow(
                            step = step,
                            isGranted = granted,
                            isOptional = !step.required,
                            onClick = {
                                if (!granted) requestStep(step)
                            }
                        )
                        if (index != permissionSteps.lastIndex) {
                            HorizontalDivider(thickness = 1.25.dp, color = DeepOlive.copy(alpha = 0.08f))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.5.dp)
                    .padding(bottom = 11.25.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.63.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = SageAccent,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Your activity stays on your device, only anonymous summaries are ever shared.",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 19.5.sp,
                    color = SageAccent
                )
            }

            HorizontalDivider(thickness = 1.25.dp, color = DeepOlive.copy(alpha = 0.08f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.75.dp)
                    .padding(top = 15.dp, bottom = 30.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.5.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(15.dp),
                            ambientColor = DeepOlive.copy(alpha = 0.25f),
                            spotColor = DeepOlive.copy(alpha = 0.25f)
                        )
                        .background(DeepOlive, RoundedCornerShape(15.dp))
                        .clickable {
                            // Re-checks live instead of trusting the cached grantedStates — the
                            // ON_RESUME refresh can race with the OS actually finishing binding
                            // a just-toggled accessibility service, so a stale cache could keep
                            // re-opening Settings for something the user already granted.
                            refreshTick++
                            val freshGranted = permissionSteps.map { it.isGranted(context) }
                            val nextRequiredIndex = permissionSteps.indices.firstOrNull { index ->
                                permissionSteps[index].required && !freshGranted.getOrElse(index) { false }
                            } ?: -1
                            if (nextRequiredIndex >= 0) {
                                requestStep(permissionSteps[nextRequiredIndex])
                            } else {
                                onFinished()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (allGranted) "Continue to Dashboard" else "Continue ($grantedCount/$totalCount)",
                        textAlign = TextAlign.Center,
                        fontFamily = Nunito,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.5.sp,
                        color = BackgroundLight
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
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
