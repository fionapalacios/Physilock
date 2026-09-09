package com.example.physi_lock.ui.lock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.physi_lock.service.AppMonitorService
import com.example.physi_lock.ui.theme.PhysiLockTheme

/** Generic real full-screen violation overlay (2026-09-06) for modes that previously just
 *  silently kicked the user to the home screen -- Focus Mode (`handleFocusBlock`) and Deep
 *  Work Mode (the `deepWorkActive` branch), both in `AppMonitorService.handleWindowStateChange`.
 *  Same shape as [BedtimeLockActivity]/[BedtimeLockScreen] (that screen isn't reused directly
 *  to avoid touching an already-shipped, working screen), just parameterized by title/message
 *  so one screen serves both trigger types. Purely informational, no bypass surfaced here --
 *  ending the session (Focus) or the shake-exit gate (Deep Work) stays the only way in, exactly
 *  as before this overlay existed; this only replaces the silent home-kick + notification with
 *  an honest explanation of why the app is blocked. */
class ModeLockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(AppMonitorService.EXTRA_PACKAGE_NAME) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "App Blocked"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "This app is currently blocked."
        val hint = intent.getStringExtra(EXTRA_HINT)
        setContent {
            PhysiLockTheme {
                ModeLockScreen(
                    packageName = packageName,
                    title = title,
                    message = message,
                    hint = hint,
                    onGoHome = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_TITLE = "extra_mode_lock_title"
        const val EXTRA_MESSAGE = "extra_mode_lock_message"
        const val EXTRA_HINT = "extra_mode_lock_hint"
    }
}
