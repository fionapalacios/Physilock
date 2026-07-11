package com.prototype.physi_lock.ui.screens.auth

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.ui.components.AuthFieldLabel
import com.prototype.physi_lock.ui.components.AuthTextField
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen
import com.prototype.physi_lock.ui.theme.TertiaryTan

private val forgotPasswordSteps = listOf("Email", "Verify", "Reset", "Done")
private const val EMAIL_STEP_INDEX = 0

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onResetLinkSent: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 480.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.06.dp, color = PrimaryDark.copy(alpha = 0.08f))
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
                Column {
                    Text(
                        text = "FORGOT PASSWORD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        color = PrimaryGreen
                    )
                    Text(
                        text = "Enter your email",
                        fontFamily = NunitoFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 18.7.sp,
                        color = PrimaryDark
                    )
                }
            }

            ForgotPasswordSteps(
                currentStepIndex = EMAIL_STEP_INDEX,
                modifier = Modifier.padding(top = 15.dp, bottom = 18.75.dp, start = 18.75.dp, end = 18.75.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.75.dp)
            ) {
                Text(
                    text = "Enter the email address linked to your Physi-Lock account. " +
                        "We'll send a 6-digit verification code.",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 22.4.sp,
                    color = DeepOlive
                )

                Spacer(modifier = Modifier.height(20.dp))

                AuthFieldLabel(text = "Email Address")
                Spacer(modifier = Modifier.height(6.dp))
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "alex@example.com",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.padding(bottom = 15.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(15.dp),
                            ambientColor = PrimaryDark.copy(alpha = 0.25f),
                            spotColor = PrimaryDark.copy(alpha = 0.25f)
                        )
                        .background(PrimaryDark, RoundedCornerShape(15.dp))
                        .clickable(onClick = onResetLinkSent)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Send Verification Code",
                        textAlign = TextAlign.Center,
                        fontFamily = NunitoFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.5.sp,
                        color = BackgroundLight
                    )
                }
            }
        }
    }
}

@Composable
private fun ForgotPasswordSteps(currentStepIndex: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        forgotPasswordSteps.forEachIndexed { index, label ->
            val isActive = index == currentStepIndex
            Row(
                modifier = if (index == forgotPasswordSteps.lastIndex) Modifier else Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.5.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (isActive) PrimaryDark else TertiaryTan, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.5.sp,
                            color = if (isActive) BackgroundLight else DeepOlive
                        )
                    }
                    Spacer(modifier = Modifier.height(3.75.dp))
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp,
                        color = if (isActive) PrimaryDark else PrimaryGreen
                    )
                }
                if (index != forgotPasswordSteps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 18.dp)
                            .height(2.dp)
                            .background(TertiaryTan, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    PhysiLockTheme {
        ForgotPasswordScreen(onBackClick = {}, onResetLinkSent = {})
    }
}
