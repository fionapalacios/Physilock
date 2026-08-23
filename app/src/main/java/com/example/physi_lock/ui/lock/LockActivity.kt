package com.example.physi_lock.ui.lock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.physi_lock.data.MotionInterventionLog
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.service.AppMonitorService
import com.example.physi_lock.ui.theme.PhysiLockTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LockActivity : ComponentActivity() {
    private var packageName = ""
    private var unlocked = false
    private var bypassLogged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packageName = intent.getStringExtra(AppMonitorService.EXTRA_PACKAGE_NAME) ?: ""
        setContent {
            PhysiLockTheme {
                LockScreen(packageName = packageName, onUnlocked = { unlocked = true; finish() })
            }
        }
    }

    // A "bypass attempt" (Module 2 Random Forest feature): the user was shown this
    // challenge and left without completing it — back button, home button, recents
    // swipe-away, or a screen-off all stop this Activity the same way. onStop() is a
    // reliable single hook for all of those, guarded so it only ever logs once per
    // challenge shown (a screen-off/on cycle would otherwise call onStop() again).
    override fun onStop() {
        super.onStop()
        if (!unlocked && !bypassLogged) {
            bypassLogged = true
            val bypassedPackage = packageName
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    PhysiLockDatabase.getInstance(applicationContext).motionInterventionLogDao().insert(
                        MotionInterventionLog(
                            packageName = bypassedPackage,
                            triggerType = "MOTION_LOCK",
                            interventionTimestamp = System.currentTimeMillis(),
                            userResponse = "DISMISSED"
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}