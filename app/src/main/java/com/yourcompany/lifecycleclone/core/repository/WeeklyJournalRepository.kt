package com.yourcompany.lifecycleclone.core.repository

import com.yourcompany.lifecycleclone.core.db.VisitDao
import com.yourcompany.lifecycleclone.core.db.WeeklyJournalDao
import com.yourcompany.lifecycleclone.core.model.WeeklyJournalEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository used to generate and store weekly journal summaries.  A summary includes a
 * human‑readable breakdown of time spent in various categories and a list of photo URIs (not
 * implemented here).  Summaries are saved into the [WeeklyJournalDao] so the journal screen can
 * quickly retrieve them.
 */
class WeeklyJournalRepository(
    private val visitDao: VisitDao,
    private val journalDao: WeeklyJournalDao
) {
    /**
     * Generates a weekly summary for the ISO week beginning at [weekStartEpochDay].  If a
     * summary already exists in the database it will be replaced.  Photo support is
     * not implemented; the returned summary will contain an empty photo list.  Returns the
     * generated [WeeklyJournalEntity].
     */
    suspend fun generateWeeklySummary(weekStartEpochDay: Long): WeeklyJournalEntity = withContext(Dispatchers.IO) {
        val from = java.time.LocalDate.ofEpochDay(weekStartEpochDay)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val to = java.time.LocalDate.ofEpochDay(weekStartEpochDay).plusWeeks(1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val visits = visitDao.getVisitsInRange(from, to)
        // Aggregate durations by category
        val totals = mutableMapOf<String, Long>()
        visits.forEach { visit ->
            val endTime = visit.endTime ?: System.currentTimeMillis()
            val duration = endTime - visit.startTime
            totals.merge(visit.placeCategory, duration) { acc, value -> acc + value }
        }
        val summaryLines = totals.entries.sortedByDescending { it.value }
            .joinToString(separator = "\n") { (category, millis) ->
                val hours = millis / 3_600_000.0
                String.format("%s: %.1f h", category, hours)
            }
        // In a real implementation we would select top photos from the user's device based on
        // timestamps.  Here we store an empty list.
        val photosJson = Json.encodeToString<List<String>>(emptyList())
        val entity = WeeklyJournalEntity(
            weekStartEpochDay = weekStartEpochDay,
            summaryJson = Json.encodeToString(summaryLines),
            photoUrisJson = photosJson
        )
        journalDao.insert(entity)
        entity
    }

    suspend fun getWeeklySummary(weekStartEpochDay: Long): WeeklyJournalEntity? = withContext(Dispatchers.IO) {
        journalDao.getJournalForWeek(weekStartEpochDay)
    }
}