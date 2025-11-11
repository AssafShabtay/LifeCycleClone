package com.yourcompany.lifecycleclone.tracking.service

import com.yourcompany.lifecycleclone.core.db.SleepSessionDao
import com.yourcompany.lifecycleclone.core.model.SleepSessionEntity
import com.yourcompany.lifecycleclone.tracking.visits.VisitSessionManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Derives sleep sessions from long still visits recorded overnight. To avoid duplicates it guards
 * database inserts with a mutex and checks for overlapping sessions before writing.
 */
class SleepDetectionManager(
    private val sleepSessionDao: SleepSessionDao
) {
    private val mutex = Mutex()

    suspend fun onStillVisitCompleted(summary: VisitSessionManager.VisitSummary) {
        if (!summary.type.isStillLike) return
        val end = summary.endTime ?: return
        val durationMillis = end - summary.startTime
        if (durationMillis < MIN_DURATION_MILLIS || durationMillis > MAX_DURATION_MILLIS) {
            return
        }
        val zoneId = ZoneId.systemDefault()
        val startZoned = Instant.ofEpochMilli(summary.startTime).atZone(zoneId)
        val endZoned = Instant.ofEpochMilli(end).atZone(zoneId)
        if (!isWithinSleepWindow(startZoned, endZoned)) {
            return
        }

        mutex.withLock {
            val overlapping = sleepSessionDao.countOverlappingSessions(summary.startTime, end)
            if (overlapping > 0) return
            val sessionId = "auto_${summary.startTime}_${end}"
            val entity = SleepSessionEntity(
                sessionId = sessionId,
                startTime = summary.startTime,
                endTime = end,
                sourcePackage = AUTO_SOURCE_PACKAGE
            )
            sleepSessionDao.insertAll(listOf(entity))
        }
    }

    private fun isWithinSleepWindow(start: ZonedDateTime, end: ZonedDateTime): Boolean {
        val zone = start.zone
        val windowStart = start.toLocalDate().atTime(21, 0).atZone(zone)
        val windowEnd = windowStart.plusHours(9) // extends to 06:00 next day
        if (end > windowStart && start < windowEnd) {
            return true
        }
        val previousWindowStart = windowStart.minusDays(1)
        val previousWindowEnd = previousWindowStart.plusHours(9)
        return end > previousWindowStart && start < previousWindowEnd
    }

    companion object {
        private const val AUTO_SOURCE_PACKAGE = "lifecycle.auto"
        private const val MIN_DURATION_MILLIS = 2 * 60 * 60 * 1000L
        private const val MAX_DURATION_MILLIS = 12 * 60 * 60 * 1000L
    }
}
