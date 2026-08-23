package com.example.physi_lock.ui.focus

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage

/** Ported from the teammate's sprint-2-ui-navigation branch. UI only — no Focus Mode session/timer backend exists yet (Module 6). */
private val FocusTimerBackground = Color(0xFF2E3820)

private val focusQuotes = listOf(
    "Your mind is sharper without the noise.",
    "The best ideas come in the quiet.",
    "Stillness is where clarity begins.",
    "One focused hour beats five distracted ones."
)

private val defaultBlockedApps = listOf(
    "📸" to "Instagram",
    "🎵" to "TikTok",
    "𝕏" to "Twitter/X",
    "▶️" to "YouTube",
    "🟠" to "Reddit"
)

@Composable
fun FocusModeScreen(
    elapsedSeconds: Long,
    onEndFocusClick: () -> Unit,
    modifier: Modifier = Modifier,
    blockedApps: List<Pair<String, String>> = defaultBlockedApps
) {
    val quote = remember { focusQuotes.random() }
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOlive)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp, bottom = 7.5.dp, start = 18.75.dp, end = 18.75.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = SecondarySage,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "FOCUS MODE ACTIVE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp,
                    color = SageAccent
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 22.5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(FocusTimerBackground, CircleShape)
                    .border(2.dp, SageAccent.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${"%02d".format(minutes)}:${"%02d".format(seconds)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 28.sp,
                        color = BackgroundLight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "focused",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = SageAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "\"$quote\"",
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 21.sp,
                color = SecondarySage,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "BLOCKED DURING FOCUS",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                color = SageAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(11.25.dp))

            BlockedAppsCloud(blockedApps = blockedApps)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.5.dp)
                .padding(bottom = 22.5.dp)
                .height(52.dp)
                .background(BackgroundLight.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
                .border(0.79.dp, SageAccent.copy(alpha = 0.25f), RoundedCornerShape(15.dp))
                .clickable(onClick = onEndFocusClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "End Focus Session",
                textAlign = TextAlign.Center,
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp,
                color = SecondarySage
            )
        }
    }
}

@Composable
private fun BlockedAppsCloud(blockedApps: List<Pair<String, String>>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.5.dp)
    ) {
        blockedApps.chunked(2).forEach { rowApps ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.5.dp)
            ) {
                rowApps.forEach { (emoji, name) -> BlockedAppChip(emoji = emoji, name = name) }
            }
        }
    }
}

@Composable
private fun BlockedAppChip(emoji: String, name: String) {
    Row(
        modifier = Modifier
            .background(BackgroundLight.copy(alpha = 0.06f), RoundedCornerShape(19.dp))
            .border(0.79.dp, BackgroundLight.copy(alpha = 0.08f), RoundedCornerShape(19.dp))
            .padding(horizontal = 11.25.dp, vertical = 5.63.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.63.dp)
    ) {
        Text(text = emoji, fontSize = 14.sp, modifier = Modifier.alpha(0.5f))
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MutedText,
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = name,
            fontFamily = Nunito,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 19.5.sp,
            color = MutedText
        )
    }
}

/**
 * Drives [FocusModeScreen] with a locally-ticking elapsed-time counter, since no real Focus
 * Mode session/timer backend exists yet (Module 6). The timer resets whenever this leaves
 * composition (e.g. navigating away ends the "session") — there's no persisted session state.
 */
@Composable
fun FocusModeRoute(onEndFocusClick: () -> Unit, modifier: Modifier = Modifier) {
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    FocusModeScreen(
        elapsedSeconds = elapsedSeconds,
        onEndFocusClick = onEndFocusClick,
        modifier = modifier
    )
}
