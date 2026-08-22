package com.example.physi_lock.ui.challenge

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.sensor.ChallengeDetector
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.sensor.ChallengeType
import com.example.physi_lock.sensor.RotationalArmDetector
import com.example.physi_lock.sensor.StepChallengeDetector
import com.example.physi_lock.sensor.StepChallengeMode
import com.example.physi_lock.ui.components.CircularProgressRing

// Sensor-driving + progress-ring UI shared by the forced lock-trigger screen
// (LockScreen, full-screen/no-back) and the voluntary Challenges hub
// (MoveScreen, cancellable) — only the surrounding chrome differs between the
// two callers; this composable is cancellation-unaware, callers stop rendering
// it (triggering onDispose) to cancel.
@Composable
fun ChallengeProgressContent(
    challengeType: ChallengeType,
    sensitivity: ChallengeSensitivity,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    ringColor: Color = Color(0xFF4CAF50),
    trackColor: Color = Color.White.copy(alpha = 0.1f),
    textColor: Color = Color.White,
    secondaryTextColor: Color = Color.LightGray
) {
    val context = LocalContext.current

    var stepCount by remember { mutableIntStateOf(0) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var cadence by remember { mutableIntStateOf(0) }

    DisposableEffect(challengeType, sensitivity) {
        val detector: ChallengeDetector = when (challengeType) {
            ChallengeType.ROTATIONAL_ARM -> RotationalArmDetector(
                context = context,
                sensitivity = sensitivity,
                onProgress = { count -> stepCount = count },
                onComplete = onComplete
            )
            ChallengeType.WALK -> StepChallengeDetector(
                context = context,
                mode = StepChallengeMode.WALK,
                sensitivity = sensitivity,
                onProgress = { steps, _, _ -> stepCount = steps },
                onComplete = onComplete
            )
            ChallengeType.RUN_JOG -> StepChallengeDetector(
                context = context,
                mode = StepChallengeMode.JOG,
                sensitivity = sensitivity,
                onProgress = { steps, elapsed, stepsPerMin -> stepCount = steps; elapsedMs = elapsed; cadence = stepsPerMin },
                onComplete = onComplete
            )
        }
        detector.start()
        onDispose { detector.stop() }
    }

    val (progress, primaryText, secondaryText) = when (challengeType) {
        ChallengeType.ROTATIONAL_ARM -> Triple(
            stepCount.toFloat() / sensitivity.armRepsRequired.toFloat(),
            "$stepCount",
            "of ${sensitivity.armRepsRequired} moves"
        )
        ChallengeType.WALK -> Triple(
            stepCount.toFloat() / sensitivity.walkStepsRequired.toFloat(),
            "$stepCount",
            "of ${sensitivity.walkStepsRequired} steps"
        )
        ChallengeType.RUN_JOG -> Triple(
            elapsedMs.toFloat() / sensitivity.jogDurationMs.toFloat(),
            formatMmSs(elapsedMs),
            "of ${formatMmSs(sensitivity.jogDurationMs)} · $cadence/${sensitivity.jogMinStepsPerMin} steps/min"
        )
    }

    CircularProgressRing(
        progress = progress,
        trackColor = trackColor,
        progressColor = ringColor,
        ringSize = 200.dp,
        strokeWidth = 12.dp,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = primaryText, color = textColor, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text(
                text = secondaryText,
                color = secondaryTextColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatMmSs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
