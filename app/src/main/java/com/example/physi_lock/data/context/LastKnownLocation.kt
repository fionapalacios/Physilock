package com.example.physi_lock.data.context

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

// Module 7 (Context-Aware AI): the device's last-known location, from the platform
// LocationManager directly -- no Play Services Location dependency, matching this app's
// existing "no new location SDK" stance (see ContextAlertsScreen.kt's Wi-Fi matching).
// A passive last-fix read, not a live GPS request: good enough for a periodic proximity
// check (same 30s-ish cadence as WifiSsidReader.kt), and avoids the battery/permission-
// prompt cost of starting active location updates just to back a background alert.
fun lastKnownLocation(context: Context): Location? {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) {
        return null
    }
    return try {
        val locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        locationManager?.getProviders(true)
            ?.mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
            ?.maxByOrNull { it.time }
    } catch (e: Exception) {
        null
    }
}
