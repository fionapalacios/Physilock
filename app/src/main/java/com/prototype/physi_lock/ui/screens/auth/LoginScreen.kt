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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.ui.components.AuthBackButton
import com.prototype.physi_lock.ui.components.AuthFieldLabel
import com.prototype.physi_lock.ui.components.AuthHeader
import com.prototype.physi_lock.ui.components.AuthModeTabs
import com.prototype.physi_lock.ui.components.AuthTab
import com.prototype.physi_lock.ui.components.AuthTextField
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.ErrorRed
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen
import com.prototype.physi_lock.data.AuthRepository
import com.prototype.physi_lock.data.AuthResult

@Composable
fun LoginScreen(
    onBackClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

            AuthHeader(title = "Welcome back", subtitle = "Sign in to continue")

            AuthModeTabs(
                selected = AuthTab.SIGN_IN,
                onSignInClick = { },
                onRegisterClick = onNavigateToRegister,
                modifier = Modifier.padding(bottom = 30.dp)
            )

            AuthFieldLabel(text = "Email Address")
            Spacer(modifier = Modifier.height(5.63.dp))
            AuthTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                placeholder = "you@example.com",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.padding(bottom = 15.dp)
            )

            AuthFieldLabel(text = "Password")
            Spacer(modifier = Modifier.height(5.63.dp))
            AuthTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                placeholder = "••••••••",
                leadingIcon = Icons.Default.Lock,
                isPassword = !passwordVisible,
                keyboardType = KeyboardType.Password,
                trailingContent = {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = PrimaryGreen,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { passwordVisible = !passwordVisible }
                    )
                }
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Forgot password?",
                textAlign = TextAlign.Right,
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 21.sp,
                color = PrimaryGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToForgotPassword)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(11.25.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.85.sp,
                    color = ErrorRed,
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
                    .clickable {
                        when (val result = AuthRepository.signIn(email, password)) {
                            is AuthResult.Success -> {
                                errorMessage = null
                                onLoginSuccess()
                            }
                            is AuthResult.Failure -> errorMessage = result.message
                        }
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
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
private fun LoginScreenPreview() {
    PhysiLockTheme {
        LoginScreen(
            onBackClick = {},
            onNavigateToRegister = {},
            onNavigateToForgotPassword = {},
            onLoginSuccess = {}
        )
    }
}
