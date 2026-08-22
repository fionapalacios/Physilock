package com.example.physi_lock.ui.onboarding

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private data class PermissionStep(
    val title: String,
    val description: String,
    val isGranted: (Context) -> Boolean,
    val settingsIntent: (Context) -> Intent,
    val required: Boolean = true
)

private val permissionSteps = listOf(
    PermissionStep(
        title = "Usage Access",
        description = "Lets Physi-Lock see how long you spend in each app.",
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
        title = "Display Over Other Apps",
        description = "Needed to show the shake-to-unlock screen on top of locked apps.",
        isGranted = { context -> Settings.canDrawOverlays(context) },
        settingsIntent = { context ->
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        }
    ),
    PermissionStep(
        title = "Accessibility Service",
        description = "Lets Physi-Lock detect which app is in the foreground so it can enforce locks.",
        isGranted = { context ->
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
        },
        settingsIntent = { Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
    ),
    PermissionStep(
        title = "Notifications",
        description = "Lets Physi-Lock send break reminders, overuse alerts, and intervention prompts.",
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
        }
    ),
    PermissionStep(
        title = "Battery Optimization",
        description = "Recommended for reliable background monitoring and lock enforcement.",
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome to Physi-Lock", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Physi-Lock helps you cut down on excessive phone use with friction-based unlock challenges. Grant these permissions to get started.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        permissionSteps.forEachIndexed { index, step ->
            val granted = grantedStates.getOrElse(index) { false }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (step.required) step.title else "${step.title} (recommended)",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(text = step.description, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (!granted) {
                        TextButton(onClick = { context.startActivity(step.settingsIntent(context)) }) {
                            Text(if (step.required) "Enable" else "Enable")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (allGranted) {
                    onFinished()
                } else {
                    val nextRequiredIndex = permissionSteps.indices.firstOrNull { index ->
                        permissionSteps[index].required && !grantedStates.getOrElse(index) { false }
                    } ?: -1
                    if (nextRequiredIndex >= 0) {
                        context.startActivity(permissionSteps[nextRequiredIndex].settingsIntent(context))
                    } else {
                        onFinished()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (allGranted) "Continue to Dashboard" else "Continue $grantedCount/$totalCount")
        }
    }
}
