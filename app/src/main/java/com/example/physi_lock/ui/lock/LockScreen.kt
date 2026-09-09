package com.example.physi_lock.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.entity.MotionInterventionLog
import com.example.physi_lock.data.db.PhysiLockDatabase
import com.example.physi_lock.ml.RiskFeatureExtractor
import com.example.physi_lock.ml.RiskScoringEngine
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.sensor.ChallengeType
import com.example.physi_lock.sensor.isActivityRecognitionGranted
import com.example.physi_lock.service.AppMonitorService
import com.example.physi_lock.ui.challenge.ChallengeProgressContent
import kotlinx.coroutines.launch

private val challengePromptText = mapOf(
    ChallengeType.ARM_SWING_FRONT_BACK to "Swing your arm front to back to unlock",
    ChallengeType.ARM_FULL_ROTATION to "Rotate your arm in a full circle to unlock",
    ChallengeType.ARM_BICEP_CURL to "Curl your arm to unlock",
    ChallengeType.ARM_SIDE_RAISE to "Raise your arm out to the side to unlock",
    ChallengeType.ARM_SWAY to "Sway your arm to unlock",
    ChallengeType.ARM_STRETCH to "Stretch your arm to unlock",
    ChallengeType.WALK to "Walk to unlock",
    ChallengeType.RUN_JOG to "Jog in place to unlock"
)

@Composable
fun LockScreen(packageName: String, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { PhysiLockDatabase.getInstance(context) }

    var sensitivity by remember { mutableStateOf<ChallengeSensitivity?>(null) }
    var isAdaptive by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val cfg = try { db.userConfigurationDao().getActiveConfigurationOnce() } catch (e: Exception) { null }
        val rule = try { db.appLockRuleDao().getRuleOnce(packageName) } catch (e: Exception) { null }

        // Trigger Adaptive Lock: apps flagged ADAPTIVE in App Lock Rules scale
        // challenge difficulty off the real Module 2 risk score instead of the
        // static Motion Lock Sensitivity setting. Falls back to that static
        // setting if risk scoring fails for any reason (e.g. no usage logged yet).
        val adaptiveSensitivity = if (rule?.lockType == "ADAPTIVE") {
            try {
                val features = RiskFeatureExtractor(context).extractTodayFeatures()
                val level = RiskScoringEngine.score(features).level
                ChallengeSensitivity.fromRiskLevel(level)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        isAdaptive = adaptiveSensitivity != null
        sensitivity = adaptiveSensitivity ?: ChallengeSensitivity.fromLabel(cfg?.motionLockSensitivity)
    }

    // One random challenge per lock event; rememberSaveable so a rotation or
    // process death mid-attempt doesn't re-roll it. Two-stage pick (category, then
    // arm variant) keeps Walk/Jog/Arm at roughly equal odds -- a flat
    // ChallengeType.entries.random() would make the arm category 6x more likely than
    // Walk or Jog alone now that it has 6 variants. Walk/Jog need ACTIVITY_RECOGNITION
    // — if it isn't granted, fall back to a random arm variant (always available, no
    // extra permission) rather than blocking this forced, no-back-button screen on a
    // permission dialog.
    var challengeType by rememberSaveable {
        val pick = when (listOf("WALK", "RUN_JOG", "ARM").random()) {
            "WALK" -> ChallengeType.WALK
            "RUN_JOG" -> ChallengeType.RUN_JOG
            else -> ChallengeType.ARM_VARIANTS.random()
        }
        val needsStepSensor = pick == ChallengeType.WALK || pick == ChallengeType.RUN_JOG
        mutableStateOf(
            if (needsStepSensor && !isActivityRecognitionGranted(context)) {
                ChallengeType.ARM_VARIANTS.random()
            } else {
                pick
            }
        )
    }

    val activeSensitivity = sensitivity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A2540)), // Deep dark blue tech accent background
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Physi-Lock Active",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = challengePromptText[challengeType] ?: "Complete the challenge to unlock",
            color = Color.LightGray,
            fontSize = 14.sp
        )
        if (isAdaptive) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Difficulty adapted to your current risk level",
                color = Color.LightGray.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(40.dp))

        if (activeSensitivity != null) {
            ChallengeProgressContent(
                challengeType = challengeType,
                sensitivity = activeSensitivity,
                onComplete = {
                    AppMonitorService.grantTemporaryUnlock(packageName, AppMonitorService.CHALLENGE_UNLOCK_DURATION_MS)
                    coroutineScope.launch {
                        try {
                            db.motionInterventionLogDao().insert(
                                MotionInterventionLog(
                                    packageName = packageName,
                                    triggerType = "MOTION_LOCK",
                                    interventionTimestamp = System.currentTimeMillis(),
                                    userResponse = "UNLOCKED",
                                    challengeType = challengeType.name,
                                    xpEarned = challengeType.xpReward
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    onUnlocked()
                }
            )
        }
    }
}
