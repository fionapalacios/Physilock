package com.example.physi_lock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.PhysiLockDatabase
import com.example.physi_lock.sensor.ShakeDetector
import com.example.physi_lock.sensor.ShakeSensitivity
import com.example.physi_lock.service.AppMonitorService

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current

    // Settings > Motion Lock Sensitivity controls both the shake count and the
    // g-force threshold; load it once before starting the sensor so the very
    // first shake is already measured against the right difficulty.
    var sensitivity by remember { mutableStateOf<ShakeSensitivity?>(null) }
    LaunchedEffect(Unit) {
        val db = PhysiLockDatabase.getInstance(context)
        val cfg = try { db.userConfigurationDao().getActiveConfigurationOnce() } catch (e: Exception) { null }
        sensitivity = ShakeSensitivity.fromLabel(cfg?.motionLockSensitivity)
    }

    val activeSensitivity = sensitivity
    val totalShakesRequired = activeSensitivity?.shakesRequired ?: ShakeSensitivity.MEDIUM.shakesRequired

    // State to track shakes and dynamically force Compose recomposition
    var currentShakes by remember { mutableIntStateOf(0) }

    DisposableEffect(activeSensitivity) {
        if (activeSensitivity == null) {
            // Still loading the configured sensitivity; don't start the sensor
            // with a guessed difficulty.
            return@DisposableEffect onDispose { }
        }

        val detector = ShakeDetector(
            context = context,
            sensitivity = activeSensitivity,
            onShakeProgress = { count: Int ->
                currentShakes = count
            },
            onShakeComplete = {
                // This flags the service to ignore the incoming ghost window states
                com.example.physi_lock.service.AppMonitorService.triggerGlobalUnlock()
                onUnlocked()
            }
        )

        detector.start()
        onDispose { detector.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A2540)), // Deep dark blue tech accent background
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Physi-Lock Active",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Shake your device aggressively to unlock",
            color = Color.LightGray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(40.dp))

        // --- START OF VISUAL SHAKE DISPLAY ---
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            // Background Track Ring
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = Color.White.copy(alpha = 0.1f),
                strokeWidth = 12.dp
            )

            // Dynamic Progress Ring (Fills up as they shake)
            CircularProgressIndicator(
                progress = { currentShakes.toFloat() / totalShakesRequired.toFloat() },
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF4CAF50), // Vibrant Green progress accent
                strokeWidth = 12.dp
            )

            // Central Counter Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$currentShakes",
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "of $totalShakesRequired",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        // --- END OF VISUAL SHAKE DISPLAY ---
    }
}