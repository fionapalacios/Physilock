package com.example.physi_lock.ui.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.sensor.RotationalArmDetector
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage
import kotlinx.coroutines.delay

/** Ported from the teammate's Figma "DeepWorkPage" (2026-09-04). The mockup's "🤳 Tap to
 *  Simulate Shake" button is replaced with the real thing -- [RotationalArmDetector] over
 *  the actual accelerometer+gyroscope, same detector Move-to-Unlock challenges use, fixed
 *  at exactly 5 reps regardless of the user's Motion Lock Sensitivity setting (this is a
 *  safety confirmation gate, not a difficulty-scaled challenge). See [DeepWorkSession] /
 *  [DeepWorkViewModel] / AppMonitorService.deepWorkBlockedPackages for the real session/
 *  enforcement backend. */
private val DeepWorkBackground = Color(0xFF0F1A09)
private val DeepWorkSheetBackground = Color(0xFF1A2710)
private val DeepWorkExitGateReps = 5

private val deepWorkMessages = listOf(
    "Deep work is your competitive advantage. Protect it.",
    "Every minute of focus compounds into mastery.",
    "The world is distracted. You are not.",
    "Great work is done in silence, not in scroll.",
    "Block the noise. Build something worthy."
)

private fun formatCountdown(secs: Long): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}

@Composable
fun DeepWorkScreen(
    elapsedSeconds: Long,
    durationSecs: Int,
    blockedApps: List<String>,
    onEndClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showExitFlow by remember { mutableStateOf(false) }
    var msgIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            msgIndex = (msgIndex + 1) % deepWorkMessages.size
        }
    }

    val remainingSecs = (durationSecs - elapsedSeconds).coerceAtLeast(0)
    val done = remainingSecs <= 0
    val progress = (elapsedSeconds.toFloat() / durationSecs.toFloat()).coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxSize().background(DeepWorkBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(SageAccent, CircleShape))
                    Text(
                        text = "DEEP WORK ACTIVE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SageAccent
                    )
                }
                if (!done) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Exit Deep Work",
                        tint = SageAccent.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp).clickable { showExitFlow = true }
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PulsingFocusRing(progress = progress, done = done, remainingSecs = remainingSecs)

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(targetState = msgIndex, label = "deepWorkMessage") { index ->
                    Text(
                        text = "\"${deepWorkMessages[index]}\"",
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondarySage,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ALL SOCIAL & ENTERTAINMENT BLOCKED",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MutedText.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(9.dp))
                DeepWorkBlockedAppsCloud(blockedApps)

                Spacer(modifier = Modifier.height(15.dp))

                if (!done) {
                    Row(
                        modifier = Modifier
                            .background(DeepOlive.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = MutedText, modifier = Modifier.size(14.dp))
                        Text(text = "Shake your device $DeepWorkExitGateReps× to exit early", fontFamily = Nunito, fontSize = 12.sp, color = MutedText)
                    }
                }
            }

            if (done) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)) {
                    Text(
                        text = "Deep Work session complete! 🎉",
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SageAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SageAccent, RoundedCornerShape(16.dp))
                            .clickable { onEndClick() }
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "End Session", fontFamily = Nunito, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = BackgroundLight)
                    }
                }
            }
        }

        if (showExitFlow) {
            DeepWorkExitSheet(
                onConfirmedExit = {
                    showExitFlow = false
                    onEndClick()
                },
                onCancel = { showExitFlow = false }
            )
        }
    }
}

@Composable
private fun PulsingFocusRing(progress: Float, done: Boolean, remainingSecs: Long) {
    val infiniteTransition = rememberInfiniteTransition(label = "deepWorkPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(animation = tween(5000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(animation = tween(5000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(DeepOlive.copy(alpha = pulseAlpha), CircleShape)
        )
        Canvas(modifier = Modifier.size(124.dp)) {
            val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = DeepOlive.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            if (progress > 0f) {
                drawArc(
                    color = DeepOlive,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = SageAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        if (done) {
            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = SageAccent, modifier = Modifier.size(36.dp))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatCountdown(remainingSecs),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BackgroundLight
                )
                Text(text = "remaining", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SageAccent)
            }
        }
    }
}

@Composable
private fun DeepWorkBlockedAppsCloud(apps: List<String>) {
    if (apps.isEmpty()) {
        Text(
            text = "No apps categorized as Social Media/Entertainment yet — ask your Admin to categorize apps.",
            fontFamily = Nunito,
            fontSize = 12.sp,
            color = SecondarySage,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        return
    }
    val shown = apps.take(8)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        shown.chunked(3).forEach { rowApps ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowApps.forEach { name -> DeepWorkAppChip(name) }
            }
        }
        if (apps.size > shown.size) {
            Text(text = "+${apps.size - shown.size} more", fontFamily = Nunito, fontSize = 11.sp, color = MutedText)
        }
    }
}

@Composable
private fun DeepWorkAppChip(name: String) {
    Row(
        modifier = Modifier
            .background(BackgroundLight.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = MutedText, modifier = Modifier.size(10.dp))
        Text(text = name, fontFamily = Nunito, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SecondarySage.copy(alpha = 0.6f))
    }
}

private enum class ExitGateStep { PROMPT, SHAKING, DONE }

@Composable
private fun DeepWorkExitSheet(onConfirmedExit: () -> Unit, onCancel: () -> Unit) {
    var step by remember { mutableStateOf(ExitGateStep.PROMPT) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = step == ExitGateStep.PROMPT, onClick = onCancel),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepWorkSheetBackground, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            when (step) {
                ExitGateStep.PROMPT -> {
                    Text(text = "Exit Deep Work?", fontFamily = Nunito, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BackgroundLight)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This mode requires a real shake gesture to exit early. Shake your phone $DeepWorkExitGateReps times to confirm you want to leave.",
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        color = SageAccent,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SageAccent, RoundedCornerShape(16.dp))
                            .clickable { step = ExitGateStep.SHAKING }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Begin Shake Gesture", fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BackgroundLight)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundLight.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                            .clickable(onClick = onCancel)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Stay in Deep Work", fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SageAccent)
                    }
                }
                ExitGateStep.SHAKING -> {
                    ShakeGateContent(
                        onComplete = { step = ExitGateStep.DONE },
                        onCancel = { step = ExitGateStep.PROMPT }
                    )
                }
                ExitGateStep.DONE -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = SageAccent, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Session ended early", fontFamily = Nunito, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BackgroundLight)
                    }
                    LaunchedEffect(Unit) {
                        delay(900)
                        onConfirmedExit()
                    }
                }
            }
        }
    }
}

@Composable
private fun ShakeGateContent(onComplete: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var shakeCount by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val detector = RotationalArmDetector(
            context = context,
            sensitivity = ChallengeSensitivity.MODERATE,
            onProgress = { count -> shakeCount = count },
            onComplete = onComplete,
            repsOverride = DeepWorkExitGateReps
        )
        detector.start()
        onDispose { detector.stop() }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "SHAKE $shakeCount / $DeepWorkExitGateReps",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = SageAccent,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 20.dp)) {
            repeat(DeepWorkExitGateReps) { index ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (index < shakeCount) SageAccent else BackgroundLight.copy(alpha = 0.1f),
                            RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        tint = if (index < shakeCount) BackgroundLight else MutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Text(
            text = "Shake your phone — a real accelerometer + gyroscope gate, not a tap.",
            fontFamily = Nunito,
            fontSize = 12.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = "Cancel",
            fontFamily = Nunito,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MutedText,
            modifier = Modifier.clickable(onClick = onCancel)
        )
    }
}

/** Drives [DeepWorkScreen] off a real, persisted [DeepWorkViewModel] session -- same
 *  survives-navigation/process-death pattern as [FocusModeRoute]. [durationSecs] only
 *  applies to a session actually started here; re-entering mid-session keeps the
 *  original duration. */
@Composable
fun DeepWorkRoute(
    onEndClick: () -> Unit,
    durationSecs: Int = 60 * 60,
    modifier: Modifier = Modifier,
    viewModel: DeepWorkViewModel = viewModel()
) {
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val blockedApps by viewModel.blockedAppNames.collectAsState()
    val session by viewModel.activeSession.collectAsState()

    BackHandler {
        // Same "no silent exit" stance as Focus Mode -- surface the real shake gate
        // via the on-screen X, don't let system back skip it.
    }

    LaunchedEffect(Unit) {
        viewModel.startSessionIfNeeded(durationSecs)
    }

    val actualDuration = session?.durationSecs ?: durationSecs

    DeepWorkScreen(
        elapsedSeconds = elapsedSeconds,
        durationSecs = actualDuration,
        blockedApps = blockedApps,
        onEndClick = {
            val endedEarly = elapsedSeconds < actualDuration
            viewModel.endSession(endedEarly)
            onEndClick()
        },
        modifier = modifier
    )
}
