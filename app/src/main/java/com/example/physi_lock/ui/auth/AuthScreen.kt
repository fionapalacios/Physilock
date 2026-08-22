package com.example.physi_lock.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent

// Local, screen-specific colors matching the Figma export (same pattern as LandingScreen.kt).
private val PageBackground = Color(0xFFFEFEFE)
private val LogoTint = Color(0xFFBAC892)
private val ToggleTrackSurface = Color(0xFFF5F3EB)
private val FieldSurface = Color(0xFFEEEADE)

enum class AuthMode { LOGIN, REGISTER }

data class AuthFormState(
    val username: String = "",
    val fullName: String = "",
    val identifier: String = "", // login: username or email · register: email
    val password: String = "",
    val confirmPassword: String = ""
)

@Composable
fun AuthScreen(
    onLogin: (identifier: String, password: String) -> Unit,
    onRegister: (AuthFormState) -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onBack: () -> Unit,
    errorMessage: String? = null
) {
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var form by remember { mutableStateOf(AuthFormState()) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Back", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MutedText)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DeepOlive),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Eco, contentDescription = null, tint = LogoTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (mode == AuthMode.LOGIN) "Welcome back" else "Create account",
                    fontFamily = Nunito,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = DeepOlive
                )
                Text(
                    text = if (mode == AuthMode.LOGIN) "Sign in to continue" else "Start your detox journey",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    color = SageAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mode toggle pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ToggleTrackSurface)
                .padding(4.dp)
        ) {
            listOf(AuthMode.LOGIN to "Sign In", AuthMode.REGISTER to "Register").forEach { (m, label) ->
                val selected = mode == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) DeepOlive else Color.Transparent)
                        .clickable { mode = m }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = Nunito,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (selected) PageBackground else MutedText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(visible = mode == AuthMode.REGISTER) {
            Column {
                AuthField(
                    label = "Username",
                    value = form.username,
                    onValueChange = { form = form.copy(username = it) },
                    placeholder = "yourusername",
                    leadingIcon = Icons.Filled.Person
                )
                Spacer(modifier = Modifier.height(16.dp))
                AuthField(
                    label = "Full Name",
                    value = form.fullName,
                    onValueChange = { form = form.copy(fullName = it) },
                    placeholder = "Alex Rivera",
                    leadingIcon = Icons.Filled.Person
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        AuthField(
            label = if (mode == AuthMode.LOGIN) "Username or Email" else "Email Address",
            value = form.identifier,
            onValueChange = { form = form.copy(identifier = it) },
            placeholder = if (mode == AuthMode.LOGIN) "yourusername or you@example.com" else "you@example.com",
            leadingIcon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthField(
            label = "Password",
            value = form.password,
            onValueChange = { form = form.copy(password = it) },
            placeholder = "••••••••",
            leadingIcon = Icons.Filled.Lock,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = SageAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        )

        AnimatedVisibility(visible = mode == AuthMode.REGISTER) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                AuthField(
                    label = "Confirm Password",
                    value = form.confirmPassword,
                    onValueChange = { form = form.copy(confirmPassword = it) },
                    placeholder = "••••••••",
                    leadingIcon = Icons.Filled.Lock,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                tint = SageAccent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                )
            }
        }

        if (mode == AuthMode.LOGIN) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onForgotPassword,
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Forgot password?", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SageAccent)
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                fontFamily = Nunito,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color(0xFFC0392B)
            )
        }

        if (validationError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = validationError.orEmpty(),
                fontFamily = Nunito,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color(0xFFC0392B)
            )
        }

        Spacer(modifier = Modifier.height(if (mode == AuthMode.LOGIN) 8.dp else 24.dp))

        Button(
            onClick = {
                validationError = null
                if (mode == AuthMode.LOGIN) {
                    if (form.identifier.isBlank() || form.password.isBlank()) {
                        validationError = "Enter your username/email and password to sign in."
                    } else {
                        onLogin(form.identifier, form.password)
                    }
                } else {
                    if (form.username.isBlank() ||
                        form.fullName.isBlank() ||
                        form.identifier.isBlank() ||
                        form.password.isBlank() ||
                        form.confirmPassword.isBlank()
                    ) {
                        validationError = "Fill in all fields to create your account."
                    } else if (form.password != form.confirmPassword) {
                        validationError = "Passwords do not match."
                    } else {
                        onRegister(form)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepOlive, contentColor = PageBackground)
        ) {
            Text(
                text = if (mode == AuthMode.LOGIN) "Sign In" else "Create Account",
                fontFamily = Nunito,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = DeepOlive.copy(alpha = 0.15f))
            Text(
                text = "  or  ",
                fontFamily = Nunito,
                fontSize = 12.sp,
                color = MutedText
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = DeepOlive.copy(alpha = 0.15f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DeepOlive.copy(alpha = 0.2f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepOlive)
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = SageAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Continue with Google",
                fontFamily = Nunito,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontFamily = Nunito,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = DeepOlive,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(FieldSurface)
                .border(1.dp, DeepOlive.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(leadingIcon, contentDescription = null, tint = SageAccent, modifier = Modifier.size(15.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        color = MutedText.copy(alpha = 0.5f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = Nunito, fontSize = 14.sp, color = DeepOlive),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(DeepOlive)
                )
            }
            trailingIcon?.invoke()
        }
    }
}
