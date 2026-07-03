package com.prototype.physi_lock.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.ui.components.AuthBackButton
import com.prototype.physi_lock.ui.components.AuthFieldLabel
import com.prototype.physi_lock.ui.components.AuthHeader
import com.prototype.physi_lock.ui.components.AuthTextField
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onResetLinkSent: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.5.dp),
            horizontalAlignment = Alignment.Start
        ) {
            AuthBackButton(onClick = onBackClick)

            AuthHeader(
                title = "Reset your Password",
                subtitle = "Enter the email linked to your account to reset password"
            )

            AuthFieldLabel(text = "Email")
            Spacer(modifier = Modifier.height(5.63.dp))
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "you@example.com",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(7.5.dp))

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
                    text = "Send Link",
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

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    PhysiLockTheme {
        ForgotPasswordScreen(onBackClick = {}, onResetLinkSent = {})
    }
}
