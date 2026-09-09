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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.entity.UserConfiguration
import com.example.physi_lock.data.context.currentWifiSsid
import com.example.physi_lock.data.context.isLocationPermissionGranted
import com.example.physi_lock.ui.components.AuthTabsBackground
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SoftSand

/**
 * Built from scratch (Module 7 — no teammate UI to port, this module wasn't built by them
 * either). Originally just Wi-Fi-network-name matching, since the manuscript's own "Future
 * Enhancements" list files real GPS-based location-locking as out of MVP scope. 2026-09-04:
 * the user explicitly asked to add real GPS via a Leaflet.js map anyway (their own call,
 * knowingly beyond the manuscript's stated MVP scope) — kept this same layout/backing
 * screen ("same scratch UI") and added a map section alongside the existing Wi-Fi one
 * rather than replacing it; either or both trigger signals can be set independently. Still
 * alert-only, never a block — see AppMonitorService.isNearWatchedLocation/isOnWatchedWifi.
 */
@Composable
fun ContextAlertsScreen(
    config: UserConfiguration,
    onToggleEnabled: (Boolean) -> Unit,
    onSaveSsid: (String?) -> Unit,
    onSaveLocation: (Double, Double, Int) -> Unit,
    onClearLocation: () -> Unit,
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

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsCard {
                        Text(
                            text = "WATCHED LOCATION",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SageAccent
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Tap the map to drop a pin — you'll get an alert opening a distracting app within the shown radius.",
                            fontFamily = Nunito,
                            fontSize = 12.sp,
                            color = DeepOlive.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(9.dp))

                        var pickedLat by remember(config.contextAlertLatitude) { mutableStateOf(config.contextAlertLatitude) }
                        var pickedLng by remember(config.contextAlertLongitude) { mutableStateOf(config.contextAlertLongitude) }
                        var radiusMeters by remember(config.contextAlertRadiusMeters) { mutableStateOf(config.contextAlertRadiusMeters) }

                        LeafletMapPicker(
                            initialLat = pickedLat,
                            initialLng = pickedLng,
                            radiusMeters = radiusMeters,
                            onPick = { lat, lng -> pickedLat = lat; pickedLng = lng }
                        )

                        Spacer(modifier = Modifier.height(11.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(50, 100, 250, 500).forEach { meters ->
                                RadiusChip(
                                    label = "${meters}m",
                                    selected = radiusMeters == meters,
                                    onClick = { radiusMeters = meters }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(11.dp))

                        Text(
                            text = if (pickedLat != null && pickedLng != null) {
                                "Pinned: %.5f, %.5f".format(pickedLat, pickedLng)
                            } else {
                                "No location pinned yet"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = DeepOlive.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(11.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(DeepOlive, RoundedCornerShape(15.dp))
                                    .clickable(enabled = pickedLat != null && pickedLng != null) {
                                        onSaveLocation(pickedLat!!, pickedLng!!, radiusMeters)
                                    }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Save Location",
                                    fontFamily = Nunito,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BackgroundLight.copy(alpha = if (pickedLat != null) 1f else 0.5f)
                                )
                            }
                            if (config.contextAlertLatitude != null) {
                                Box(
                                    modifier = Modifier
                                        .background(DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
                                        .clickable {
                                            pickedLat = null
                                            pickedLng = null
                                            onClearLocation()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 13.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "Clear", fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepOlive)
                                }
                            }
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
                    text = "Real GPS proximity is built (above); these are still just ideas, not yet built.",
                    fontFamily = Nunito,
                    fontSize = 12.sp,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(9.dp))
                PlannedFeatureRow("Multiple saved locations", "Not just one watched network/location at a time")
                PlannedFeatureRow("Per-location custom rules", "Different app-lock rules per saved context")
                PlannedFeatureRow("Automatic profile switching", "e.g. auto-switch Student/Work Mode by location, not just a passive alert")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Real map, via Leaflet.js in a WebView -- per the user's explicit ask (2026-09-04), not
 *  a native Compose/Play-Services-Maps widget. Loads Leaflet + OpenStreetMap tiles from
 *  their public CDN/tile servers (needs the INTERNET permission this app already has);
 *  tapping the map drops a pin + shows the alert radius, and calls back into Kotlin via a
 *  JavaScript interface. No location SDK dependency added -- the "use my current
 *  location" fallback center reuses the same plain-LocationManager
 *  [com.example.physi_lock.data.context.lastKnownLocation] read the background proximity
 *  check itself uses. */
@Composable
private fun LeafletMapPicker(
    initialLat: Double?,
    initialLng: Double?,
    radiusMeters: Int,
    onPick: (Double, Double) -> Unit
) {
    val context = LocalContext.current

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(15.dp)),
        factory = { ctx ->
            val fallback = com.example.physi_lock.data.context.lastKnownLocation(ctx)
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webViewClient = android.webkit.WebViewClient()
                addJavascriptInterface(
                    object {
                        @android.webkit.JavascriptInterface
                        fun onPick(lat: Double, lng: Double) {
                            post { onPick(lat, lng) }
                        }
                    },
                    "AndroidBridge"
                )
                loadDataWithBaseURL(
                    "https://unpkg.com",
                    leafletMapHtml(
                        lat = initialLat,
                        lng = initialLng,
                        radiusMeters = radiusMeters,
                        fallbackLat = fallback?.latitude,
                        fallbackLng = fallback?.longitude
                    ),
                    "text/html",
                    "utf-8",
                    null
                )
            }
        },
        update = { view -> view.evaluateJavascript("if (window.setRadius) { window.setRadius($radiusMeters); }", null) }
    )
}

private fun leafletMapHtml(lat: Double?, lng: Double?, radiusMeters: Int, fallbackLat: Double?, fallbackLng: Double?): String {
    val startLat = lat ?: fallbackLat ?: 0.0
    val startLng = lng ?: fallbackLng ?: 0.0
    val startZoom = if (lat != null) 16 else if (fallbackLat != null) 13 else 2
    val markerJs = if (lat != null && lng != null) "placeMarker($lat, $lng);" else ""
    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <style>html,body,#map{height:100%;margin:0;padding:0;}</style>
        </head>
        <body>
        <div id="map"></div>
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <script>
          var radius = $radiusMeters;
          var map = L.map('map', { attributionControl: false }).setView([$startLat, $startLng], $startZoom);
          L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
          var marker = null, circle = null;
          function placeMarker(lat, lng) {
            if (marker) { map.removeLayer(marker); }
            if (circle) { map.removeLayer(circle); }
            marker = L.marker([lat, lng]).addTo(map);
            circle = L.circle([lat, lng], { radius: radius, color: '#3d4928', fillColor: '#9cb06d', fillOpacity: 0.2 }).addTo(map);
          }
          $markerJs
          map.on('click', function(e) {
            placeMarker(e.latlng.lat, e.latlng.lng);
            if (window.AndroidBridge) { AndroidBridge.onPick(e.latlng.lat, e.latlng.lng); }
          });
          window.setRadius = function(r) {
            radius = r;
            if (circle) { circle.setRadius(r); }
          };
        </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
private fun RadiusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) DeepOlive else DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) BackgroundLight else DeepOlive
        )
    }
}
