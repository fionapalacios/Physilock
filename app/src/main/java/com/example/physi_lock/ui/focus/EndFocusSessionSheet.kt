package com.example.physi_lock.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.components.AuthTabsBackground
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito

/** Ported from the teammate's sprint-2-ui-navigation branch. UI only — see FocusModeScreen. */
@Composable
fun EndFocusSessionSheet(
    elapsedSeconds: Long,
    onEndSession: () -> Unit,
    onKeepGoing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val creditMinutes = elapsedSeconds / 300

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOlive.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onKeepGoing
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .background(BackgroundLight, RoundedCornerShape(topStart = 22.5.dp, topEnd = 22.5.dp))
                .padding(22.5.dp)
        ) {
            Text(
                text = "End focus session?",
                fontFamily = Nunito,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 27.sp,
                color = DeepOlive
            )

            Text(
                text = "You've focused for ${"%02d".format(minutes)}:${"%02d".format(seconds)}. " +
                    "Earned $creditMinutes min of screen credit.",
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 21.sp,
                color = MutedText,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(11.25.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepOlive, RoundedCornerShape(15.dp))
                        .clickable(onClick = onEndSession)
                        .padding(vertical = 13.13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "End Session",
                        textAlign = TextAlign.Center,
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp,
                        color = BackgroundLight
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AuthTabsBackground, RoundedCornerShape(15.dp))
                        .clickable(onClick = onKeepGoing)
                        .padding(vertical = 13.13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keep Going",
                        textAlign = TextAlign.Center,
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp,
                        color = DeepOlive
                    )
                }
            }
        }
    }
}
