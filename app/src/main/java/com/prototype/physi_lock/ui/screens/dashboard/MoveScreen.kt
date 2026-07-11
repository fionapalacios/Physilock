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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen

val MoveStreakOrange = Color(0xFFE8854A)

data class MoveChallenge(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val xpReward: Int,
    val isTimeBased: Boolean,
    val targetValue: Int,
    val durationLabel: String,
    val rewardText: String,
    val quote: String
)

val moveChallenges = listOf(
    MoveChallenge(
        id = "run_jog",
        title = "Run / Jog",
        description = "Continuous jogging detected via accelerometer",
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        accentColor = com.prototype.physi_lock.ui.theme.AccentLavender,
        xpReward = 40,
        isTimeBased = true,
        targetValue = 180,
        durationLabel = "3 min",
        rewardText = "Unlocks Instagram for 30 min",
        quote = "Your body is asking for this. Trust it."
    ),
    MoveChallenge(
        id = "walk_5min",
        title = "5-Minute Walk",
        description = "Walk at a steady pace",
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        accentColor = PrimaryGreen,
        xpReward = 20,
        isTimeBased = true,
        targetValue = 300,
        durationLabel = "5 min",
        rewardText = "Unlocks TikTok for 20 min",
        quote = "One step at a time — that's all it takes."
    ),
    MoveChallenge(
        id = "arm_shake",
        title = "Arm Shake Burst",
        description = "Shake or rotate your arms vigorously",
        icon = Icons.Default.Bolt,
        accentColor = DeepOlive,
        xpReward = 15,
        isTimeBased = false,
        targetValue = 30,
        durationLabel = "30 reps",
        rewardText = "Adds 15 min to screen budget",
        quote = "Motion is the medicine. Keep moving."
    )
)

@Composable
fun MoveScreen(
    totalXp: Int,
    completedChallengeIds: Set<String>,
    onStartChallenge: (MoveChallenge) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 15.dp)
            .padding(top = 7.5.dp, bottom = 24.dp)
    ) {
        Text(
            text = "MOTION LOCK",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.5.sp,
            color = PrimaryGreen
        )
        Text(
            text = "Move to Unlock",
            fontFamily = NunitoFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 22.sp,
            color = PrimaryDark
        )

        Spacer(modifier = Modifier.height(18.75.dp))

        XpStreakCard(totalXp = totalXp)

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "TODAY'S CHALLENGES",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.5.sp,
            color = PrimaryGreen
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(11.25.dp)) {
            moveChallenges.forEach { challenge ->
                ChallengeCard(
                    challenge = challenge,
                    isCompleted = challenge.id in completedChallengeIds,
                    onStartChallenge = { onStartChallenge(challenge) }
                )
            }
        }
    }
}

private val weekDayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
private fun XpStreakCard(totalXp: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryDark, RoundedCornerShape(22.5.dp))
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TOTAL XP EARNED",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = PrimaryGreen
                )
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.75.dp)) {
                    Text(
                        text = "$totalXp",
                        fontFamily = NunitoFontFamily,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp,
                        color = BackgroundLight
                    )
                    Text(
                        text = "pts",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = com.prototype.physi_lock.ui.theme.SecondarySage
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.63.dp)) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MoveStreakOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "3",
                    fontFamily = NunitoFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 27.sp,
                    color = BackgroundLight
                )
                Text(
                    text = "day streak",
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    color = com.prototype.physi_lock.ui.theme.SecondarySage
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.63.dp)) {
            weekDayLabels.forEachIndexed { index, label ->
                val isActive = index < 3
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.75.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .then(
                                if (isActive) {
                                    Modifier.background(MoveStreakOrange.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                } else {
                                    Modifier.border(1.06.dp, PrimaryGreen.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (isActive) MoveStreakOrange else PrimaryGreen.copy(alpha = 0.45f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = if (isActive) PrimaryGreen else PrimaryGreen.copy(alpha = 0.40f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    challenge: MoveChallenge,
    isCompleted: Boolean,
    onStartChallenge: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(challenge.accentColor.copy(alpha = 0.13f), RoundedCornerShape(15.dp))
            .border(1.06.dp, challenge.accentColor.copy(alpha = 0.27f), RoundedCornerShape(15.dp))
            .padding(15.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(11.25.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(challenge.accentColor.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = challenge.icon,
                    contentDescription = null,
                    tint = challenge.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.63.dp)) {
                    Text(
                        text = challenge.title,
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 21.sp,
                        color = PrimaryDark
                    )
                    Box(
                        modifier = Modifier
                            .background(challenge.accentColor.copy(alpha = 0.13f), RoundedCornerShape(3.75.dp))
                            .padding(horizontal = 5.63.dp, vertical = 1.88.dp)
                    ) {
                        Text(
                            text = "+${challenge.xpReward} XP",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            color = challenge.accentColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(PrimaryDark.copy(alpha = 0.08f), RoundedCornerShape(3.75.dp))
                            .padding(horizontal = 5.63.dp, vertical = 1.88.dp)
                    ) {
                        Text(
                            text = challenge.durationLabel,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            color = DeepOlive
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = challenge.description,
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🎁 ${challenge.rewardText}",
                    fontFamily = NunitoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                    color = challenge.accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(11.25.dp))

        if (isCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(challenge.accentColor.copy(alpha = 0.13f), RoundedCornerShape(19.dp))
                    .padding(vertical = 7.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.63.dp)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = challenge.accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Completed",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp,
                        color = challenge.accentColor
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(challenge.accentColor, RoundedCornerShape(19.dp))
                    .clickable(onClick = onStartChallenge)
                    .padding(vertical = 9.38.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.63.dp)) {
                    Text(
                        text = "Start Challenge",
                        textAlign = TextAlign.Center,
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp,
                        color = BackgroundLight
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BackgroundLight,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun MoveScreenPreview() {
    PhysiLockTheme {
        MoveScreen(
            totalXp = 20,
            completedChallengeIds = setOf("walk_5min"),
            onStartChallenge = {}
        )
    }
}
