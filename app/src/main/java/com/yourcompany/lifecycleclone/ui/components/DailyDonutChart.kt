package com.yourcompany.lifecycleclone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val MINUTES_PER_DAY = 24 * 60

/**
 * Describes a slice for [DailyDonutChart]. Each slice defines a start minute and duration within the
 * 24 hour day together with colour and optional overrides for stroke width, gap and stroke cap.
 */
data class DailyDonutSlice(
    val label: String,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val color: Color,
    val strokeWidthFractionOverride: Float? = null,
    val gapDegreesOverride: Float? = null,
    val strokeCapOverride: StrokeCap? = null
)

/**
 * Draws a circular 24 hour view similar to the Life Cycle day ring. Slices can be customised with
 * different stroke widths, gaps and stroke caps. A background track is rendered for untracked time
 * and optional hour ticks provide context.
 */
@Composable
fun DailyDonutChart(
    slices: List<DailyDonutSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    defaultStrokeWidthFraction: Float = 0.18f,
    defaultGapDegrees: Float = 2f,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    showHourTicks: Boolean = true,
    tickColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
    tickHours: List<Int> = listOf(0, 6, 12, 18),
    centerContent: (@Composable () -> Unit)? = null
) {
    val resolvedDefaultStroke = defaultStrokeWidthFraction.coerceIn(0.05f, 0.5f)
    val sanitizedSlices = mutableListOf<DailyDonutSlice>()

    slices.forEach { slice ->
        var remaining = slice.durationMinutes.coerceAtLeast(0)
        if (remaining == 0) return@forEach
        var currentStart = normalizeStart(slice.startMinuteOfDay)
        while (remaining > 0) {
            val available = MINUTES_PER_DAY - currentStart
            val chunk = min(remaining, available)
            if (chunk > 0) {
                sanitizedSlices.add(
                    slice.copy(startMinuteOfDay = currentStart, durationMinutes = chunk)
                )
            }
            remaining -= chunk
            currentStart = 0
        }
    }

    val cleanedSlices = sanitizedSlices.sortedBy { it.startMinuteOfDay }
    val maxStrokeFraction = cleanedSlices.fold(resolvedDefaultStroke) { acc, slice ->
        max(acc, (slice.strokeWidthFractionOverride ?: resolvedDefaultStroke).coerceIn(0.05f, 0.5f))
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasMinDim = this.size.minDimension
            val maxStrokeWidth = canvasMinDim * maxStrokeFraction
            val resolvedDefaultGap = defaultGapDegrees.coerceAtLeast(0f)

            inset(maxStrokeWidth / 2f) {
                drawArc(
                    color = backgroundColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = maxStrokeWidth, cap = StrokeCap.Butt)
                )

                cleanedSlices.forEach { slice ->
                    val strokeFraction = (slice.strokeWidthFractionOverride ?: resolvedDefaultStroke)
                        .coerceIn(0.05f, 0.5f)
                    val strokeWidth = canvasMinDim * strokeFraction
                    val gap = (slice.gapDegreesOverride ?: resolvedDefaultGap).coerceIn(0f, 45f)

                    val startAngle = minuteToAngle(slice.startMinuteOfDay) + gap / 2f
                    val sweepAngle = minuteDurationToSweep(slice.durationMinutes) - gap
                    if (sweepAngle <= 0f) return@forEach

                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(
                            width = strokeWidth,
                            cap = slice.strokeCapOverride ?: StrokeCap.Round
                        )
                    )
                }
            }

            if (showHourTicks && tickHours.isNotEmpty()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val outerRadius = canvasMinDim / 2f
                val innerRadius = (outerRadius - maxStrokeWidth).coerceAtLeast(0f)
                val tickStroke = max(1f, maxStrokeWidth * 0.08f)

                tickHours.distinct().mapNotNull { hour ->
                    hour.takeIf { it in 0..24 }
                }.forEach { hour ->
                    val angleDegrees = minuteToAngle(hour * 60)
                    val angleRad = angleDegrees.toDouble() * (PI / 180.0)
                    val cosAngle = cos(angleRad)
                    val sinAngle = sin(angleRad)
                    val startX = center.x + cosAngle.toFloat() * innerRadius
                    val startY = center.y + sinAngle.toFloat() * innerRadius
                    val endX = center.x + cosAngle.toFloat() * outerRadius
                    val endY = center.y + sinAngle.toFloat() * outerRadius
                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        centerContent?.invoke()
    }
}

private fun normalizeStart(start: Int): Int {
    val mod = start % MINUTES_PER_DAY
    return if (mod >= 0) mod else mod + MINUTES_PER_DAY
}

private fun minuteToAngle(minute: Int): Float {
    val clamped = minute.coerceIn(0, MINUTES_PER_DAY)
    val fraction = clamped.toFloat() / MINUTES_PER_DAY.toFloat()
    return -90f + fraction * 360f
}

private fun minuteDurationToSweep(duration: Int): Float {
    val clamped = duration.coerceIn(0, MINUTES_PER_DAY)
    return clamped.toFloat() / MINUTES_PER_DAY.toFloat() * 360f
}