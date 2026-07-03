package com.prototype.physi_lock.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.prototype.physi_lock.service.AppMonitorService
import com.prototype.physi_lock.ui.screens.dashboard.ChallengeCompleteScreen
import com.prototype.physi_lock.ui.screens.dashboard.MotionChallengeScreen
import com.prototype.physi_lock.ui.screens.dashboard.motionChallenges
import com.prototype.physi_lock.ui.theme.PhysiLockTheme

class LockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lockedAppName = intent.getStringExtra(EXTRA_APP_NAME) ?: "This app"

        setContent {
            PhysiLockTheme {
                var completed by remember { mutableStateOf(false) }
                val challenge = remember { motionChallenges.random() }

                if (completed) {
                    ChallengeCompleteScreen(
                        unlockedAppName = lockedAppName,
                        onOpenApp = {
                            AppMonitorService.triggerGlobalUnlock()
                            finish()
                        }
                    )
                } else {
                    MotionChallengeScreen(
                        challenge = challenge,
                        onComplete = { completed = true }
                    )
                }
            }
        }
    }
}
