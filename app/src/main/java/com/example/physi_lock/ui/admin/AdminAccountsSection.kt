package com.example.physi_lock.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.Account
import com.example.physi_lock.data.Role
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.CardCream
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.DmMono
import com.example.physi_lock.ui.theme.ErrorRed
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage
import com.example.physi_lock.ui.theme.TertiaryTan

@Composable
fun AdminAccountsSection(
    accounts: List<Account>,
    errorMessage: String?,
    onRetry: () -> Unit,
    onSetActive: (Account, Boolean) -> Unit,
    onDelete: (Account) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<Account?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // A real error doesn't blank the screen -- if accounts has stale cached data from
        // before the listener failed, that list stays visible underneath this banner
        // instead of the previous silent-forever-stale behavior with no indication at all.
        errorMessage?.let { message ->
            AdminErrorBanner(message = message, onRetry = onRetry)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (accounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (errorMessage != null) "Couldn't load accounts" else "No accounts yet",
                    fontFamily = Nunito,
                    color = MutedText
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(accounts, key = { it.id }) { account ->
                AdminAccountCard(
                    account = account,
                    onSetActive = { active -> onSetActive(account, active) },
                    onDelete = { pendingDelete = account }
                )
            }
            item { Spacer(modifier = Modifier.height(4.dp)) }
        }
    }

    pendingDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete account?") },
            text = { Text("This permanently removes ${account.fullName} (@${account.username}). This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(account)
                    pendingDelete = null
                }) { Text("Delete", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminAccountCard(
    account: Account,
    onSetActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isAdmin = account.role == Role.ADMIN

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardCream, RoundedCornerShape(22.5.dp))
            .border(0.8.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(22.5.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SecondarySage.copy(alpha = 0.13f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = account.fullName.trim().take(1).uppercase().ifEmpty { "?" },
                    fontFamily = Nunito,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = SageAccent
                )
            }

            Spacer(modifier = Modifier.width(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.fullName,
                        fontFamily = Nunito,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DeepOlive
                    )
                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ADMIN",
                            fontFamily = Nunito,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = SageAccent
                        )
                    }
                }
                Text("@${account.username}", fontFamily = DmMono, fontSize = 11.sp, color = SageAccent)
                Text(account.email, fontFamily = DmMono, fontSize = 11.sp, color = MutedText)
            }

            Spacer(modifier = Modifier.width(8.dp))
            StatusPill(active = account.isActive)

            if (!isAdmin) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete account",
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 11.dp),
            color = DeepOlive.copy(alpha = 0.08f),
            thickness = 0.8.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (account.isActive) "Deactivate account" else "Activate account",
                fontFamily = Nunito,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MutedText
            )
            Switch(
                checked = account.isActive,
                enabled = !isAdmin,
                onCheckedChange = onSetActive,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = DeepOlive,
                    checkedThumbColor = BackgroundLight,
                    uncheckedTrackColor = TertiaryTan,
                    uncheckedThumbColor = BackgroundLight,
                    uncheckedBorderColor = DeepOlive.copy(alpha = 0.25f)
                )
            )
        }
    }
}

@Composable
private fun StatusPill(active: Boolean) {
    val background = if (active) SageAccent.copy(alpha = 0.09f) else ErrorRed.copy(alpha = 0.07f)
    val border = if (active) SageAccent.copy(alpha = 0.27f) else ErrorRed.copy(alpha = 0.20f)
    val textColor = if (active) SageAccent else ErrorRed

    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .border(0.8.dp, border, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (active) "Active" else "Inactive",
            fontFamily = DmMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = textColor
        )
    }
}
