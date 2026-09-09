package com.example.physi_lock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito

/** Ported from the teammate's Figma "ConfirmSheet" (2026-09-04): a bottom-anchored
 *  confirmation card over a scrim, in place of the generic centered `AlertDialog`s
 *  Sign Out/Reset to Default Settings used before (Sign Out previously had no
 *  confirmation at all -- direct `onClick = onLogout`). Custom Box overlay rather than
 *  Material3's `ModalBottomSheet` -- this app doesn't use that API elsewhere, and a
 *  static confirm card doesn't need its drag/swipe-to-dismiss state machinery. */
@Composable
fun ConfirmSheet(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmColor: Color = com.example.physi_lock.ui.theme.ErrorRed
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(BackgroundLight)
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Text(
                text = title,
                fontFamily = Nunito,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepOlive
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                fontFamily = Nunito,
                fontSize = 14.sp,
                color = MutedText,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(confirmColor)
                    .clickable(onClick = onConfirm)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = confirmLabel, fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BackgroundLight)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepOlive.copy(alpha = 0.06f))
                    .clickable(onClick = onCancel)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Cancel", fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MutedText)
            }
        }
    }
}
