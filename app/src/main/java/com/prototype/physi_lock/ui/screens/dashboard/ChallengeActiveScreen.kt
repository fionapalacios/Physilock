package com.prototype.physi_lock.ui.screens.dashboard

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.ui.components.AuthTabsBackground
import com.prototype.physi_lock.ui.components.CircularProgressRing
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen
import kotlinx.coroutines.delay

@Composable
fun ChallengeActiveScreen(
    challenge: MoveChallenge,
    onBackClick: () -> Unit,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    var elapsedSeconds by remember(challenge) { mutableIntStateOf(0) }
    var reps by remember(challenge) { mutableIntStateOf(0) }
    var isCompleted by remember(challenge) { mutableStateOf(false) }

    val progressValue = if (challenge.isTimeBased) elapsedSeconds else reps
    val progress = (progressValue.toFloat() / challenge.targetValue.toFloat()).coerceIn(0f, 1f)

    LaunchedEffect(challenge) {
        if (challenge.isTimeBased) {
            while (elapsedSeconds < challenge.targetValue) {
                delay(1_000L)
                elapsedSeconds++
            }
            isCompleted = true
        }
    }

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
                color = PrimaryGreen
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
                text = challenge.title,
                textAlign = TextAlign.Center,
                fontFamily = NunitoFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 25.3.sp,
                color = PrimaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = challenge.description,
                textAlign = TextAlign.Center,
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = DeepOlive
            )

            Spacer(modifier = Modifier.height(18.75.dp))

            Box(
                modifier = Modifier
                    .size(170.dp)
                    .background(challenge.accentColor.copy(alpha = 0.09f), RoundedCornerShape(22.5.dp))
                    .border(1.06.dp, challenge.accentColor.copy(alpha = 0.27f), RoundedCornerShape(22.5.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = challenge.accentColor,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(7.5.dp))
                        Text(
                            text = "Done!",
                            fontFamily = NunitoFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 19.5.sp,
                            color = PrimaryDark
                        )
                    }
                } else {
                    Icon(
                        imageVector = challenge.icon,
                        contentDescription = null,
                        tint = challenge.accentColor,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.75.dp))

            CircularProgressRing(
                progress = if (isCompleted) 1f else progress,
                trackColor = com.prototype.physi_lock.ui.theme.TertiaryTan,
                progressColor = challenge.accentColor,
                ringSize = 120.dp,
                strokeWidth = 7.dp
            ) {
                if (isCompleted) {
                    Text(
                        text = "Complete!",
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 19.5.sp,
                        color = challenge.accentColor
                    )
                } else if (challenge.isTimeBased) {
                    val remaining = (challenge.targetValue - elapsedSeconds).coerceAtLeast(0)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${remaining / 60}:${"%02d".format(remaining % 60)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 26.sp,
                            color = PrimaryDark
                        )
                        Text(
                            text = "remaining",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = DeepOlive
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row {
                            Text(
                                text = "$reps",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 26.sp,
                                color = PrimaryDark
                            )
                            Text(
                                text = "/${challenge.targetValue}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.sp,
                                color = PrimaryGreen
                            )
                        }
                        Text(
                            text = "reps",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = DeepOlive
                        )
                    }
                }
            }

            if (!challenge.isTimeBased && !isCompleted) {
                Spacer(modifier = Modifier.height(15.dp))
                Row(
                    modifier = Modifier
                        .background(challenge.accentColor, RoundedCornerShape(15.dp))
                        .clickable {
                            reps++
                            if (reps >= challenge.targetValue) isCompleted = true
                        }
                        .padding(horizontal = 30.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = BackgroundLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Tap per Rep",
                        textAlign = TextAlign.Center,
                        fontFamily = NunitoFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.5.sp,
                        color = BackgroundLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AuthTabsBackground, RoundedCornerShape(15.dp))
                    .border(1.06.dp, PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
                    .padding(horizontal = 18.75.dp, vertical = 15.dp)
            ) {
                Text(
                    text = "\"${challenge.quote}\"",
                    textAlign = TextAlign.Center,
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.4.sp,
                    color = PrimaryDark,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.75.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(challenge.accentColor.copy(alpha = 0.09f), RoundedCornerShape(19.dp))
                    .border(1.06.dp, challenge.accentColor.copy(alpha = 0.27f), RoundedCornerShape(19.dp))
                    .padding(horizontal = 15.dp, vertical = 9.38.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.5.dp)
            ) {
                Text(text = "🎁", fontSize = 14.sp)
                Text(
                    text = challenge.rewardText,
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    color = PrimaryDark
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
                            ambientColor = PrimaryDark.copy(alpha = 0.25f),
                            spotColor = PrimaryDark.copy(alpha = 0.25f)
                        )
                        .background(PrimaryDark, RoundedCornerShape(15.dp))
                        .clickable(onClick = onClaim)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Claim +${challenge.xpReward} XP 🎉",
                        textAlign = TextAlign.Center,
                        fontFamily = NunitoFontFamily,
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

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ChallengeActiveScreenPreview() {
    PhysiLockTheme {
        ChallengeActiveScreen(
            challenge = moveChallenges.first { it.id == "arm_shake" },
            onBackClick = {},
            onClaim = {}
        )
    }
}
