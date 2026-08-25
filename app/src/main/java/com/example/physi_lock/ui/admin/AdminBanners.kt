package com.example.physi_lock.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.physi_lock.ui.theme.ErrorRed
import com.example.physi_lock.ui.theme.MutedText

/**
 * Admin governance (known gap tracked since 2026-08-09): shared "couldn't load, tap to
 * retry" / "couldn't save" / "you're offline" banner shapes, so AdminHomeScreen and each
 * section render these consistently instead of failing silently as before.
 */
@Composable
fun AdminErrorBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = ErrorRed, modifier = Modifier.padding(end = 8.dp))
        Text(message, modifier = Modifier.weight(1f), color = ErrorRed, style = MaterialTheme.typography.bodySmall)
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text("Retry", color = ErrorRed) }
        }
        if (onDismiss != null) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = ErrorRed)
            }
        }
    }
}

@Composable
fun AdminOfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MutedText.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MutedText, modifier = Modifier.padding(end = 8.dp))
        Text("You're offline — showing the last synced data", color = MutedText, style = MaterialTheme.typography.bodySmall)
    }
}
