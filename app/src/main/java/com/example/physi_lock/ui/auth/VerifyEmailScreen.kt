package com.example.physi_lock.ui.auth

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.ErrorRed
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage

/**
 * Ported from the teammate's sprint-2-ui-navigation branch, then adapted from a custom
 * in-app 6-digit code entry to Firebase Auth's native link-based verification — Firebase
 * emails a link, so there's no code to type here. [onCheckVerified] fires when the user
 * taps "I've verified"; the caller re-checks Firebase's `isEmailVerified` flag (a link
 * click doesn't push a signal into the app, so this has to be a manual recheck) and
 * reports failure back via [verificationError].
 */
@Composable
fun VerifyEmailScreen(
    email: String,
    onBackClick: () -> Unit,
    onCheckVerified: () -> Unit,
    onResendCode: () -> Unit = {},
    modifier: Modifier = Modifier,
    checking: Boolean = false,
    verificationError: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.99.dp,
                        color = DeepOlive.copy(alpha = 0.08f)
                    )
                    .padding(horizontal = 15.dp, vertical = 11.25.dp)
                    .clickable(onClick = onBackClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.25.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DeepOlive,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Verify your email",
                    fontFamily = Nunito,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 18.7.sp,
                    color = DeepOlive
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.5.dp)
                    .padding(top = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 18.75.dp)
                        .size(72.dp)
                        .background(SecondarySage.copy(alpha = 0.13f), RoundedCornerShape(22.dp))
                        .border(1.98.dp, SecondarySage.copy(alpha = 0.40f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        tint = SageAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "We sent a verification link to",
                    textAlign = TextAlign.Center,
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 22.4.sp,
                    color = DeepOlive,
                    modifier = Modifier.padding(bottom = 7.5.dp)
                )

                Text(
                    text = email,
                    textAlign = TextAlign.Center,
                    fontFamily = Nunito,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.5.sp,
                    color = DeepOlive,
                    modifier = Modifier.padding(bottom = 15.dp)
                )

                Text(
                    text = "Open the email on this device and tap the link, then come back " +
                        "and tap the button below to continue.",
                    textAlign = TextAlign.Center,
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = DeepOlive,
                    modifier = Modifier.padding(bottom = 22.5.dp)
                )

                if (verificationError != null) {
                    Text(
                        text = verificationError,
                        fontFamily = Nunito,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.85.sp,
                        color = ErrorRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 15.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 15.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(15.dp),
                            ambientColor = DeepOlive.copy(alpha = 0.25f),
                            spotColor = DeepOlive.copy(alpha = 0.25f)
                        )
                        .background(DeepOlive, RoundedCornerShape(15.dp))
                        .clickable(enabled = !checking, onClick = onCheckVerified)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (checking) "Checking..." else "I've verified — Continue",
                        textAlign = TextAlign.Center,
                        fontFamily = Nunito,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.5.sp,
                        color = BackgroundLight
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.63.dp),
                    modifier = Modifier.clickable(onClick = onResendCode)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = SageAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Resend email",
                        textAlign = TextAlign.Center,
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp,
                        color = SageAccent
                    )
                }

                Text(
                    text = "Didn't receive anything? Check your spam folder or tap Resend email.",
                    textAlign = TextAlign.Center,
                    fontFamily = Nunito,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp,
                    color = DeepOlive,
                    modifier = Modifier.padding(top = 22.5.dp, bottom = 24.dp)
                )
            }
        }
    }
}
