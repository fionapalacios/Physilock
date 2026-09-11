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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Room
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.context.isLocationPermissionGranted
import com.example.physi_lock.data.entity.UserConfiguration
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.DmMono
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent

/**
 * "Location" (Module 7 -- no teammate UI to port, this module wasn't built by them either).
 * 2026-09-09 redesign, per explicit user instruction: dropped Wi-Fi-network matching
 * entirely (it was confusing bundled alongside GPS under one "Context Alerts" screen) and
 * the earlier single generic watched-location in favor of two named, independently pinned
 * anchors -- School and Work -- each with its own real Leaflet.js map + an optional
 * "only alert during these hours" time gate against a preset window. Still alert-only,
 * never a block -- see AppMonitorService.nearestWatchedLocationName.
 */
@Composable
fun LocationScreen(
    config: UserConfiguration,
    onToggleEnabled: (Boolean) -> Unit,
    onSaveSchoolLocation: (Double, Double, Int) -> Unit,
    onClearSchoolLocation: () -> Unit,
    onSetSchoolTimeGateEnabled: (Boolean) -> Unit,
    onSetSchoolTimeStart: (Int) -> Unit,
    onSetSchoolTimeEnd: (Int) -> Unit,
    onSaveWorkLocation: (Double, Double, Int) -> Unit,
    onClearWorkLocation: () -> Unit,
    onSetWorkTimeGateEnabled: (Boolean) -> Unit,
    onSetWorkTimeStart: (Int) -> Unit,
    onSetWorkTimeEnd: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(isLocationPermissionGranted(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

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
                text = "Location",
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
                        text = "Alerts, not tracking",
                        fontFamily = Nunito,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BackgroundLight
                    )
                    Text(
                        text = "Notifies you when you open a distracting app near your pinned School or Work location",
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
                            text = "Enable Location Alerts",
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
                            text = "Location permission is needed to check your position against your pinned School/Work spots.",
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
                    LocationAnchorCard(
                        label = "School Location",
                        latitude = config.schoolLocationLatitude,
                        longitude = config.schoolLocationLongitude,
                        radiusMeters = config.schoolLocationRadiusMeters,
                        timeGateEnabled = config.schoolLocationTimeGateEnabled,
                        timeStartMinute = config.schoolLocationTimeStartMinute,
                        timeEndMinute = config.schoolLocationTimeEndMinute,
                        onSaveLocation = onSaveSchoolLocation,
                        onClearLocation = onClearSchoolLocation,
                        onSetTimeGateEnabled = onSetSchoolTimeGateEnabled,
                        onSetTimeStart = onSetSchoolTimeStart,
                        onSetTimeEnd = onSetSchoolTimeEnd
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LocationAnchorCard(
                        label = "Work Location",
                        latitude = config.workLocationLatitude,
                        longitude = config.workLocationLongitude,
                        radiusMeters = config.workLocationRadiusMeters,
                        timeGateEnabled = config.workLocationTimeGateEnabled,
                        timeStartMinute = config.workLocationTimeStartMinute,
                        timeEndMinute = config.workLocationTimeEndMinute,
                        onSaveLocation = onSaveWorkLocation,
                        onClearLocation = onClearWorkLocation,
                        onSetTimeGateEnabled = onSetWorkTimeGateEnabled,
                        onSetTimeStart = onSetWorkTimeStart,
                        onSetTimeEnd = onSetWorkTimeEnd
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LocationAnchorCard(
    label: String,
    latitude: Double?,
    longitude: Double?,
    radiusMeters: Int,
    timeGateEnabled: Boolean,
    timeStartMinute: Int,
    timeEndMinute: Int,
    onSaveLocation: (Double, Double, Int) -> Unit,
    onClearLocation: () -> Unit,
    onSetTimeGateEnabled: (Boolean) -> Unit,
    onSetTimeStart: (Int) -> Unit,
    onSetTimeEnd: (Int) -> Unit
) {
    // "One-time set" (2026-09-10, explicit user ask): once an anchor is pinned, don't keep
    // re-rendering the live map (and re-fetching tiles) on every visit -- the alert logic
    // itself never needed the map at all, it's pure local GPS math (see
    // AppMonitorService.nearestWatchedLocationName). Show a read-only summary instead, and
    // only pay for a live map when the user actually wants to change the pin.
    var isEditing by remember(latitude) { mutableStateOf(latitude == null) }

    SettingsCard {
        Text(
            text = label.uppercase(),
            fontFamily = DmMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = SageAccent
        )

        if (!isEditing && latitude != null && longitude != null) {
            Spacer(modifier = Modifier.height(9.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepOlive.copy(alpha = 0.05f), RoundedCornerShape(13.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pinned: %.5f, %.5f".format(latitude, longitude),
                        fontFamily = DmMono,
                        fontSize = 12.sp,
                        color = DeepOlive
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Alert radius: ${radiusMeters}m",
                        fontFamily = Nunito,
                        fontSize = 12.sp,
                        color = DeepOlive.copy(alpha = 0.7f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(
                        modifier = Modifier
                            .background(DeepOlive, RoundedCornerShape(12.dp))
                            .clickable { isEditing = true }
                            .padding(horizontal = 13.dp, vertical = 9.dp)
                    ) {
                        Text(text = "Change", fontFamily = Nunito, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BackgroundLight)
                    }
                    Box(
                        modifier = Modifier
                            .background(DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable { onClearLocation() }
                            .padding(horizontal = 13.dp, vertical = 9.dp)
                    ) {
                        Text(text = "Clear", fontFamily = Nunito, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepOlive)
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Tap the map to drop a pin — you'll get an alert opening a distracting app within the shown radius.",
                fontFamily = Nunito,
                fontSize = 12.sp,
                color = DeepOlive.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(9.dp))

            var pickedLat by remember(latitude) { mutableStateOf(latitude) }
            var pickedLng by remember(longitude) { mutableStateOf(longitude) }
            var radius by remember(radiusMeters) { mutableStateOf(radiusMeters) }

            LeafletMapPicker(
                initialLat = pickedLat,
                initialLng = pickedLng,
                radiusMeters = radius,
                onPick = { lat, lng -> pickedLat = lat; pickedLng = lng }
            )

            Spacer(modifier = Modifier.height(11.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(50, 100, 250, 500).forEach { meters ->
                    RadiusChip(
                        label = "${meters}m",
                        selected = radius == meters,
                        onClick = { radius = meters }
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
                fontFamily = DmMono,
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
                            onSaveLocation(pickedLat!!, pickedLng!!, radius)
                            isEditing = false
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
                if (latitude != null) {
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

        Spacer(modifier = Modifier.height(14.dp))
        Box(modifier = Modifier.fillMaxWidth().background(DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(1.dp)).height(1.dp))
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Only alert during set hours",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepOlive
                )
                Text(
                    text = if (timeGateEnabled) "On — outside this window, being near this spot won't alert" else "Off — alerts any time you're near this spot",
                    fontFamily = Nunito,
                    fontSize = 11.sp,
                    color = DeepOlive.copy(alpha = 0.6f)
                )
            }
            Switch(checked = timeGateEnabled, onCheckedChange = onSetTimeGateEnabled, colors = brandedSwitchColors())
        }

        if (timeGateEnabled) {
            Spacer(modifier = Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                TimeField(
                    label = "STARTS AT",
                    minuteOfDay = timeStartMinute,
                    onSelect = onSetTimeStart,
                    modifier = Modifier.weight(1f)
                )
                TimeField(
                    label = "ENDS AT",
                    minuteOfDay = timeEndMinute,
                    onSelect = onSetTimeEnd,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Real HH:MM precision, not the app's usual hour-only DropdownMenu picker (see
// BedtimeModeScreen's HourField) -- explicit user instruction for Location's time gate:
// "hh:mm for start and finish, not fixed time". Native TimePickerDialog gives full
// minute precision with no custom picker UI to build.
@Composable
private fun TimeField(label: String, minuteOfDay: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .background(DeepOlive.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .clickable {
                val hour = minuteOfDay / 60
                val minute = minuteOfDay % 60
                android.app.TimePickerDialog(
                    context,
                    { _, pickedHour, pickedMinute -> onSelect(pickedHour * 60 + pickedMinute) },
                    hour,
                    minute,
                    false
                ).show()
            }
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Column {
            Text(text = label, fontFamily = DmMono, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SageAccent)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = formatMinuteOfDay(minuteOfDay), fontFamily = Nunito, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepOlive)
        }
    }
}

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val hour24 = minuteOfDay / 60
    val minute = minuteOfDay % 60
    val amPm = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "%d:%02d %s".format(hour12, minute, amPm)
}
