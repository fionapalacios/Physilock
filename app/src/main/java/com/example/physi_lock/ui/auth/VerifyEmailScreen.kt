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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.components.AuthInputFieldBackground
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.ErrorRed
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage

/**
 * Ported from the teammate's sprint-2-ui-navigation branch. UI only — there is no
 * verification-code backend yet (see MODULE_PROGRESS.md). [onVerified] only fires once
 * the entered code passes the client-side 6-digit format check; a real check against a
 * sent code needs to be wired in once that backend exists.
 */
private const val CODE_LENGTH = 6

@Composable
fun VerifyEmailScreen(
    email: String,
    onBackClick: () -> Unit,
    onVerified: () -> Unit,
    onResendCode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val code = remember { mutableStateListOf(*Array(CODE_LENGTH) { "" }) }
    val focusRequesters = remember { List(CODE_LENGTH) { FocusRequester() } }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        focusRequesters.first().requestFocus()
    }

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
                    text = "We sent a 6-digit code to",
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
                    modifier = Modifier.padding(bottom = 22.5.dp)
                )

                Row(
                    modifier = Modifier.padding(bottom = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.5.dp)
                ) {
                    code.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(52.dp)
                                .background(AuthInputFieldBackground, RoundedCornerShape(12.dp))
                                .border(1.98.dp, DeepOlive.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = code[index],
                                onValueChange = { newValue ->
                                    val digit = newValue.filter { it.isDigit() }.takeLast(1)
                                    code[index] = digit
                                    errorMessage = null
                                    if (digit.isNotEmpty() && index < CODE_LENGTH - 1) {
                                        focusRequesters[index + 1].requestFocus()
                                    }
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = Nunito,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = DeepOlive
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                cursorBrush = SolidColor(DeepOlive),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequesters[index])
                                    .onKeyEvent { event ->
                                        if (event.key == Key.Backspace && code[index].isEmpty() && index > 0) {
                                            focusRequesters[index - 1].requestFocus()
                                            true
                                        } else {
                                            false
                                        }
                                    }
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
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
                        .clickable {
                            val enteredCode = code.joinToString("")
                            if (enteredCode.length != CODE_LENGTH || enteredCode.any { !it.isDigit() }) {
                                errorMessage = "Enter the 6-digit code we sent to your email."
                            } else {
                                errorMessage = null
                                onVerified()
                            }
                        }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Verify Email",
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
                    modifier = Modifier.clickable {
                        code.indices.forEach { code[it] = "" }
                        errorMessage = null
                        onResendCode()
                        focusRequesters.first().requestFocus()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = SageAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Resend code",
                        textAlign = TextAlign.Center,
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp,
                        color = SageAccent
                    )
                }

                Text(
                    text = "Didn't receive anything? Check your spam folder or tap Resend.",
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
