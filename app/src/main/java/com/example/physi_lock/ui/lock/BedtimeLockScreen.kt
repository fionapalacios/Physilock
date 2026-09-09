package com.example.physi_lock.ui.lock

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.components.AppIconAvatar
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.CardCream
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent

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

/** Real content for [BedtimeLockActivity] -- same app-theme tokens (BackgroundLight,
 *  DeepOlive, SageAccent, CardCream, Nunito) as BedtimeModeScreen and the rest of
 *  Settings/Home/Reports, not a one-off dark screen. No challenge to complete: Bedtime is a
 *  hard block by design, so [onGoHome] is the only action -- finishes the activity, and
 *  because BedtimeLockActivity launches with NEW_TASK|CLEAR_TASK (same as LockActivity),
 *  the OS shows the home launcher next. */
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
            .background(BackgroundLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(CardCream, RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Bedtime,
                contentDescription = null,
                tint = DeepOlive,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Bedtime Mode Active",
            fontFamily = Nunito,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepOlive,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardCream, RoundedCornerShape(16.dp))
                .padding(15.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                AppIconAvatar(
                    packageName = packageName,
                    appName = appName,
                    backgroundColor = BackgroundLight,
                    contentColor = DeepOlive,
                    size = 44.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = appName,
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Locked until ${formatMinuteOfDay(bedtimeEndMinute)}",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = SageAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "Apps are locked during Bedtime Mode. Only whitelisted apps (see Whitelist Manager) remain accessible.",
            fontFamily = Nunito,
            fontSize = 12.sp,
            color = MutedText,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepOlive, RoundedCornerShape(15.dp))
                .clickable(onClick = onGoHome)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Return to Home Screen",
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BackgroundLight
            )
        }
    }
}
