package com.example.physi_lock.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.physi_lock.ui.permissions.PermissionRow
import com.example.physi_lock.ui.permissions.PermissionStep
import com.example.physi_lock.ui.permissions.permissionSteps
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito

/** Revisitable twin of OnboardingScreen's grant flow -- same permissionSteps/PermissionRow
 *  from ui.permissions, so a permission toggled off in Android Settings after onboarding
 *  (or an optional one skipped at first run) shows up here too, not just at first launch.
 *  Visual pattern matches ContextAlertsScreen: back-header row + SettingsCard. */
@Composable
fun PermissionsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTick by remember { mutableIntStateOf(0) }
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
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        if (permanentlyDenied) {
            context.startActivity(step.settingsIntent(context))
        } else {
            askedPermissions = askedPermissions + permission
            permissionLauncher.launch(permission)
        }
    }

    // Granting Usage Access/Overlay/Accessibility all happen in Android Settings, outside
    // this screen entirely -- refresh on return, same as OnboardingScreen's own resume check.
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
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = DeepOlive,
                modifier = Modifier.size(20.dp).clickable(onClick = onBackClick)
            )
            Text(
                text = "Permissions",
                fontFamily = Nunito,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepOlive
            )
        }

        Column(modifier = Modifier.padding(15.dp)) {
            SettingsCard {
                Text(
                    text = "App Permissions",
                    fontFamily = Nunito,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Physi-Lock needs these to lock apps and track usage. Tap a row that isn't granted yet.",
                    fontFamily = Nunito,
                    fontSize = 12.sp,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(9.dp))

                permissionSteps.forEachIndexed { index, step ->
                    val granted = grantedStates.getOrElse(index) { false }
                    PermissionRow(
                        step = step,
                        isGranted = granted,
                        isOptional = !step.required,
                        onClick = { if (!granted) requestStep(step) }
                    )
                    if (index != permissionSteps.lastIndex) {
                        HorizontalDivider(thickness = 0.79.dp, color = DeepOlive.copy(alpha = 0.09f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
