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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import com.example.physi_lock.ui.components.AuthFieldLabelWithHint
import com.example.physi_lock.ui.components.AuthGoogleButton
import com.example.physi_lock.ui.components.AuthHeader
import com.example.physi_lock.ui.components.AuthModeTabs
import com.example.physi_lock.ui.components.AuthOrDivider
import com.example.physi_lock.ui.components.AuthRememberMeCheckbox
import com.example.physi_lock.ui.components.AuthTab
import com.example.physi_lock.ui.components.AuthTabsBackground
import com.example.physi_lock.ui.components.AuthTextField
import com.example.physi_lock.ui.components.PasswordRequirementsChecklist
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.ErrorRed
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent

private enum class UsageMode(val emoji: String, val label: String, val configValue: String) {
    STUDENT("📚", "Student", "STUDENT_MODE"),
    WORK("💼", "Work", "WORK_MODE")
}

/**
 * Ported from the teammate's sprint-2-ui-navigation branch. Unlike the original (which called
 * an in-memory AuthRepository mock directly), this takes [onCreateAccount] with the shared
 * [AuthFormState] so the caller can wire it to the real HybridAccountRepository via
 * AuthViewModel once nav-wiring is approved. Unlike the teammate's version — where the Usage
 * Mode picker was cosmetic, never actually passed into registration — [AuthFormState.usageMode]
 * here is real: MainActivity saves it to UserConfiguration right after a successful registration.
 */
@Composable
fun CreateAccountScreen(
    onBackClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onCreateAccount: (AuthFormState) -> Unit,
    onGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordFieldFocused by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var usageMode by remember { mutableStateOf(UsageMode.STUDENT) }
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

            AuthHeader(title = "Create account", subtitle = "Start your detox journey")

            AuthModeTabs(
                selected = AuthTab.REGISTER,
                onSignInClick = onNavigateToLogin,
                onRegisterClick = { },
                modifier = Modifier.padding(bottom = 30.dp)
            )

            AuthFieldLabelWithHint(label = "Username", hint = "(unique)", hintColor = SageAccent)
            Spacer(modifier = Modifier.height(5.63.dp))
            AuthTextField(
                value = username,
                onValueChange = {
                    username = it
                    validationError = null
                },
                placeholder = "e.g. alexrivera23",
                leadingIcon = Icons.Default.AccountCircle,
                modifier = Modifier.padding(bottom = 15.dp)
            )

            AuthFieldLabel(text = "Full Name", fontSize = 14.sp, lineHeight = 21.sp)
            Spacer(modifier = Modifier.height(5.63.dp))
            AuthTextField(
                value = name,
                onValueChange = {
                    name = it
                    validationError = null
                },
                placeholder = "Alex Rivera",
                leadingIcon = Icons.Default.Person,
                modifier = Modifier.padding(bottom = 15.dp)
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
                placeholder = "Enter your password",
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
                },
                onFocusChanged = { passwordFieldFocused = it }
            )
            if (passwordFieldFocused) {
                PasswordRequirementsChecklist(
                    password = password,
                    modifier = Modifier.padding(bottom = 15.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(15.dp))
            }

            AuthFieldLabel(text = "Confirm Password")
            Spacer(modifier = Modifier.height(5.63.dp))
            AuthTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    validationError = null
                },
                placeholder = "Re-enter your password",
                leadingIcon = Icons.Default.Lock,
                isPassword = !confirmPasswordVisible,
                keyboardType = KeyboardType.Password,
                trailingContent = {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                        tint = SageAccent,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { confirmPasswordVisible = !confirmPasswordVisible }
                    )
                }
            )

            Spacer(modifier = Modifier.height(15.dp))

            AuthRememberMeCheckbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it }
            )

            AuthFieldLabel(
                text = "Usage Mode",
                modifier = Modifier.padding(top = 15.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.5.dp)
            ) {
                UsageMode.entries.forEach { mode ->
                    UsageModePill(
                        mode = mode,
                        isSelected = usageMode == mode,
                        onClick = { usageMode = mode },
                        modifier = Modifier.weight(1f)
                    )
                }
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

            Spacer(modifier = Modifier.height(if (displayedError != null) 7.5.dp else 22.5.dp))

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
                        if (username.isBlank() || name.isBlank() || email.isBlank() ||
                            password.isBlank() || confirmPassword.isBlank()
                        ) {
                            validationError = "Fill in all fields to create your account."
                        } else if (password != confirmPassword) {
                            validationError = "Passwords do not match."
                        } else {
                            validationError = null
                            onCreateAccount(
                                AuthFormState(
                                    username = username,
                                    fullName = name,
                                    identifier = email,
                                    password = password,
                                    confirmPassword = confirmPassword,
                                    usageMode = usageMode.configValue
                                )
                            )
                        }
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create Account",
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

@Composable
private fun UsageModePill(
    mode: UsageMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) DeepOlive else AuthTabsBackground,
                shape = RoundedCornerShape(19.dp)
            )
            .padding(vertical = 11.25.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.75.dp)
    ) {
        Text(
            text = mode.emoji,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            lineHeight = 27.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = mode.label,
            textAlign = TextAlign.Center,
            fontFamily = Nunito,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp,
            color = if (isSelected) BackgroundLight else DeepOlive,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
