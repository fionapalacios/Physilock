package com.prototype.physi_lock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.prototype.physi_lock.data.AppUsageLog
import com.prototype.physi_lock.data.PhysiLockDatabase
import com.prototype.physi_lock.ui.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppMonitorService : AccessibilityService() {

    companion object {
        var lastUnlockTime = 0L

        fun triggerGlobalUnlock() {
            lastUnlockTime = System.currentTimeMillis()
        }
    }

    @Volatile private var lockedPackages: Set<String> = emptySet()
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: PhysiLockDatabase
    private var currentForegroundPackage: String? = null
    private var currentSessionStartTime: Long? = null
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onServiceConnected() {
        super.onServiceConnected()
        database = PhysiLockDatabase.getInstance(this)
        serviceScope.launch {
            database.appLockRuleDao().getLockedPackageNames().collect { packages ->
                lockedPackages = packages.toSet()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        val currentTime = System.currentTimeMillis()

        // IGNORE EVERYTHING FOR 8 SECONDS AFTER UNLOCKING (Prevents close-app loop)
        if (currentTime - lastUnlockTime < 8000L) {
            return
        }

        // Ignore Physi-Lock's own events
        if (packageName == "com.prototype.physi_lock" || packageName.contains("physi_lock")) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChange(packageName, currentTime)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // Reserved for doomscrolling detection
                logScrollEvent(packageName, currentTime)
            }
        }
    }

    private fun handleWindowStateChange(packageName: String, currentTime: Long) {
        // Log the previous app session if it changed
        if (currentForegroundPackage != null && currentForegroundPackage != packageName) {
            logAppSession(currentForegroundPackage!!, currentTime)
        }

        // Update current foreground app
        currentForegroundPackage = packageName
        currentSessionStartTime = currentTime

        // Check if app should be locked
        if (packageName in lockedPackages) {
            val intent = Intent(this, LockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(LockActivity.EXTRA_APP_NAME, getAppName(packageName))
            }
            startActivity(intent)
        }
    }

    private fun logAppSession(packageName: String, endTime: Long) {
        val startTime = currentSessionStartTime ?: return
        val duration = endTime - startTime
        if (duration <= 0) return

        serviceScope.launch {
            try {
                val appName = getAppName(packageName)
                val dateKey = dateFormatter.format(Date(startTime))
                val log = AppUsageLog(
                    packageName = packageName,
                    appName = appName,
                    sessionStartTime = startTime,
                    sessionEndTime = endTime,
                    foregroundDurationMs = duration,
                    dateKey = dateKey
                )
                database.appUsageLogDao().insert(log)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun logScrollEvent(packageName: String, currentTime: Long) {
        // Reserved for future doomscrolling-detection work
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    override fun onInterrupt() {
        // Service interrupted by system
    }

    override fun onDestroy() {
        super.onDestroy()
        // Log final session if any
        currentForegroundPackage?.let {
            logAppSession(it, System.currentTimeMillis())
        }
    }
}
