package com.example.physi_lock.ui.challenge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.db.PhysiLockDatabase
import com.example.physi_lock.data.entity.MotionInterventionLog
import com.example.physi_lock.ml.RiskLevel
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.sensor.RotationalArmDetector
import com.example.physi_lock.sensor.StepChallengeDetector
import com.example.physi_lock.sensor.StepChallengeMode
import com.example.physi_lock.service.AppMonitorService
import com.example.physi_lock.ui.components.CircularProgressRing
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.ErrorRed
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OveruseBackground = Color(0xFF1F2A14)

private data class OveruseStep(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val targetReps: Int,
    val instruction: String
)

/** Risk tier -> motion-step sequence, per the manuscript-backed mockup: LOW is a single
 *  quick motion, MODERATE chains two, HIGH adds a mandatory cooldown before a longer
 *  sequence. "Shake" and "Rotate" both run on [RotationalArmDetector] -- there's no
 *  separate sensor for them, they're the same accelerometer+gyroscope arm motion the
 *  rest of this app already uses for App Lock Rules/Deep Work's exit gate, just
 *  relabeled per step for the mockup's copy. */
private fun stepsForTier(tier: RiskLevel): List<OveruseStep> = when (tier) {
    RiskLevel.LOW -> listOf(
        OveruseStep("shake", "Shake", Icons.Filled.PhoneAndroid, 3, "Shake your phone briskly side-to-side")
    )
    RiskLevel.MODERATE -> listOf(
        OveruseStep("shake", "Shake", Icons.Filled.PhoneAndroid, 3, "Shake your phone briskly 3 times"),
        OveruseStep("rotate", "Rotate", Icons.Filled.Bolt, 3, "Rotate your wrist or flip the phone 3 times")
    )
    RiskLevel.HIGH -> listOf(
        OveruseStep("shake", "Shake", Icons.Filled.PhoneAndroid, 8, "Shake your phone 8 times"),
        OveruseStep("walk", "Walk", Icons.AutoMirrored.Filled.DirectionsWalk, 7, "Walk or step in place 7 times")
    )
}

private fun xpForTier(tier: RiskLevel): Int = when (tier) {
    RiskLevel.LOW -> 15
    RiskLevel.MODERATE -> 30
    RiskLevel.HIGH -> 50
}

private data class RiskMeta(val label: String, val color: Color)

private fun riskMetaFor(tier: RiskLevel): RiskMeta = when (tier) {
    RiskLevel.LOW -> RiskMeta("LOW RISK", SageAccent)
    RiskLevel.MODERATE -> RiskMeta("MODERATE RISK", Orchid)
    RiskLevel.HIGH -> RiskMeta("HIGH RISK", ErrorRed)
}

private const val HIGH_TIER_COOLDOWN_SECS = 300

private fun formatCooldown(secs: Int): String = "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}"

/** Real content for [OveruseInterventionActivity]. Unlike the comparison mockup's "tap to
 *  register" buttons, each step here is driven by the actual sensor detector
 *  ([RotationalArmDetector]/[StepChallengeDetector]) -- same real-motion standard this
 *  project holds everywhere else (App Lock Rules, Deep Work's exit gate). "Emergency
 *  Access" is always available (per the mockup's own "always visible, per UX rules" note)
 *  and grants no unlock -- it only dismisses and logs the bypass, same bypass-logging
 *  pattern [com.example.physi_lock.ui.lock.LockActivity] already uses. */
@Composable
fun OveruseInterventionScreen(
    packageName: String,
    riskTier: RiskLevel,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { PhysiLockDatabase.getInstance(context) }
    val appName = remember(packageName) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }

    val steps = remember(riskTier) { stepsForTier(riskTier) }
    val meta = remember(riskTier) { riskMetaFor(riskTier) }

    var cooldownActive by remember { mutableStateOf(riskTier == RiskLevel.HIGH) }
    var cooldownSecs by remember { mutableIntStateOf(HIGH_TIER_COOLDOWN_SECS) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(cooldownActive) {
        while (cooldownActive && cooldownSecs > 0) {
            delay(1000)
            cooldownSecs -= 1
        }
        if (cooldownSecs <= 0) cooldownActive = false
    }

    LaunchedEffect(done) {
        if (!done) return@LaunchedEffect
        AppMonitorService.grantTemporaryUnlock(packageName, AppMonitorService.CHALLENGE_UNLOCK_DURATION_MS)
        try {
            db.motionInterventionLogDao().insert(
                MotionInterventionLog(
                    packageName = packageName,
                    triggerType = "OVERUSE_INTERVENTION",
                    interventionTimestamp = System.currentTimeMillis(),
                    userResponse = "UNLOCKED",
                    xpEarned = xpForTier(riskTier)
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logBypassAndDismiss() {
        coroutineScope.launch {
            try {
                db.motionInterventionLogDao().insert(
                    MotionInterventionLog(
                        packageName = packageName,
                        triggerType = "OVERUSE_INTERVENTION",
                        interventionTimestamp = System.currentTimeMillis(),
                        userResponse = "BYPASSED"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize().background(OveruseBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(meta.color.copy(alpha = 0.13f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = meta.label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = meta.color
                    )
                }
                if (!done) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = SageAccent.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp).clickable { logBypassAndDismiss() }
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    cooldownActive -> CooldownContent(appName = appName, secsRemaining = cooldownSecs)
                    !done -> {
                        val step = steps[currentStepIndex]
                        StepContent(
                            step = step,
                            stepNumber = currentStepIndex + 1,
                            totalSteps = steps.size,
                            appName = appName,
                            tint = meta.color,
                            onStepComplete = {
                                if (currentStepIndex < steps.size - 1) {
                                    currentStepIndex += 1
                                } else {
                                    done = true
                                }
                            }
                        )
                    }
                    else -> DoneContent(
                        appName = appName,
                        xpEarned = xpForTier(riskTier),
                        onOpenApp = onUnlocked
                    )
                }
            }

            if (!done) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Emergency Access (instant, logged)",
                        fontFamily = Nunito,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ErrorRed,
                        modifier = Modifier.clickable { logBypassAndDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CooldownContent(appName: String, secsRemaining: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "App Locked",
            fontFamily = Nunito,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BackgroundLight,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "High-risk usage triggers a mandatory 5-minute cooldown before unlocking $appName.",
            fontFamily = Nunito,
            fontSize = 14.sp,
            color = SageAccent,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        CircularProgressRing(
            progress = 1f - (secsRemaining.toFloat() / HIGH_TIER_COOLDOWN_SECS),
            trackColor = BackgroundLight.copy(alpha = 0.1f),
            progressColor = ErrorRed,
            ringSize = 110.dp,
            strokeWidth = 8.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatCooldown(secsRemaining),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BackgroundLight
                )
                Text(text = "cooldown", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = SageAccent)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Step away from your screen. After the cooldown, you'll need to complete a physical challenge to unlock the app.",
            fontFamily = Nunito,
            fontSize = 13.sp,
            color = SageAccent.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun StepContent(
    step: OveruseStep,
    stepNumber: Int,
    totalSteps: Int,
    appName: String,
    tint: Color,
    onStepComplete: () -> Unit
) {
    var reps by remember(step) { mutableIntStateOf(0) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(64.dp).background(DeepOlive, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = step.icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Complete the Challenge",
            fontFamily = Nunito,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BackgroundLight,
            textAlign = TextAlign.Center
        )
        Text(
            text = "to unlock $appName",
            fontFamily = Nunito,
            fontSize = 14.sp,
            color = SageAccent,
            textAlign = TextAlign.Center
        )

        if (totalSteps > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (index < stepNumber - 1) SageAccent else if (index == stepNumber - 1) BackgroundLight else BackgroundLight.copy(alpha = 0.2f),
                                CircleShape
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        CircularProgressRing(
            progress = reps.toFloat() / step.targetReps,
            trackColor = BackgroundLight.copy(alpha = 0.1f),
            progressColor = tint,
            ringSize = 130.dp,
            strokeWidth = 9.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Step $stepNumber: ${step.label}",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BackgroundLight,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "$reps / ${step.targetReps}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = step.instruction,
            fontFamily = Nunito,
            fontSize = 13.sp,
            color = SageAccent.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }

    val context = LocalContext.current
    DisposableEffect(step) {
        val detector = if (step.id == "walk") {
            StepChallengeDetector(
                context = context,
                mode = StepChallengeMode.WALK,
                sensitivity = ChallengeSensitivity.MODERATE,
                onProgress = { stepsSoFar, _, _ -> reps = stepsSoFar },
                onComplete = onStepComplete,
                walkStepsOverride = step.targetReps
            )
        } else {
            RotationalArmDetector(
                context = context,
                sensitivity = ChallengeSensitivity.MODERATE,
                onProgress = { count -> reps = count },
                onComplete = onStepComplete,
                repsOverride = step.targetReps
            )
        }
        detector.start()
        onDispose { detector.stop() }
    }
}

@Composable
private fun DoneContent(appName: String, xpEarned: Int, onOpenApp: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = SageAccent, modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Challenge Complete!",
            fontFamily = Nunito,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BackgroundLight,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$appName is unlocked. Use it mindfully.",
            fontFamily = Nunito,
            fontSize = 14.sp,
            color = SageAccent,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .background(SageAccent.copy(alpha = 0.13f), RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                text = "+$xpEarned XP · Unlock logged to report",
                fontFamily = Nunito,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SageAccent
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SageAccent, RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenApp)
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Open $appName",
                fontFamily = Nunito,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BackgroundLight
            )
        }
    }
}
