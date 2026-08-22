package com.prototype.physi_lock.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.R
import com.prototype.physi_lock.ui.theme.AccentLavender
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen
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

private data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

private val onboardingSlides = listOf(
    OnboardingSlide(
        "Core Monitoring",
        "Real-time tracking of app usage, screen unlocks, and overuse alerts so you always know where your time goes.",
        Icons.Filled.Visibility,
        PrimaryGreen
    ),
    OnboardingSlide(
        "AI Behavior Analysis",
        "Personalized addiction risk scoring and early predictions that flag unhealthy patterns before they take hold.",
        Icons.Filled.Psychology,
        AccentLavender
    ),
    OnboardingSlide(
        "Motion-Responsive Locking",
        "Physical move-to-unlock challenges that turn screen time limits into a quick burst of real movement.",
        Icons.Filled.Bolt,
        PrimaryGreen
    ),
    OnboardingSlide(
        "Smart Intervention",
        "Focus mode sessions and goal-based limits step in automatically the moment you start to drift.",
        Icons.Filled.Shield,
        DeepOlive
    ),
    OnboardingSlide(
        "Mental Wellness",
        "Timed break reminders, guided daily reflections, and wellness nudges when you need them most.",
        Icons.Filled.Favorite,
        AccentLavender
    ),
    OnboardingSlide(
        "Personalization",
        "Student and Work mode profiles tailor limits, schedules, and nudges to fit how you actually live.",
        Icons.Filled.Tune,
        PrimaryGreen
    ),
    OnboardingSlide(
        "Context-Aware AI",
        "Location-aware detection spots doomscrolling and context switches so interventions land at the right moment.",
        Icons.Filled.LocationOn,
        DeepOlive
    )
)

@Composable
fun GetStartedScreen(
    onGetStartedClick: () -> Unit,
    onLoginClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { onboardingSlides.size })

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = cssLinearGradient(
                    angleDeg = 170f,
                    colorStops = arrayOf(
                        0.0f to SecondarySage,
                        0.45f to TertiaryTan,
                        1.0f to BackgroundLight
                    ),
                    size = size
                )
                onDrawBehind { drawRect(brush) }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.5.dp)
                    .padding(top = 28.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Physi-Lock logo",
                    modifier = Modifier
                        .size(128.dp)
                        .padding(bottom = 14.dp)
                )
                Text(
                    text = "Physi-Lock",
                    textAlign = TextAlign.Center,
                    fontFamily = NunitoFontFamily,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 33.sp,
                    color = PrimaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Move your body. Reclaim your mind.",
                    textAlign = TextAlign.Center,
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.5.sp,
                    color = DeepOlive
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .fillMaxWidth(0.78f)
                            .height(180.dp)
                            .background(BackgroundLight.copy(alpha = 0.30f), RoundedCornerShape(23.dp))
                            .border(1.5.dp, PrimaryDark.copy(alpha = 0.12f), RoundedCornerShape(23.dp))
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 25.dp)
                            .fillMaxWidth(0.74f)
                            .height(171.dp)
                            .background(BackgroundLight.copy(alpha = 0.17f), RoundedCornerShape(22.dp))
                            .border(1.35.dp, PrimaryDark.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth(0.86f)
                            .height(200.dp)
                            .background(BackgroundLight.copy(alpha = 0.92f), RoundedCornerShape(22.5.dp))
                            .border(1.5.dp, PrimaryDark.copy(alpha = 0.14f), RoundedCornerShape(22.5.dp))
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(22.5.dp)
                        ) { page ->
                            val slide = onboardingSlides[page]
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(slide.accentColor.copy(alpha = 0.09f), RoundedCornerShape(15.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = slide.icon,
                                            contentDescription = null,
                                            tint = slide.accentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        text = "${page + 1} / ${onboardingSlides.size}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 19.5.sp,
                                        color = PrimaryGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(15.dp))
                                Text(
                                    text = slide.title,
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 24.sp,
                                    color = PrimaryDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = slide.description,
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 20.15.sp,
                                    color = DeepOlive
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.75.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.63.dp)
                ) {
                    onboardingSlides.indices.forEach { index ->
                        val isActive = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isActive) 18.dp else 6.dp)
                                .background(
                                    color = if (isActive) PrimaryDark else TertiaryTan,
                                    shape = RoundedCornerShape(50)
                                )
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(thickness = 1.5.dp, color = PrimaryDark.copy(alpha = 0.12f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.75.dp)
                        .padding(top = 7.5.dp, bottom = 30.dp)
                ) {
                    Button(
                        onClick = onGetStartedClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryDark,
                            contentColor = BackgroundLight
                        )
                    ) {
                        Text(
                            text = "Get Started",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = BackgroundLight,
                            modifier = Modifier.height(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(11.25.dp))

                    OutlinedButton(
                        onClick = onLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.5.dp, PrimaryDark.copy(alpha = 0.18f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryDark
                        )
                    ) {
                        Text(
                            text = "I already have an account",
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 21.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GetStartedScreenPreview() {
    PhysiLockTheme {
        GetStartedScreen(onGetStartedClick = {})
    }
}
