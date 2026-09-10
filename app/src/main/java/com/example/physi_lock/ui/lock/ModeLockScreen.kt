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
import androidx.compose.material.icons.filled.Lock
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
import com.example.physi_lock.ui.theme.DeepForest
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SecondarySage

/** Real content for [ModeLockActivity] -- Figma's "Lock Screens" export specifies a dark
 *  full-screen background (#1F2A14 "Deep Forest") for Focus Mode/Deep Work's violation
 *  overlay specifically (distinct from the plain App-Lock challenge screen and matching
 *  [FocusModeScreen]/[DeepWorkScreen]'s own in-session dark treatment), with SecondarySage
 *  icon/button-text and translucent-white button chrome (2026-09-10 re-theme, was previously
 *  built light by mistake). Generalized with [title]/[message]/[hint] so one screen serves
 *  both Focus Mode and Deep Work Mode. No challenge to complete and no bypass surfaced here --
 *  [onGoHome] is the only action, matching the decision that ending the session (Focus) or
 *  the shake-exit gate (Deep Work) stays the only real way back in. */
@Composable
fun ModeLockScreen(
    packageName: String,
    title: String,
    message: String,
    onGoHome: () -> Unit,
    hint: String? = null
) {
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
            .background(DeepForest)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = SecondarySage,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
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
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                .padding(15.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                AppIconAvatar(
                    packageName = packageName,
                    appName = appName,
                    backgroundColor = DeepForest,
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
                    text = message,
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = SecondarySage,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (hint != null) {
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = hint,
                fontFamily = Nunito,
                fontSize = 12.sp,
                color = SecondarySage.copy(alpha = 0.8f),
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }

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
