package com.prototype.physi_lock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.R
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.ErrorRed
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen

val AuthTabsBackground = Color(0xFFF5F3EB)
val AuthInputFieldBackground = Color(0xFFEEEADE)

enum class AuthTab { SIGN_IN, REGISTER }

@Composable
fun AuthBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(top = 15.dp, bottom = 22.5.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.75.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = DeepOlive,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Back",
            fontFamily = NunitoFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 21.sp,
            color = DeepOlive
        )
    }
}

@Composable
fun AuthHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(bottom = 34.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Physi-Lock logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(76.dp)
        )
        Column {
            Text(
                text = title,
                fontFamily = NunitoFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 22.sp,
                color = PrimaryDark
            )
            Text(
                text = subtitle,
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 21.sp,
                color = PrimaryGreen
            )
        }
    }
}

@Composable
fun AuthModeTabs(
    selected: AuthTab,
    onSignInClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AuthTabsBackground, RoundedCornerShape(19.dp))
            .padding(3.75.dp)
    ) {
        AuthTab(
            text = "Sign In",
            isActive = selected == AuthTab.SIGN_IN,
            onClick = onSignInClick,
            modifier = Modifier.weight(1f)
        )
        AuthTab(
            text = "Register",
            isActive = selected == AuthTab.REGISTER,
            onClick = onRegisterClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AuthTab(text: String, isActive: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(39.73.dp)
            .then(
                if (isActive) {
                    Modifier.background(PrimaryDark, RoundedCornerShape(15.dp))
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontFamily = NunitoFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 21.sp,
            color = if (isActive) BackgroundLight else DeepOlive
        )
    }
}

@Composable
fun AuthFieldLabel(text: String, modifier: Modifier = Modifier, fontSize: androidx.compose.ui.unit.TextUnit = 12.sp, lineHeight: androidx.compose.ui.unit.TextUnit = 18.sp) {
    Text(
        text = text,
        fontFamily = NunitoFontFamily,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        lineHeight = lineHeight,
        color = PrimaryDark,
        modifier = modifier
    )
}

@Composable
fun AuthFieldLabelWithHint(
    label: String,
    hint: String,
    modifier: Modifier = Modifier,
    hintColor: Color = PrimaryGreen,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    lineHeight: androidx.compose.ui.unit.TextUnit = 19.5.sp
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = PrimaryDark, fontWeight = FontWeight.Bold)) {
                append("$label ")
            }
            withStyle(SpanStyle(color = hintColor, fontWeight = FontWeight.Medium)) {
                append(hint)
            }
        },
        fontFamily = NunitoFontFamily,
        fontSize = fontSize,
        lineHeight = lineHeight,
        modifier = modifier
    )
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingContent: (@Composable () -> Unit)? = null,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AuthInputFieldBackground, RoundedCornerShape(19.dp))
            .border(0.79.dp, PrimaryDark.copy(alpha = 0.12f), RoundedCornerShape(19.dp))
            .padding(horizontal = 15.dp, vertical = 13.13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.25.dp)
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(15.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    color = PrimaryDark.copy(alpha = 0.5f)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    color = PrimaryDark
                ),
                visualTransformation = if (isPassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(PrimaryDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocusChanged(it.isFocused) }
            )
        }
        trailingContent?.invoke()
    }
}

@Composable
fun AuthRememberMeCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(15.dp)
                .background(if (checked) PrimaryDark else BackgroundLight, RoundedCornerShape(2.dp))
                .border(1.dp, if (checked) PrimaryDark else Color(0xFF767676), RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = BackgroundLight,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
        Text(
            text = "Remember me",
            fontFamily = NunitoFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.5.sp,
            color = DeepOlive
        )
    }
}

@Composable
fun AuthOrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.25.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = PrimaryDark.copy(alpha = 0.12f)
        )
        Text(
            text = "or",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.5.sp,
            color = PrimaryGreen
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = PrimaryDark.copy(alpha = 0.12f)
        )
    }
}

@Composable
fun AuthGoogleButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(15.dp),
                ambientColor = PrimaryDark.copy(alpha = 0.07f),
                spotColor = PrimaryDark.copy(alpha = 0.07f)
            )
            .background(BackgroundLight, RoundedCornerShape(15.dp))
            .border(0.79.dp, PrimaryDark.copy(alpha = 0.15f), RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.25.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Continue with Google",
            fontFamily = NunitoFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 21.sp,
            color = PrimaryDark
        )
    }
}

private data class PasswordRequirement(val label: String, val isMet: (String) -> Boolean)

private val passwordRequirements = listOf(
    PasswordRequirement("8 characters minimum") { it.length >= 8 },
    PasswordRequirement("At least one uppercase letter") { it.any(Char::isUpperCase) },
    PasswordRequirement("At least one number") { it.any(Char::isDigit) },
    PasswordRequirement("At least one lowercase letter") { it.any(Char::isLowerCase) }
)

@Composable
fun PasswordRequirementsChecklist(password: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 7.5.dp),
        verticalArrangement = Arrangement.spacedBy(3.75.dp)
    ) {
        val isEmpty = password.isEmpty()
        passwordRequirements.forEach { requirement ->
            val isMet = requirement.isMet(password)
            val color = when {
                isEmpty -> PrimaryDark.copy(alpha = 0.35f)
                isMet -> PrimaryGreen
                else -> ErrorRed
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.63.dp)
            ) {
                Icon(
                    imageVector = when {
                        isEmpty -> Icons.Filled.Circle
                        isMet -> Icons.Filled.CheckCircle
                        else -> Icons.Filled.Cancel
                    },
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(if (isEmpty) 8.dp else 13.dp)
                )
                Text(
                    text = requirement.label,
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 19.5.sp,
                    color = color
                )
            }
        }
    }
}
