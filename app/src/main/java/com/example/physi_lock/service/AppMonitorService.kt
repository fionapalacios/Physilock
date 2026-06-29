package com.example.physi_lock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.physi_lock.ui.LockActivity

class AppMonitorService : AccessibilityService() {

    private val lockedPackages = setOf(
        "com.ss.android.ugc.trill",
        "com.instagram.android",
        "com.facebook.katana",
        "com.google.android.youtube"
    )

    private var lastLockedPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        Log.d("PhysiLockMonitor", "Event from: $packageName")
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        if (packageName in lockedPackages && packageName != lastLockedPackage) {
            lastLockedPackage = packageName
            try {
                Log.d("PhysiLockMonitor", "Attempting to launch LockActivity for $packageName")
                startActivity(Intent(this, LockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                Log.d("PhysiLockMonitor", "startActivity call completed without exception")
            } catch (e: Exception) {
                Log.e("PhysiLockMonitor", "Failed to launch LockActivity", e)
            }
        } else if (packageName !in lockedPackages) {
            lastLockedPackage = null
        }
    }

    override fun onInterrupt() {}
}