package com.yourcompany.lifecycleclone.ui.util

import androidx.compose.ui.graphics.Color

/**
 * Provides a deterministic color per activity category.
 *
 * - Known categories get curated Material-ish colors for visual consistency.
 * - Unknown categories are hashed into a stable, unique color.
 */
fun getColorForCategory(category: String): Color {
    val key = category.trim().lowercase()

    return when (key) {
        "home" -> Color(0xFF4CAF50)
        "work" -> Color(0xFF2196F3)
        "gym", "exercise" -> Color(0xFFFF9800)
        "driving", "vehicle" -> Color(0xFFFFC107)
        "walking", "on foot" -> Color(0xFF8BC34A)
        "running" -> Color(0xFFF44336) // red
        "cycling", "bicycle" -> Color(0xFF9C27B0) // purple
        "unknown" -> Color(0xFF9E9E9E)
        else -> {
            // Stable pseudo-random color from the category string.
            val hash = key.hashCode()

            // Extract RGB bytes
            val r = (hash shr 16) and 0xFF
            val g = (hash shr 8) and 0xFF
            val b = hash and 0xFF

            // Use explicit RGBA constructor to avoid deprecated Color(Int) usage
            Color(
                red = r,
                green = g,
                blue = b,
                alpha = 0xFF
            )
        }
    }
}
