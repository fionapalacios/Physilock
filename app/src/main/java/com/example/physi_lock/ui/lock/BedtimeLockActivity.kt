package com.example.physi_lock.ui.lock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.physi_lock.service.AppMonitorService
import com.example.physi_lock.ui.theme.PhysiLockTheme

/** Bedtime Mode's real hard-block lock screen (2026-09-06), replacing the old
 *  performGlobalAction(HOME) + notification-only enforcement (see
 *  AppMonitorService.handleWindowStateChange's Bedtime branch) with an actual full-screen
 *  UI -- same startActivity(NEW_TASK|CLEAR_TASK) mechanism [LockActivity] already uses for
 *  App Lock Rules' challenge-based lock. Styled with the app's real light theme (matching
 *  BedtimeModeScreen and the rest of Settings/Home/Reports) rather than LockScreen's
 *  one-off dark-blue background or a new visual style of its own. Unlike LockActivity,
 *  there's no challenge to complete -- Bedtime is a hard block by design (see
 *  BedtimeModeScreen's own "Apps are locked during Bedtime Mode" copy), so the only
 *  action here is acknowledging and returning home. */
class BedtimeLockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(AppMonitorService.EXTRA_PACKAGE_NAME) ?: ""
        val bedtimeEndMinute = intent.getIntExtra(AppMonitorService.EXTRA_BEDTIME_END_MINUTE, 7 * 60)
        setContent {
            PhysiLockTheme {
                BedtimeLockScreen(
                    packageName = packageName,
                    bedtimeEndMinute = bedtimeEndMinute,
                    onGoHome = { finish() }
                )
            }
        }
    }
}
