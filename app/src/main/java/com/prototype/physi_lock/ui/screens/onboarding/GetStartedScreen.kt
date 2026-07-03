package com.prototype.physi_lock.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.ui.components.DashboardCard
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.SecondarySage
import com.prototype.physi_lock.ui.theme.TertiaryTan
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Reproduces CSS `linear-gradient(<angleDeg>deg, ...)` line placement for a given box [size]. */
private fun cssLinearGradient(
    angleDeg: Float,
    colorStops: Array<Pair<Float, Color>>,
    size: Size
): Brush {
    val angleRad = Math.toRadians(angleDeg.toDouble())
    val dx = sin(angleRad).toFloat()
    val dy = -cos(angleRad).toFloat()
    val length = abs(size.width * dx) + abs(size.height * dy)
    val center = Offset(size.width / 2f, size.height / 2f)
    val half = length / 2f
    val start = Offset(center.x - dx * half, center.y - dy * half)
    val end = Offset(center.x + dx * half, center.y + dy * half)
    return Brush.linearGradient(colorStops = colorStops, start = start, end = end)
}

private data class CoreModule(val title: String, val description: String)

private val coreModules = listOf(
    CoreModule("Core Monitoring", "Track screen time & overuse alerts"),
    CoreModule("AI Behavior Analysis", "Addiction risk scoring & predictions"),
    CoreModule("Motion Lock", "Move-to-unlock physical challenges"),
    CoreModule("Smart Intervention", "Focus mode & goal-based limits"),
    CoreModule("Mental Wellness", "Break reminders & reflection prompts"),
    CoreModule("Personalization", "Student & Work mode profiles"),
    CoreModule("Context AI", "Location-aware, doomscrolling detection")
)

@Composable
fun GetStartedScreen(
    onGetStartedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .align(Alignment.TopCenter)
                .drawWithCache {
                    val brush = cssLinearGradient(
                        angleDeg = 160f,
                        colorStops = arrayOf(
                            0.0849f to SecondarySage,
                            0.583f to TertiaryTan,
                            0.9151f to BackgroundLight
                        ),
                        size = size
                    )
                    onDrawBehind { drawRect(brush) }
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Physi-Lock",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Move your body. Reclaim your mind.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = PrimaryDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .background(
                        color = PrimaryDark.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(15.dp)
                    )
                    .border(
                        width = 0.792.dp,
                        color = PrimaryDark.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(15.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "AI-powered screen time control that rewards physical movement " +
                        "and protects your mental wellness.",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 22.75.sp,
                    textAlign = TextAlign.Center,
                    color = PrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(value = "7", label = "Modules", modifier = Modifier.weight(1f))
                StatItem(value = "13+", label = "Features", modifier = Modifier.weight(1f))
                StatItem(value = "AI", label = "Powered", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "CORE MODULES",
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            coreModules.forEach { module ->
                DashboardCard(title = module.title, description = module.description)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onGetStartedClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Get Started →",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.60f),
                shape = RoundedCornerShape(19.dp)
            )
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GetStartedScreenPreview() {
    PhysiLockTheme {
        GetStartedScreen(onGetStartedClick = {})
    }
}
