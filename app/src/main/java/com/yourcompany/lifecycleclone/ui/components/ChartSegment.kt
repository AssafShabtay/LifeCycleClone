package com.yourcompany.lifecycleclone.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Represents a slice in a pie or donut chart.  Each segment has a label, the fraction of
 * the whole (between 0 and 1), and an associated color for the segment.  Use this when
 * constructing a [DonutChart] to provide visually appealing colours and labels.
 */
data class ChartSegment(
    val label: String,
    val fraction: Float,
    val color: Color
)