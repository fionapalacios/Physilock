package com.example.physi_lock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.physi_lock.ui.LockActivity

class AppMonitorService : AccessibilityService() {

    companion object {
        var lastUnlockTime = 0L

        fun triggerGlobalUnlock() {
            lastUnlockTime = System.currentTimeMillis()
        }
    }

    private val lockedPackages = setOf("com.google.android.youtube")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        val currentTime = System.currentTimeMillis()

        // IGNORE EVERYTHING FOR 8 SECONDS AFTER UNLOCKING (Kills the close-app loop completely)
        if (currentTime - lastUnlockTime < 8000L) {
            return
        }

        if (packageName == "com.example.physi_lock" || packageName.contains("physi_lock")) {
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (packageName in lockedPackages) {
                val intent = Intent(this, LockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {}
}