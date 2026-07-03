package com.example.physi_lock.ui.landing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideIn
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.Orchid
import com.example.physi_lock.ui.theme.SageAccent
import kotlinx.coroutines.delay

// Near-white gradient stops specific to this hero banner; not general design tokens
// (same pattern LockScreen.kt uses for its own one-off full-bleed background).
private val LandingSurface = Color(0xFFFEFEFE)
private val HeroGradientTop = Color(0xFFBAC892)
private val HeroGradientMid = Color(0xFFDAD5BB)
private val ModuleCardSurface = Color(0xFFF5F3EB)

private data class PhysiLockModule(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val description: String
)

private val modules = listOf(
    PhysiLockModule(Icons.Filled.MonitorHeart, "Core Monitoring", SageAccent, "Track screen time & overuse alerts"),
    PhysiLockModule(Icons.Filled.Psychology, "AI Behavior Analysis", Orchid, "Addiction risk scoring & predictions"),
    PhysiLockModule(Icons.Filled.FitnessCenter, "Motion Lock", SageAccent, "Move-to-unlock physical challenges"),
    PhysiLockModule(Icons.Filled.Shield, "Smart Intervention", MutedText, "Focus mode & goal-based limits"),
    PhysiLockModule(Icons.Filled.Favorite, "Mental Wellness", Orchid, "Break reminders & reflection prompts"),
    PhysiLockModule(Icons.Filled.Tune, "Personalization", SageAccent, "Student & Work mode profiles"),
    PhysiLockModule(Icons.Filled.Memory, "Context AI", MutedText, "Location-aware, doomscrolling detection")
)

private data class LandingStat(val value: String, val unit: String)

private val stats = listOf(
    LandingStat("7", "Modules"),
    LandingStat("13+", "Features"),
    LandingStat("AI", "Powered")
)

@Composable
fun LandingScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LandingSurface)
            .verticalScroll(rememberScrollState())
    ) {
        HeroSection()
        ModulesSection()
        CtaSection(onGetStarted = onGetStarted)
    }
}

@Composable
private fun HeroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to HeroGradientTop,
                    0.6f to HeroGradientMid,
                    1f to LandingSurface
                )
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedEntrance(delayMillis = 0) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(DeepOlive),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Eco,
                    contentDescription = null,
                    tint = HeroGradientTop,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedEntrance(delayMillis = 150) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Physi-Lock",
                    fontFamily = Nunito,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = DeepOlive
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Move your body. Reclaim your mind.",
                    fontFamily = Nunito,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedEntrance(delayMillis = 300) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepOlive.copy(alpha = 0.07f))
                    .border(1.dp, DeepOlive.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("AI-powered screen time control that rewards ")
                        withStyle(androidx.compose.ui.text.SpanStyle(color = MutedText, fontWeight = FontWeight.Bold)) {
                            append("physical movement")
                        }
                        append(" and protects your ")
                        withStyle(androidx.compose.ui.text.SpanStyle(color = MutedText, fontWeight = FontWeight.Bold)) {
                            append("mental wellness")
                        }
                        append(".")
                    },
                    style = TextStyle(
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = DeepOlive,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedEntrance(delayMillis = 450) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stats.forEach { stat ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.6f))
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stat.value,
                            fontFamily = Nunito,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = DeepOlive
                        )
                        Text(
                            text = stat.unit,
                            fontFamily = Nunito,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModulesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modules.forEachIndexed { index, module ->
            AnimatedEntrance(delayMillis = 500 + index * 100, slideFromStart = true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ModuleCardSurface)
                        .border(1.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(module.color.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = module.icon,
                            contentDescription = null,
                            tint = module.color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = module.label,
                            fontFamily = Nunito,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DeepOlive
                        )
                        Text(
                            text = module.description,
                            fontFamily = Nunito,
                            fontSize = 13.sp,
                            color = MutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(module.color)
                    )
                }
            }
        }
    }
}

@Composable
private fun CtaSection(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedEntrance(delayMillis = 1200) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepOlive,
                    contentColor = LandingSurface
                )
            ) {
                Text(
                    text = "Get Started",
                    fontFamily = Nunito,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Free · No ads · Your data stays on device",
            fontFamily = Nunito,
            fontSize = 13.sp,
            color = SageAccent
        )
    }
}

@Composable
private fun AnimatedEntrance(
    delayMillis: Int,
    slideFromStart: Boolean = false,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + slideIn(
            animationSpec = tween(400),
            initialOffset = { size ->
                if (slideFromStart) IntOffset(-size.width / 6, 0) else IntOffset(0, size.height / 6)
            }
        )
    ) {
        content()
    }
}
