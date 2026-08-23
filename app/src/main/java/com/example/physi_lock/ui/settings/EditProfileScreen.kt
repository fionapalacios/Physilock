package com.example.physi_lock.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.Account
import com.example.physi_lock.ui.components.AuthFieldLabel
import com.example.physi_lock.ui.components.AuthTextField
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent

/**
 * Ported from the teammate's sprint-2-ui-navigation branch (`ui/screens/settings/EditProfileScreen.kt`).
 * Unlike the original — which read/wrote a local-only `UserConfiguration` row via `ProfileViewModel`,
 * a table this repo's account data doesn't actually live in — this takes [currentAccount]/[onSave]
 * so the caller can wire it to the real `FirebaseAccountRepository.updateAccount`, matching the
 * pattern the existing inline account editor already used. The password-change fields and avatar
 * picker were dropped: the teammate's version never wired them to anything (no password-change or
 * photo-storage backend exists), so keeping them would just be dead UI that looks functional.
 */
@Composable
fun EditProfileScreen(
    currentAccount: Account?,
    onBackClick: () -> Unit,
    onSave: (Account) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    var username by remember(currentAccount) { mutableStateOf(currentAccount?.username.orEmpty()) }
    var fullName by remember(currentAccount) { mutableStateOf(currentAccount?.fullName.orEmpty()) }
    var occupation by remember(currentAccount) { mutableStateOf(currentAccount?.occupation.orEmpty()) }
    val email = currentAccount?.email.orEmpty()

    Column(modifier = modifier.fillMaxSize().background(BackgroundLight)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.79.dp, DeepOlive.copy(alpha = 0.10f))
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
                fontFamily = Nunito,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 18.7.sp,
                color = DeepOlive
            )
            Box(
                modifier = Modifier
                    .background(DeepOlive, RoundedCornerShape(19.dp))
                    .clickable(enabled = currentAccount != null) {
                        currentAccount?.let {
                            onSave(
                                it.copy(
                                    username = username.trim(),
                                    fullName = fullName.trim(),
                                    occupation = occupation.trim().ifBlank { null }
                                )
                            )
                        }
                    }
                    .padding(horizontal = 11.25.dp, vertical = 5.63.dp)
            ) {
                Text(
                    text = "Save",
                    fontFamily = Nunito,
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
                .padding(top = 22.5.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
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
                AuthFieldLabel(text = "Occupation", fontSize = 14.sp, lineHeight = 21.sp)
                Spacer(modifier = Modifier.height(5.63.dp))
                AuthTextField(
                    value = occupation,
                    onValueChange = { occupation = it },
                    placeholder = "Occupation",
                    leadingIcon = Icons.Default.Person,
                    trailingContent = { EditFieldIcon() }
                )
            }

            Column {
                AuthFieldLabel(text = "Email", fontSize = 14.sp, lineHeight = 21.sp)
                Spacer(modifier = Modifier.height(5.63.dp))
                AuthTextField(
                    value = email,
                    onValueChange = {},
                    placeholder = "Email",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )
                Text(
                    text = "Email changes aren't supported yet — contact support to update it.",
                    fontFamily = Nunito,
                    fontSize = 11.sp,
                    color = SageAccent,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFC0392B)
                )
            }
        }
    }
}

@Composable
private fun EditFieldIcon() {
    Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = "Edit",
        tint = SageAccent,
        modifier = Modifier.size(13.dp)
    )
}
