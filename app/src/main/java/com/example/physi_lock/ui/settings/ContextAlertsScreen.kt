package com.example.physi_lock.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.UserConfiguration
import com.example.physi_lock.data.currentWifiSsid
import com.example.physi_lock.data.isLocationPermissionGranted
import com.example.physi_lock.ui.components.AuthTabsBackground
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SoftSand

/**
 * Built from scratch (Module 7 — no teammate UI to port, this module wasn't built by them
 * either). The manuscript's own "Future Enhancements" list files real GPS-based
 * location-locking as out of MVP scope, so this implements the lighter version its own
 * "Location-Based Locking: custom rules by WiFi network" line describes: match against the
 * connected Wi-Fi network's name, not device coordinates — no location SDK dependency, and
 * reuses the ACCESS_FINE_LOCATION permission Android already requires just to read a Wi-Fi
 * SSID (unrelated to GPS tracking, but an Android platform requirement since API 27).
 */
@Composable
fun ContextAlertsScreen(
    config: UserConfiguration,
    onToggleEnabled: (Boolean) -> Unit,
    onSaveSsid: (String?) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(isLocationPermissionGranted(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    var currentSsid by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(hasLocationPermission) {
        currentSsid = if (hasLocationPermission) currentWifiSsid(context) else null
    }

    var ssidInput by remember(config.contextAlertWifiSsid) { mutableStateOf(config.contextAlertWifiSsid ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftSand)
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
                text = "Context Alerts",
                fontFamily = Nunito,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepOlive
            )
        }

        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepOlive, RoundedCornerShape(18.dp))
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(BackgroundLight.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Room, contentDescription = null, tint = BackgroundLight, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        text = "Alerts, not GPS tracking",
                        fontFamily = Nunito,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BackgroundLight
                    )
                    Text(
                        text = "Notifies you when you open a distracting app on a Wi-Fi network you flag — like a study space",
                        fontFamily = Nunito,
                        fontSize = 12.sp,
                        color = BackgroundLight.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Context Alerts",
                            fontFamily = Nunito,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepOlive
                        )
                        Text(
                            text = if (config.contextAlertsEnabled) "On" else "Off",
                            fontFamily = Nunito,
                            fontSize = 12.sp,
                            color = DeepOlive.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = config.contextAlertsEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = brandedSwitchColors()
                    )
                }
            }

            if (config.contextAlertsEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                if (!hasLocationPermission) {
                    SettingsCard {
                        Text(
                            text = "Location permission needed",
                            fontFamily = Nunito,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepOlive
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Android requires Location permission to read a Wi-Fi network's name, even though this feature doesn't use your GPS location.",
                            fontFamily = Nunito,
                            fontSize = 12.sp,
                            color = DeepOlive
                        )
                        Spacer(modifier = Modifier.height(11.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepOlive, RoundedCornerShape(15.dp))
                                .clickable { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Grant Location Permission",
                                fontFamily = Nunito,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BackgroundLight
                            )
                        }
                    }
                } else {
                    SettingsCard {
                        Text(
                            text = "WATCHED WI-FI NETWORK",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SageAccent
                        )
                        Spacer(modifier = Modifier.height(9.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AuthTabsBackground, RoundedCornerShape(15.dp))
                                .border(0.79.dp, DeepOlive.copy(alpha = 0.12f), RoundedCornerShape(15.dp))
                                .padding(horizontal = 15.dp, vertical = 13.dp)
                        ) {
                            if (ssidInput.isEmpty()) {
                                Text(
                                    text = "e.g. Library-WiFi",
                                    fontFamily = Nunito,
                                    fontSize = 14.sp,
                                    color = DeepOlive.copy(alpha = 0.5f)
                                )
                            }
                            BasicTextField(
                                value = ssidInput,
                                onValueChange = { ssidInput = it },
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = Nunito, fontSize = 14.sp, color = DeepOlive),
                                cursorBrush = SolidColor(DeepOlive),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(9.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = SageAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentSsid != null) "Currently on: $currentSsid" else "Not connected to Wi-Fi right now",
                                fontFamily = Nunito,
                                fontSize = 12.sp,
                                color = DeepOlive.copy(alpha = 0.7f)
                            )
                            if (currentSsid != null) {
                                Spacer(modifier = Modifier.width(9.dp))
                                Text(
                                    text = "Use this",
                                    fontFamily = Nunito,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Orchid,
                                    modifier = Modifier.clickable { ssidInput = currentSsid ?: "" }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(11.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepOlive, RoundedCornerShape(15.dp))
                                .clickable { onSaveSsid(ssidInput) }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Save Network",
                                fontFamily = Nunito,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BackgroundLight
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard {
                Text(
                    text = "Planned for Context-Aware AI",
                    fontFamily = Nunito,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Not built yet — the manuscript itself lists real GPS-based location locking as a future enhancement beyond this MVP.",
                    fontFamily = Nunito,
                    fontSize = 12.sp,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(9.dp))
                PlannedFeatureRow("Multiple saved locations", "Not just one watched network at a time")
                PlannedFeatureRow("GPS geofencing", "True location radius, not just Wi-Fi network name")
                PlannedFeatureRow("Per-location custom rules", "Different app-lock rules per saved context")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
