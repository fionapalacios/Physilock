package com.example.physi_lock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** [outlineColor], when set, draws the ring in the mockup's "sandwich" style -- a wider
 *  outline-colored stroke underneath each arc (track and progress) with the normal-width
 *  track/progress color drawn on top of it, so a thin outline peeks out on both edges.
 *  Defaults to `null` (the original single-stroke look) so every existing call site is
 *  unaffected unless it opts in. */
@Composable
fun CircularProgressRing(
    progress: Float,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier,
    ringSize: Dp = 100.dp,
    strokeWidth: Dp = 10.dp,
    outlineColor: Color? = null,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokePx = strokeWidth.toPx()
            // The widest stroke drawn determines how much inset the arc needs to stay fully
            // inside the canvas -- only the outline path (when enabled) draws anything wider
            // than strokePx, so the geometry below is identical to the pre-outline version
            // whenever outlineColor is null, keeping every existing call site pixel-for-pixel
            // unchanged.
            val outlineStrokePx = strokePx + 2.dp.toPx()
            val widestStrokePx = if (outlineColor != null) outlineStrokePx else strokePx
            val inset = widestStrokePx / 2f
            val arcSize = Size(size.width - widestStrokePx, size.height - widestStrokePx)
            val topLeft = Offset(inset, inset)
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
            val clampedProgress = progress.coerceIn(0f, 1f)

            if (outlineColor != null) {
                drawArc(
                    color = outlineColor.copy(alpha = 0.18f),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(width = outlineStrokePx, cap = StrokeCap.Round)
                )
            }
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            if (outlineColor != null && clampedProgress > 0f) {
                drawArc(
                    color = outlineColor,
                    startAngle = -90f, sweepAngle = 360f * clampedProgress, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(width = outlineStrokePx, cap = StrokeCap.Round)
                )
            }
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * clampedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }
        content()
    }
}
