package com.example.physi_lock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.physi_lock.sensor.ShakeDetector

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val detector = ShakeDetector(context) { onUnlocked() }
        detector.start()
        onDispose { detector.stop() } // unregisters sensor when screen leaves
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A2540)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Shake to unlock", color = Color.White, fontSize = 22.sp)
    }
}