package com.example.physi_lock.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.example.physi_lock.data.Account
import com.example.physi_lock.data.Role
import com.example.physi_lock.ui.theme.Danger
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.SageAccent

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
                    if (errorMessage != null) "Couldn't load accounts" else "No accounts yet",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(accounts, key = { it.id }) { account ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(account.fullName, fontWeight = FontWeight.SemiBold)
                            if (account.role == Role.ADMIN) {
                                Text(
                                    text = "  ADMIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SageAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "@${account.username} · ${account.email}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (account.isActive) "Active" else "Deactivated",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (account.isActive) DeepOlive else Danger
                        )
                    }
                    Switch(
                        checked = account.isActive,
                        enabled = account.role != Role.ADMIN,
                        onCheckedChange = { checked -> onSetActive(account, checked) }
                    )
                    IconButton(
                        onClick = { pendingDelete = account },
                        enabled = account.role != Role.ADMIN
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete account", tint = Danger)
                    }
                }
            }
            }
        }
    } // end Column

    pendingDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete account?") },
            text = { Text("This permanently removes ${account.fullName} (@${account.username}). This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(account)
                    pendingDelete = null
                }) { Text("Delete", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}
