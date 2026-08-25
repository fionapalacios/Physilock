package com.example.physi_lock.ui.challenge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.sensor.ChallengeSensitivity
import com.example.physi_lock.sensor.ChallengeType
import com.example.physi_lock.ui.components.CircularProgressRing
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SoftSand
import com.example.physi_lock.ui.theme.TertiaryTan

/**
 * Visual design ported from the teammate's sprint-2-ui-navigation branch
 * (ChallengeActiveScreen.kt). Their version fakes progress entirely — time-based
 * challenges just run a 1-second delay loop, and the one rep-based challenge is
 * a manual "tap per rep" button — neither reads any sensor. This keeps the real
 * accelerometer/step-sensor detection this repo already had (ChallengeProgressContent,
 * RotationalArmDetector/StepChallengeDetector, runtime-verified on-device in Module 4)
 * underneath the new visuals, same "their design, our data layer" pattern used
 * elsewhere. [onClaim] only fires after the real detector reports completion.
 */
private data class ChallengeVisual(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val quote: String
)

private fun visualFor(type: ChallengeType): ChallengeVisual = when (type) {
    ChallengeType.RUN_JOG -> ChallengeVisual(
        title = "Run / Jog",
        description = "Keep a steady jogging cadence — your phone's motion sensors track it live.",
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        accentColor = Orchid,
        quote = "Your body is asking for this. Trust it."
    )
    ChallengeType.WALK -> ChallengeVisual(
        title = "5-Minute Walk",
        description = "Walk at a steady pace — real steps counted via your device's step sensor.",
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        accentColor = SageAccent,
        quote = "One step at a time — that's all it takes."
    )
    ChallengeType.ROTATIONAL_ARM -> ChallengeVisual(
        title = "Rotational Arm Movements",
        description = "Rotate your arm through the full motion — detected via accelerometer and gyroscope.",
        icon = Icons.Default.Bolt,
        accentColor = DeepOlive,
        quote = "Motion is the medicine. Keep moving."
    )
}

@Composable
fun ChallengeActiveScreen(
    challengeType: ChallengeType,
    sensitivity: ChallengeSensitivity,
    appName: String,
    onBackClick: () -> Unit,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCompleted by remember(challengeType) { mutableStateOf(false) }
    val visual = visualFor(challengeType)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 11.25.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = DeepOlive,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onBackClick)
            )
            Text(
                text = "CHALLENGE ACTIVE",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 19.5.sp,
                color = SageAccent
            )
            Spacer(modifier = Modifier.size(18.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 22.5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = visual.title,
                textAlign = TextAlign.Center,
                fontFamily = Nunito,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 25.3.sp,
                color = DeepOlive
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = visual.description,
                textAlign = TextAlign.Center,
                fontFamily = Nunito,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = DeepOlive
            )

            Spacer(modifier = Modifier.height(18.75.dp))

            Box(
                modifier = Modifier
                    .size(170.dp)
                    .background(visual.accentColor.copy(alpha = 0.09f), RoundedCornerShape(22.5.dp))
                    .border(1.06.dp, visual.accentColor.copy(alpha = 0.27f), RoundedCornerShape(22.5.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = visual.accentColor,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(7.5.dp))
                        Text(
                            text = "Done!",
                            fontFamily = Nunito,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 19.5.sp,
                            color = DeepOlive
                        )
                    }
                } else {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.accentColor,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.75.dp))

            if (isCompleted) {
                CircularProgressRing(
                    progress = 1f,
                    trackColor = TertiaryTan,
                    progressColor = visual.accentColor,
                    ringSize = 120.dp,
                    strokeWidth = 7.dp
                ) {
                    Text(
                        text = "Complete!",
                        fontFamily = Nunito,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 19.5.sp,
                        color = visual.accentColor
                    )
                }
            } else {
                // Stops rendering (triggering the detector's onDispose) once complete,
                // per ChallengeProgressContent's own doc comment on caller-driven cancellation.
                ChallengeProgressContent(
                    challengeType = challengeType,
                    sensitivity = sensitivity,
                    onComplete = { isCompleted = true },
                    ringColor = visual.accentColor,
                    trackColor = TertiaryTan,
                    textColor = DeepOlive,
                    secondaryTextColor = DeepOlive
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftSand, RoundedCornerShape(15.dp))
                    .border(1.06.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
                    .padding(horizontal = 18.75.dp, vertical = 15.dp)
            ) {
                Text(
                    text = "\"${visual.quote}\"",
                    textAlign = TextAlign.Center,
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.4.sp,
                    color = DeepOlive,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.75.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(visual.accentColor.copy(alpha = 0.09f), RoundedCornerShape(19.dp))
                    .border(1.06.dp, visual.accentColor.copy(alpha = 0.27f), RoundedCornerShape(19.dp))
                    .padding(horizontal = 15.dp, vertical = 9.38.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.5.dp)
            ) {
                Text(text = "🎁", fontSize = 14.sp)
                Text(
                    text = "Unlocks $appName for 20 min",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    color = DeepOlive
                )
            }

            if (isCompleted) {
                Spacer(modifier = Modifier.height(22.5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(15.dp),
                            ambientColor = DeepOlive.copy(alpha = 0.25f),
                            spotColor = DeepOlive.copy(alpha = 0.25f)
                        )
                        .background(DeepOlive, RoundedCornerShape(15.dp))
                        .clickable(onClick = onClaim)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Claim +${challengeType.xpReward} XP 🎉",
                        textAlign = TextAlign.Center,
                        fontFamily = Nunito,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.5.sp,
                        color = BackgroundLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.5.dp))
        }
    }
}
