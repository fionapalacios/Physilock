package com.prototype.physi_lock.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.ui.components.CircularProgressRing
import com.prototype.physi_lock.ui.theme.AccentLavender
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryGreen
import com.prototype.physi_lock.ui.theme.SecondarySage
import kotlinx.coroutines.delay

private val ChallengeBackground = Color(0xFF1F2A14)

data class MotionChallenge(
    val title: String,
    val emoji: String,
    val accentColor: Color,
    val durationSeconds: Int
)

val motionChallenges = listOf(
    MotionChallenge("Take a 2-min Walk", "🚶", PrimaryGreen, durationSeconds = 120),
    MotionChallenge("Jog in Place — 30 sec", "🏃", AccentLavender, durationSeconds = 30),
    MotionChallenge("Arm Shake Burst", "💪", DeepOlive, durationSeconds = 20)
)

@Composable
fun MotionChallengeScreen(
    challenge: MotionChallenge,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var secondsLeft by remember(challenge) { mutableIntStateOf(challenge.durationSeconds) }

    LaunchedEffect(challenge) {
        while (secondsLeft > 0) {
            delay(1_000L)
            secondsLeft--
        }
        onComplete()
    }

    val progress = 1f - secondsLeft.toFloat() / challenge.durationSeconds.toFloat()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChallengeBackground)
            .padding(horizontal = 22.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CHALLENGE ACTIVE",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 19.5.sp,
                color = PrimaryGreen
            )

            Spacer(modifier = Modifier.height(7.5.dp))

            Text(
                text = challenge.title,
                textAlign = TextAlign.Center,
                fontFamily = NunitoFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 27.sp,
                color = BackgroundLight
            )

            Spacer(modifier = Modifier.height(22.5.dp))

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(challenge.accentColor.copy(alpha = 0.09f), RoundedCornerShape(22.5.dp))
                    .border(1.06.dp, challenge.accentColor.copy(alpha = 0.20f), RoundedCornerShape(22.5.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = challenge.emoji, fontSize = 64.sp)
            }

            Spacer(modifier = Modifier.height(22.5.dp))

            CircularProgressRing(
                progress = progress,
                trackColor = PrimaryGreen.copy(alpha = 0.15f),
                progressColor = challenge.accentColor,
                ringSize = 120.dp,
                strokeWidth = 7.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$secondsLeft",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 26.sp,
                        color = BackgroundLight
                    )
                    Text(
                        text = "left",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.sp,
                        color = PrimaryGreen
                    )
                }
            }
        }
    }
}

@Composable
fun ChallengeCompleteScreen(
    unlockedAppName: String,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChallengeBackground)
            .padding(horizontal = 22.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Challenge Complete!",
                textAlign = TextAlign.Center,
                fontFamily = NunitoFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 36.sp,
                color = BackgroundLight
            )

            Spacer(modifier = Modifier.height(7.5.dp))

            Text(
                text = "$unlockedAppName is unlocked for 20 minutes. Use it mindfully 🌿",
                textAlign = TextAlign.Center,
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 22.4.sp,
                color = SecondarySage
            )

            Spacer(modifier = Modifier.height(18.75.dp))

            Box(
                modifier = Modifier
                    .background(PrimaryGreen.copy(alpha = 0.13f), RoundedCornerShape(15.dp))
                    .border(1.06.dp, PrimaryGreen.copy(alpha = 0.27f), RoundedCornerShape(15.dp))
                    .padding(horizontal = 18.75.dp, vertical = 11.25.dp)
            ) {
                Text(
                    text = "+20 XP earned · Streak maintained 🔥",
                    textAlign = TextAlign.Center,
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    color = PrimaryGreen
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryGreen, RoundedCornerShape(15.dp))
                    .clickable(onClick = onOpenApp)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open $unlockedAppName",
                    textAlign = TextAlign.Center,
                    fontFamily = NunitoFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.5.sp,
                    color = BackgroundLight
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MotionChallengeScreenPreview() {
    PhysiLockTheme {
        MotionChallengeScreen(challenge = motionChallenges[0], onComplete = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ChallengeCompleteScreenPreview() {
    PhysiLockTheme {
        ChallengeCompleteScreen(unlockedAppName = "Instagram", onOpenApp = {})
    }
}
