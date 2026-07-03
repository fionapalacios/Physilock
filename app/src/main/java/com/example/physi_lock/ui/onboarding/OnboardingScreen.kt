package com.example.physi_lock.ui.onboarding

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
    val settingsIntent: (Context) -> Intent
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
                        Text(text = step.title, style = MaterialTheme.typography.titleLarge)
                        Text(text = step.description, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (!granted) {
                        TextButton(onClick = { context.startActivity(step.settingsIntent(context)) }) {
                            Text("Enable")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onFinished,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}
