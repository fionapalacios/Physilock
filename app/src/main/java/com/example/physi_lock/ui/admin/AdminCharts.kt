package com.example.physi_lock.ui.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.theme.DmMono
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** One bar series for [AdminDailyBarChart] — a label+color (used for the legend when there's
 *  more than one series) plus one value per day, aligned to that chart's dayLabels. */
data class ChartSeries(val label: String, val color: Color, val values: List<Int>)

/** Rounds a data max up to a "nice" axis top (e.g. 360 rather than 314) split into [steps]
 *  even gridlines, the same rounding a hand-built chart-authoring tool like Figma would use. */
private fun niceAxisMax(maxValue: Int, steps: Int): Int {
    if (maxValue <= 0) return steps
    val rawStep = maxValue.toDouble() / steps
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val normalized = rawStep / magnitude
    val niceNormalized = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    var step = (niceNormalized * magnitude).toInt().coerceAtLeast(1)
    while (step * steps < maxValue) step *= 2
    return step * steps
}

/**
 * Small bar chart matching the Admin Console's Figma card style: DM Mono axis labels in sage,
 * one bar per day for a single series, or grouped bars per day (Top Apps) for several — with
 * a color-swatch legend underneath when there's more than one series, same as the mockup.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDailyBarChart(
    dayLabels: List<String>,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    axisSteps: Int = 4
) {
    val textMeasurer = rememberTextMeasurer()
    val axisLabelStyle = TextStyle(fontFamily = DmMono, fontSize = 11.sp, color = SageAccent)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val maxValue = series.maxOfOrNull { s -> s.values.maxOrNull() ?: 0 } ?: 0
            val axisMax = niceAxisMax(maxValue, axisSteps)
            // Sized to the widest axis label actually on screen (the top one, since labels only
            // grow with i) rather than a fixed guess -- so a chart whose values grow into 3+
            // digits (e.g. Top Apps' minute totals) never clips or overlaps its own axis text.
            val axisMaxLabelWidth = textMeasurer.measure(axisMax.toString(), axisLabelStyle).size.width.toFloat()
            val leftAxisWidth = axisMaxLabelWidth + 10.dp.toPx()
            val bottomLabelsHeight = 18.dp.toPx()
            val chartTop = 6.dp.toPx()
            val chartBottom = size.height - bottomLabelsHeight
            val chartHeight = chartBottom - chartTop
            val chartLeft = leftAxisWidth
            val chartWidth = size.width - chartLeft

            for (i in 0..axisSteps) {
                val value = axisMax * i / axisSteps
                val y = chartBottom - (chartHeight * i / axisSteps)
                val label = textMeasurer.measure(value.toString(), axisLabelStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = value.toString(),
                    style = axisLabelStyle,
                    topLeft = Offset(chartLeft - label.size.width.toFloat() - 6.dp.toPx(), y - label.size.height / 2f)
                )
            }

            val dayCount = dayLabels.size.coerceAtLeast(1)
            val slotWidth = chartWidth / dayCount
            val seriesCount = series.size.coerceAtLeast(1)
            val barGap = 3.dp.toPx()
            val groupPadding = 6.dp.toPx()
            val barWidth = ((slotWidth - groupPadding * 2 - barGap * (seriesCount - 1)) / seriesCount).coerceAtLeast(2f)

            dayLabels.forEachIndexed { dayIndex, day ->
                val slotLeft = chartLeft + slotWidth * dayIndex
                series.forEachIndexed { seriesIndex, s ->
                    val value = s.values.getOrElse(dayIndex) { 0 }
                    val barHeight = if (axisMax > 0) chartHeight * value / axisMax else 0f
                    val barLeft = slotLeft + groupPadding + seriesIndex * (barWidth + barGap)
                    drawRect(
                        color = s.color,
                        topLeft = Offset(barLeft, chartBottom - barHeight),
                        size = Size(barWidth, barHeight)
                    )
                }
                val dayLabel = textMeasurer.measure(day, axisLabelStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = day,
                    style = axisLabelStyle,
                    topLeft = Offset(
                        slotLeft + slotWidth / 2f - dayLabel.size.width / 2f,
                        chartBottom + 6.dp.toPx()
                    )
                )
            }
        }

        if (series.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                series.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(width = 14.dp, height = 4.dp)
                                .background(s.color, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = s.label, fontFamily = Nunito, fontSize = 12.sp, color = s.color)
                    }
                }
            }
        }
    }
}
