package com.example.physi_lock.ui.lock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.components.AppIconAvatar
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MidnightForest
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d %s".format(displayHour, minute, if (hour < 12) "AM" else "PM")
}

/** Real content for [BedtimeLockActivity] -- Figma's "Lock Screens" export specifies a dark
 *  full-screen background (#0F1A09 "Midnight Forest") for Bedtime's violation overlay
 *  specifically, with a translucent DeepOlive-tinted info box (rgba(61,73,40,0.30) bg,
 *  rgba(156,176,109,0.20) SageAccent border) -- distinct from the plain App-Lock challenge
 *  screen's light theme and from Focus/Deep Work's DeepForest overlay (2026-09-10 re-theme,
 *  was previously built light by mistake). No challenge to complete: Bedtime is a hard block
 *  by design, so [onGoHome] is the only action -- finishes the activity, and because
 *  BedtimeLockActivity launches with NEW_TASK|CLEAR_TASK (same as LockActivity), the OS shows
 *  the home launcher next. */
@Composable
fun BedtimeLockScreen(packageName: String, bedtimeEndMinute: Int, onGoHome: () -> Unit) {
    val context = LocalContext.current
    val appName = remember(packageName) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightForest)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(DeepOlive.copy(alpha = 0.30f), RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Bedtime,
                contentDescription = null,
                tint = SecondarySage,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Bedtime Mode Active",
            fontFamily = Nunito,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepOlive.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, SageAccent.copy(alpha = 0.20f)), RoundedCornerShape(16.dp))
                .padding(15.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                AppIconAvatar(
                    packageName = packageName,
                    appName = appName,
                    backgroundColor = MidnightForest,
                    contentColor = SecondarySage,
                    size = 44.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = appName,
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Locked until ${formatMinuteOfDay(bedtimeEndMinute)}",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = SecondarySage
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "Apps are locked during Bedtime Mode. Only whitelisted apps (see Whitelist Manager) remain accessible.",
            fontFamily = Nunito,
            fontSize = 12.sp,
            color = SecondarySage.copy(alpha = 0.8f),
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(15.dp))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(15.dp))
                .clickable(onClick = onGoHome)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Return to Home Screen",
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SecondarySage
            )
        }
    }
}
