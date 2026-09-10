package com.example.physi_lock.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.CardCream

/** Loading-state placeholder, built only from real palette tokens (CardCream base,
 *  BackgroundLight highlight) -- no shimmer/skeleton pattern existed anywhere in the app
 *  before this (2026-09-10). Same [rememberInfiniteTransition] technique DeepWorkScreen's
 *  pulse animation already uses, for consistency. A fixed-width gradient window sweeps
 *  across the box regardless of its actual size -- a deliberate simplification (a
 *  size-aware shimmer needs onGloballyPositioned/BoxWithConstraints) that still reads as a
 *  shimmer at every card size actually used in this app. */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(15.dp)
) {
    val transition = rememberInfiniteTransition(label = "skeletonShimmer")
    val sweep by transition.animateFloat(
        initialValue = -400f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(animation = tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "skeletonSweep"
    )
    val brush = Brush.linearGradient(
        colors = listOf(CardCream, BackgroundLight, CardCream),
        start = Offset(sweep, 0f),
        end = Offset(sweep + 220f, 220f)
    )
    Box(
        modifier = modifier
            .background(CardCream, shape)
            .background(brush, shape)
    )
}
