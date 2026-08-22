package com.prototype.physi_lock.ui.screens.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prototype.physi_lock.ui.components.AuthFieldLabel
import com.prototype.physi_lock.ui.components.AuthTextField
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen
import com.prototype.physi_lock.ui.theme.SecondarySage

@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val profile by profileViewModel.profile.collectAsState()

    var username by remember { mutableStateOf(profile.username) }
    var fullName by remember { mutableStateOf(profile.fullName) }
    var email by remember { mutableStateOf(profile.email) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(BackgroundLight)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.79.dp, PrimaryDark.copy(alpha = 0.10f))
                .padding(horizontal = 15.dp, vertical = 11.25.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.25.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DeepOlive,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "Edit Profile",
                modifier = Modifier.weight(1f),
                fontFamily = NunitoFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 18.7.sp,
                color = PrimaryDark
            )
            Box(
                modifier = Modifier
                    .background(PrimaryDark, RoundedCornerShape(19.dp))
                    .clickable {
                        profileViewModel.updateProfile(
                            fullName = fullName.trim(),
                            username = username.trim(),
                            email = email.trim()
                        )
                        onSaveClick()
                    }
                    .padding(horizontal = 11.25.dp, vertical = 5.63.dp)
            ) {
                Text(
                    text = "Save",
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.5.sp,
                    color = BackgroundLight
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.75.dp)
                .padding(top = 7.5.dp, bottom = 30.dp)
        ) {
            AvatarPicker()

            Spacer(modifier = Modifier.height(22.5.dp))

            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Column {
                    AuthFieldLabel(text = "Username", fontSize = 14.sp, lineHeight = 21.sp)
                    Spacer(modifier = Modifier.height(5.63.dp))
                    AuthTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = "Username",
                        leadingIcon = Icons.Default.Person,
                        trailingContent = { EditFieldIcon() }
                    )
                }

                Column {
                    AuthFieldLabel(text = "Full Name", fontSize = 14.sp, lineHeight = 21.sp)
                    Spacer(modifier = Modifier.height(5.63.dp))
                    AuthTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = "Full Name",
                        leadingIcon = Icons.Default.Person,
                        trailingContent = { EditFieldIcon() }
                    )
                }

                Column {
                    AuthFieldLabel(text = "Email", fontSize = 14.sp, lineHeight = 21.sp)
                    Spacer(modifier = Modifier.height(5.63.dp))
                    AuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        trailingContent = { EditFieldIcon() }
                    )
                }

                Column {
                    AuthFieldLabel(text = "Change Password", fontSize = 14.sp, lineHeight = 21.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(11.25.dp)) {
                        AuthTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            placeholder = "Current password",
                            leadingIcon = Icons.Default.Lock,
                            isPassword = true
                        )
                        AuthTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            placeholder = "New password",
                            leadingIcon = Icons.Default.Lock,
                            isPassword = !newPasswordVisible,
                            trailingContent = {
                                Icon(
                                    imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
                                    tint = PrimaryGreen,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { newPasswordVisible = !newPasswordVisible }
                                )
                            }
                        )
                        AuthTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = "Confirm new password",
                            leadingIcon = Icons.Default.Lock,
                            isPassword = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditFieldIcon() {
    Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = "Edit",
        tint = PrimaryGreen,
        modifier = Modifier.size(13.dp)
    )
}

@Composable
private fun AvatarPicker() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SecondarySage, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🌿", fontSize = 32.sp)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .background(PrimaryDark, CircleShape)
                    .border(1.2.dp, BackgroundLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change photo",
                    tint = SecondarySage,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(11.25.dp))
        Text(
            text = "Tap to change photo",
            fontFamily = NunitoFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 19.5.sp,
            color = PrimaryGreen
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun EditProfileScreenPreview() {
    PhysiLockTheme {
        EditProfileScreen(onBackClick = {}, onSaveClick = {})
    }
}
