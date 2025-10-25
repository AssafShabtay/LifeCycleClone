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
import com.yourcompany.lifecycleclone.insights.CategoryBreakdown

/**
 * Simple donut chart implementation using a [Canvas].  It draws a ring segmented by
 * [CategoryBreakdown] values.  Colors are generated deterministically based on the category
 * hash.  For a more polished look you might inject your own palette or use the colors
 * specified in [PlaceEntity].
 */
@Composable
fun DonutChart(
    segments: List<CategoryBreakdown>,
    modifier: Modifier = Modifier,
    diameter: Dp = 200.dp // <-- renamed from `size`
) {
    Canvas(
        modifier = modifier.size(diameter) // still use it to size the Canvas
    ) {
        val totalMinutes = segments.sumOf { it.totalMinutes }.coerceAtLeast(1)
        var startAngle = -90f

        // NOW `size` here is DrawScope.size (androidx.compose.ui.geometry.Size)
        // which has width, height, and the extension property `minDimension`
        val strokeWidth = size.minDimension * 0.15f

        segments.forEach { segment ->
            val sweep = (segment.totalMinutes.toFloat() / totalMinutes.toFloat()) * 360f
            val color = Color(segment.category.hashCode() or 0xFF000000.toInt())

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset.Zero,
                size = Size(size.minDimension, size.minDimension),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            startAngle += sweep
        }
    }
}
