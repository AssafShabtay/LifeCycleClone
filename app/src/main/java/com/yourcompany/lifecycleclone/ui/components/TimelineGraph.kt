package com.yourcompany.lifecycleclone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Represents a segment in a 24‑hour timeline.  [startTime] and [endTime] are epoch
 * milliseconds relative to the same day; [color] is used to draw the segment and [label]
 * identifies the category for debugging or tooltips.  If [endTime] is null, the segment is
 * considered ongoing until the end of the day.
 */
data class TimelineSegment(
    val startTime: Long,
    val endTime: Long?,
    val color: Color,
    val label: String
)

/**
 * Draws a horizontal timeline representing a 24‑hour day.  Each [TimelineSegment] is
 * proportional to its duration relative to the overall day.  Gaps between segments are
 * rendered as a neutral colour to indicate untracked time.  A small stroke is drawn
 * at midnight, 06:00, 12:00 and 18:00 for reference.
 */
@Composable
fun TimelineGraph(
    segments: List<TimelineSegment>,
    startOfDay: Long,
    endOfDay: Long,
    modifier: Modifier = Modifier,
    height: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(width = Dp.Infinity, height = height)) {
        val totalDuration = (endOfDay - startOfDay).coerceAtLeast(1L).toFloat()
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw background for untracked time
        drawRect(
            color = Color(0xFFE0E0E0),
            topLeft = Offset(0f, 0f),
            size = Size(canvasWidth, canvasHeight)
        )

        // Draw each segment
        segments.forEach { seg ->
            val segStart = ((seg.startTime - startOfDay).coerceAtLeast(0L).toFloat() / totalDuration) * canvasWidth
            val segEndTime = seg.endTime ?: endOfDay
            val segEnd = ((segEndTime - startOfDay).coerceAtLeast(0L).toFloat() / totalDuration) * canvasWidth
            val width = (segEnd - segStart).coerceAtLeast(0f)
            drawRect(
                color = seg.color,
                topLeft = Offset(segStart, 0f),
                size = Size(width, canvasHeight)
            )
        }

        // Draw markers at 06:00, 12:00, 18:00
        val markers = listOf(6, 12, 18)
        markers.forEach { hour ->
            val markerTime = startOfDay + hour * 60L * 60L * 1000L
            val x = ((markerTime - startOfDay).toFloat() / totalDuration) * canvasWidth
            drawLine(
                color = Color.Black.copy(alpha = 0.5f),
                start = Offset(x, 0f),
                end = Offset(x, canvasHeight),
                strokeWidth = 1f
            )
        }
    }
}