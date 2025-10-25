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

    /**
     * Computes a basic correlation between the user's sleep sessions and their last recorded
     * activity before each sleep.  For each sleep session in the given interval this method
     * fetches the most recent visit that ended before the sleep start time and counts the
     * occurrences of each category.  The returned list is sorted in descending order of
     * frequency and includes the fraction of all sleep sessions that followed each category.
     *
     * This simple heuristic does not take into account sleep quality or duration.  In a
     * production implementation you might weight correlations by sleep quality or look at
     * multiple visits preceding sleep.
     */
    suspend fun getSleepCorrelations(
        from: Long,
        to: Long,
        sleepSessionDao: com.yourcompany.lifecycleclone.core.db.SleepSessionDao
    ): List<SleepCorrelation> = withContext(Dispatchers.IO) {
        val sessions = sleepSessionDao.getSessionsInRange(from, to)
        if (sessions.isEmpty()) return@withContext emptyList<SleepCorrelation>()
        val counts = mutableMapOf<String, Int>()
        for (session in sessions) {
            // Find the last visit before the sleep start
            val lastVisit = visitDao.getLastVisitBefore(session.startTime)
            lastVisit?.let { visit ->
                counts[visit.placeCategory] = (counts[visit.placeCategory] ?: 0) + 1
            }
        }
        val total = sessions.size
        counts.entries.map { (category, count) ->
            SleepCorrelation(
                category = category,
                count = count,
                fraction = count.toFloat() / total.toFloat()
            )
        }.sortedByDescending { it.count }
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