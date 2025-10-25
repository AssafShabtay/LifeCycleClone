package com.yourcompany.lifecycleclone.insights

/**
 * Represents a simple correlation between a sleep session and the category of the last activity
 * before that sleep.  [category] is the category name (e.g. "work", "home"), [count] is the
 * number of sleep sessions that were preceded by that category, and [fraction] is the
 * proportion of all considered sleep sessions that were preceded by that category.
 */
data class SleepCorrelation(
    val category: String,
    val count: Int,
    val fraction: Float
)