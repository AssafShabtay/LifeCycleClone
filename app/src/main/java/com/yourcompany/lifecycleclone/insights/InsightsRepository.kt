package com.yourcompany.lifecycleclone.insights

import com.yourcompany.lifecycleclone.core.db.VisitDao
import com.yourcompany.lifecycleclone.core.db.VisitWithPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for computing summaries of time spent in various categories.  The
 * calculations are performed off the main thread using coroutines.  The algorithms here are
 * simplified examples; a real implementation would handle overlapping visits, partial days and
 * multi‑week ranges.
 */
class InsightsRepository(private val visitDao: VisitDao) {

    /**
     * Returns a list of [CategoryBreakdown] objects summarising the time spent in each category
     * over the given interval.  The [from] and [to] parameters should be epoch milliseconds.
     */
    suspend fun getBreakdown(from: Long, to: Long): List<CategoryBreakdown> = withContext(Dispatchers.IO) {
        val visits: List<VisitWithPlace> = visitDao.getVisitsInRange(from, to)
        val totals: MutableMap<String, Long> = mutableMapOf()
        var totalDuration: Long = 0L
        visits.forEach { visit ->
            val end = visit.endTime ?: System.currentTimeMillis()
            val duration = end - visit.startTime
            totalDuration += duration
            totals.merge(visit.placeCategory, duration) { acc, value -> acc + value }
        }
        if (totalDuration == 0L) {
            return@withContext emptyList<CategoryBreakdown>()
        }
        totals.entries.map { (category, millis) ->
            CategoryBreakdown(
                category = category,
                totalMinutes = millis / 60_000,
                percentOfInterval = millis.toFloat() / totalDuration.toFloat()
            )
        }
    }
}

/**
 * Data class representing the time spent in a particular category.  [totalMinutes] is the
 * duration in minutes and [percentOfInterval] is the fraction of the total time spent in
 * that category.
 */
data class CategoryBreakdown(
    val category: String,
    val totalMinutes: Long,
    val percentOfInterval: Float
)