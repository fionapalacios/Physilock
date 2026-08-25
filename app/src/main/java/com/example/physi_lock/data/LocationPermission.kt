package com.example.physi_lock.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

// Module 7 (Context-Aware AI): gates Wi-Fi SSID reads (see WifiSsidReader.kt), which need
// ACCESS_FINE_LOCATION on real devices regardless of Android version.
fun isLocationPermissionGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
