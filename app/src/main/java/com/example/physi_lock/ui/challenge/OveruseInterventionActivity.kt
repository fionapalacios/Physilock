package com.example.physi_lock.ui.challenge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.physi_lock.ml.RiskLevel
import com.example.physi_lock.service.AppMonitorService
import com.example.physi_lock.ui.theme.PhysiLockTheme

/** Risk-tiered motion-challenge intervention (2026-09-06) -- the manuscript describes
 *  reflection prompts and motion-responsive unlocking as one integrated flow, triggered
 *  by both the doomscroll classifier and the Random Forest risk model. This is that real
 *  implementation: launched either from `AppMonitorService.checkDoomscrolling` (via
 *  [com.example.physi_lock.ui.reflection.DoomscrollReflectionActivity]'s "Take a Break")
 *  or directly from Home's "tap an over-limit app" row. [EXTRA_RISK_TIER] is supplied by
 *  the caller rather than recomputed here -- AppMonitorService already has a live cached
 *  risk level for the doomscroll path, and HomeViewModel already computes its own for the
 *  risk ring; recomputing a third time here would be redundant. Same
 *  startActivity(NEW_TASK|CLEAR_TASK) launch mechanism as [com.example.physi_lock.ui.lock.LockActivity]. */
class OveruseInterventionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(AppMonitorService.EXTRA_PACKAGE_NAME) ?: ""
        val riskTier = when (intent.getStringExtra(EXTRA_RISK_TIER)?.uppercase()) {
            "HIGH" -> RiskLevel.HIGH
            "LOW" -> RiskLevel.LOW
            else -> RiskLevel.MODERATE
        }
        setContent {
            PhysiLockTheme {
                OveruseInterventionScreen(
                    packageName = packageName,
                    riskTier = riskTier,
                    onUnlocked = {
                        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_RISK_TIER = "extra_overuse_risk_tier"
    }
}
