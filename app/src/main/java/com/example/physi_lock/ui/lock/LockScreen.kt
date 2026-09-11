package com.example.physi_lock.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.CardCream
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.TertiaryTan
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

/** 2026-09-10 re-theme: this was the app's one remaining screen still using a hardcoded
 *  dark-blue background/raw white text/no icons/no Nunito -- every other screen (including
 *  its own siblings [ModeLockScreen]/[BedtimeLockScreen]) already uses the real light theme
 *  tokens (BackgroundLight/CardCream/DeepOlive/Nunito), matching the Figma "Lock Screens"
 *  export which specifies bg #FEFEFE / card #F5F3EB for the plain challenge lock (only
 *  Focus/Deep Work and Bedtime's violation states get a dark full-screen treatment). */
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
            .background(BackgroundLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(CardCream, RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = DeepOlive,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Physi-Lock Active",
            fontFamily = Nunito,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepOlive,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = challengePromptText[challengeType] ?: "Complete the challenge to unlock",
            fontFamily = Nunito,
            fontSize = 14.sp,
            color = MutedText,
            textAlign = TextAlign.Center
        )

        if (isAdaptive) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Difficulty adapted to your current risk level",
                fontFamily = Nunito,
                fontSize = 12.sp,
                color = MutedText.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (activeSensitivity != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardCream, RoundedCornerShape(22.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ChallengeProgressContent(
                    challengeType = challengeType,
                    sensitivity = activeSensitivity,
                    ringColor = SageAccent,
                    trackColor = TertiaryTan,
                    textColor = DeepOlive,
                    secondaryTextColor = MutedText,
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
}
