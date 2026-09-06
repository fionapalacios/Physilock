package com.example.physi_lock.ui.reflection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.physi_lock.service.AppMonitorService
import com.example.physi_lock.ui.challenge.OveruseInterventionActivity
import com.example.physi_lock.ui.theme.PhysiLockTheme

/** Real doomscroll intervention (2026-09-06), replacing the old passive
 *  `postDoomscrollAlertNotification()` (see `AppMonitorService.checkDoomscrolling`) with an
 *  actual full-screen reflection prompt -- the manuscript describes reflection prompts and
 *  motion-responsive unlocking as one integrated flow, triggered by the doomscroll classifier
 *  and the Random Forest risk model together. "5 more minutes" snoozes re-alerting (see
 *  `AppMonitorService.snoozeDoomscrollAlerts`) without blocking anything, since doomscrolling
 *  itself was never a blocking mechanism; "Take a Break" hands off to the same real
 *  [OveruseInterventionActivity] Home's over-limit-app tap uses, for the specific app that
 *  triggered the detection. */
class DoomscrollReflectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(AppMonitorService.EXTRA_PACKAGE_NAME) ?: ""
        val riskTier = intent.getStringExtra(OveruseInterventionActivity.EXTRA_RISK_TIER)

        setContent {
            PhysiLockTheme {
                DoomscrollReflectionScreen(
                    packageName = packageName,
                    onSnooze = {
                        AppMonitorService.snoozeDoomscrollAlerts(DOOMSCROLL_SNOOZE_DURATION_MS)
                        finish()
                    },
                    onTakeBreak = {
                        val overuseIntent = Intent(this, OveruseInterventionActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            putExtra(AppMonitorService.EXTRA_PACKAGE_NAME, packageName)
                            putExtra(OveruseInterventionActivity.EXTRA_RISK_TIER, riskTier)
                        }
                        startActivity(overuseIntent)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        private const val DOOMSCROLL_SNOOZE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    }
}
