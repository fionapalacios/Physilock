package com.example.physi_lock.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.components.AuthBackButton
import com.example.physi_lock.ui.components.AuthFieldLabel
import com.example.physi_lock.ui.components.AuthGoogleButton
import com.example.physi_lock.ui.components.AuthHeader
import com.example.physi_lock.ui.components.AuthModeTabs
import com.example.physi_lock.ui.components.AuthOrDivider
import com.example.physi_lock.ui.components.AuthRememberMeCheckbox
import com.example.physi_lock.ui.components.AuthTab
import com.example.physi_lock.ui.components.AuthTextField
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.ErrorRed
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent

/**
 * Ported from the teammate's sprint-2-ui-navigation branch. Unlike the original (which called
 * an in-memory AuthRepository mock directly), this takes [onLogin] so the caller can wire it to
 * the real HybridAccountRepository via AuthViewModel once nav-wiring is approved.
 */
@Composable
fun LoginScreen(
    onBackClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLogin: (identifier: String, password: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

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
                    validationError = null
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
                    validationError = null
                },
                placeholder = "••••••••",
                leadingIcon = Icons.Default.Lock,
                isPassword = !passwordVisible,
                keyboardType = KeyboardType.Password,
                trailingContent = {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = SageAccent,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { passwordVisible = !passwordVisible }
                    )
                }
            )

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuthRememberMeCheckbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it }
                )
                Text(
                    text = "Forgot password?",
                    textAlign = TextAlign.Right,
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.5.sp,
                    color = SageAccent,
                    modifier = Modifier.clickable(onClick = onNavigateToForgotPassword)
                )
            }

            val displayedError = validationError ?: errorMessage
            if (displayedError != null) {
                Spacer(modifier = Modifier.height(11.25.dp))
                Text(
                    text = displayedError,
                    fontFamily = Nunito,
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
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(15.dp),
                        ambientColor = DeepOlive.copy(alpha = 0.25f),
                        spotColor = DeepOlive.copy(alpha = 0.25f)
                    )
                    .background(DeepOlive, RoundedCornerShape(15.dp))
                    .clickable {
                        if (email.isBlank() || password.isBlank()) {
                            validationError = "Enter your email and password to sign in."
                        } else {
                            validationError = null
                            onLogin(email, password)
                        }
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue",
                    textAlign = TextAlign.Center,
                    fontFamily = Nunito,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.5.sp,
                    color = BackgroundLight
                )
            }

            Spacer(modifier = Modifier.height(11.25.dp))

            AuthOrDivider()

            Spacer(modifier = Modifier.height(11.25.dp))

            AuthGoogleButton(
                onClick = onGoogleSignIn,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
