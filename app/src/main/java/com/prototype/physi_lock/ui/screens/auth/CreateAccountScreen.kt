package com.prototype.physi_lock.ui.screens.auth

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.ui.components.AuthBackButton
import com.prototype.physi_lock.ui.components.AuthFieldLabel
import com.prototype.physi_lock.ui.components.AuthFieldLabelWithHint
import com.prototype.physi_lock.ui.components.AuthHeader
import com.prototype.physi_lock.ui.components.AuthModeTabs
import com.prototype.physi_lock.ui.components.AuthTab
import com.prototype.physi_lock.ui.components.AuthTabsBackground
import com.prototype.physi_lock.ui.components.AuthTextField
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.ErrorRed
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen
import com.prototype.physi_lock.data.AuthRepository
import com.prototype.physi_lock.data.AuthResult

private enum class UsageMode(val emoji: String, val label: String) {
    PERSONAL("🌿", "Personal"),
    STUDENT("📚", "Student"),
    WORK("💼", "Work")
}

@Composable
fun CreateAccountScreen(
    onBackClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onAccountCreated: (email: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var usageMode by remember { mutableStateOf(UsageMode.PERSONAL) }
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

            AuthHeader(title = "Create account", subtitle = "Start your detox journey")

            AuthModeTabs(
                selected = AuthTab.REGISTER,
                onSignInClick = onNavigateToLogin,
                onRegisterClick = { },
                modifier = Modifier.padding(bottom = 30.dp)
            )

            AuthFieldLabelWithHint(label = "Username", hint = "(unique)", hintColor = PrimaryGreen)
            Spacer(modifier = Modifier.height(5.63.dp))
            AuthTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = null
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
                    errorMessage = null
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
                placeholder = "Min 8 chars, 1 uppercase, 1 number",
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
                },
                modifier = Modifier.padding(bottom = 15.dp)
            )

            AuthFieldLabel(text = "Confirm Password")
            Spacer(modifier = Modifier.height(5.63.dp))
            AuthTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                placeholder = "Re-enter your password",
                leadingIcon = Icons.Default.Lock,
                isPassword = !confirmPasswordVisible,
                keyboardType = KeyboardType.Password,
                trailingContent = {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                        tint = PrimaryGreen,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { confirmPasswordVisible = !confirmPasswordVisible }
                    )
                }
            )

            AuthFieldLabelWithHint(
                label = "Usage Mode",
                hint = "(can change later)",
                hintColor = DeepOlive,
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
                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match."
                        } else {
                            when (val result = AuthRepository.register(username, name, email, password)) {
                                is AuthResult.Success -> {
                                    errorMessage = null
                                    onAccountCreated(email)
                                }
                                is AuthResult.Failure -> errorMessage = result.message
                            }
                        }
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create Account",
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
                color = if (isSelected) PrimaryDark else AuthTabsBackground,
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
            fontFamily = NunitoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp,
            color = if (isSelected) BackgroundLight else DeepOlive,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateAccountScreenPreview() {
    PhysiLockTheme {
        CreateAccountScreen(onBackClick = {}, onNavigateToLogin = {}, onAccountCreated = { _ -> })
    }
}
