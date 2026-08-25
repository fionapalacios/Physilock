package com.example.physi_lock.data

import android.content.Context
import android.net.wifi.WifiManager

// Module 7 (Context-Aware AI): reads the SSID of the currently connected Wi-Fi network.
// Reading a real SSID requires ACCESS_FINE_LOCATION to be granted (an Android platform
// quirk since API 27 -- SSID counts as location-adjacent data) and an active Wi-Fi
// connection; returns null otherwise, including Android's own "<unknown ssid>" sentinel for
// the ungranted-permission case, so callers never mistake that literal string for a real
// network name.
fun currentWifiSsid(context: Context): String? = try {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    val ssid = wifiManager?.connectionInfo?.ssid?.trim('"')
    if (ssid.isNullOrBlank() || ssid == "<unknown ssid>") null else ssid
} catch (e: Exception) {
    null
}
